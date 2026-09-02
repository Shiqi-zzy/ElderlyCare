package com.elderlycare.app.data.incident

import com.elderlycare.app.data.binding.AlertLevel
import com.elderlycare.app.data.binding.BindingDao
import com.elderlycare.app.data.binding.BindingLifecycle
import com.elderlycare.app.data.binding.HcBindingStatus
import com.elderlycare.app.data.binding.HospitalCommunityBindingEntity
import com.elderlycare.app.data.binding.LocalAlertEntity
import com.elderlycare.app.data.community.CommunityDao
import com.elderlycare.app.data.community.ServiceRecord
import com.elderlycare.app.data.community.StaffScheduleRecord
import com.elderlycare.app.data.community.TodoItem
import com.elderlycare.app.data.local.UserStore
import com.elderlycare.app.data.model.AppUser
import com.elderlycare.app.data.model.ElderlyProfile
import com.elderlycare.app.data.model.QualificationStatus
import com.elderlycare.app.data.model.UserRole
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 四端协同事件处置核心仓库（单机演示轨：三账号共用 elderly_care.db，Room Flow 驱动跨端刷新）。
 *
 * 状态机见 [IncidentStatus]；参数见 [IncidentConfig]。
 * 责任链：家属模拟/真实触发 → 社区按楼栋自动派单生待办 → 社区联系家属/自行闭环或紧急出警 →
 * 医院在班医生先接先得 → 15s×3 加急/升级/漏接处罚 → 医院完成 → 社区闭环 → 家属可见，双方各写一条服务记录。
 */
class IncidentRepository(
    private val incidentDao: IncidentDao,
    private val communityDao: CommunityDao,
    private val bindingDao: BindingDao,
    private val userStore: UserStore
) {
    private val gson = Gson()
    private val stringListType = object : TypeToken<List<String>>() {}.type
    private val noFmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())

    private fun toJsonList(list: List<String>): String = gson.toJson(list)
    private fun fromJsonList(json: String): List<String> =
        runCatching { gson.fromJson<List<String>>(json.ifBlank { "[]" }, stringListType) }
            .getOrElse { emptyList() }

    // ==================== 触发（模拟跌倒 / 真实告警统一入口）====================

    /**
     * 触发一条跌倒事件：建 incident(RAISED→自动 COMMUNITY_RECEIVED)、给责任网格员建待办、写本地告警镜像。
     * @return 事件 id；找不到责任网格员时 communityStaffId 为空（挂社区公共池）。
     */
    suspend fun simulateFall(profile: ElderlyProfile, level: String = "HIGH", alarmId: String = ""): Long {
        val now = System.currentTimeMillis()
        val communityOrgId = profile.communityId.ifBlank { DEFAULT_COMMUNITY_ORG_ID }
        val staff = findCommunityStaff(communityOrgId, profile.buildingNo)
        val incident = IncidentEntity(
            incidentNo = genIncidentNo(now),
            alarmId = alarmId,
            elderlyId = profile.userId,
            elderlyName = profile.name.ifBlank { "老人" },
            buildingNo = profile.buildingNo,
            unitNo = profile.unitNo,
            roomNo = profile.roomNo,
            level = level,
            communityOrgId = communityOrgId,
            communityStaffId = staff?.phone ?: "",
            status = IncidentStatus.COMMUNITY_RECEIVED,
            triggeredAt = now,
            communityReceivedAt = now,
            createdAt = now
        )
        val id = incidentDao.insert(incident)

        // 责任网格员待办（公共池时也建一条，staffId 用社区机构占位以便机构内可见）
        communityDao.insertTodo(
            TodoItem(
                staffId = staff?.phone ?: communityOrgId,
                elderlyId = profile.userId,
                elderlyName = incident.elderlyName,
                todoType = "跌倒告警",
                todoSubType = SUBTYPE_FALL,
                title = "跌倒告警 - ${incident.elderlyName}（${profile.buildingNo}栋）",
                content = "设备监测到老人可能跌倒，请立即联系家属并评估是否紧急出警",
                priority = TodoItem.PRIORITY_HIGH,
                incidentId = id,
                createdAt = now
            )
        )
        // 本地告警镜像（三端共享唯一告警源）
        bindingDao.insertAlert(
            LocalAlertEntity(
                id = "incident_$id",
                deviceId = profile.deviceSn,
                elderlyId = profile.userId,
                type = "跌倒",
                level = AlertLevel.RISK.name,
                content = "跌倒事件 ${incident.incidentNo}",
                timestamp = now,
                status = com.elderlycare.app.data.binding.AlertStatus.UNREAD.name
            )
        )
        return id
    }

    // ==================== 社区动作 ====================

    suspend fun contactFamily(id: Long, note: String) {
        val inc = requireIncident(id)
        incidentDao.update(inc.copy(familyContactedAt = System.currentTimeMillis(), familyNote = note))
    }

    /**
     * 社区紧急出警。
     * @param hospitalOrgId 指定医院；null 时取该社区唯一 ACTIVE 绑定医院（0 家抛异常、多家抛异常要求选择）。
     */
    suspend fun requestDispatch(id: Long, hospitalOrgId: String? = null): Long {
        val inc = requireIncident(id)
        check(IncidentStatus.canDispatch(inc.status)) { "当前状态「${IncidentStatus.labelOf(inc.status)}」不能紧急出警" }
        val now = System.currentTimeMillis()

        val hospitals = bindingDao.getActiveHospitalsByCommunity(inc.communityOrgId)
        val targetHospital = when {
            hospitalOrgId != null -> hospitalOrgId
            hospitals.size == 1 -> hospitals[0].hospitalOrgId
            hospitals.isEmpty() -> throw IllegalStateException("该社区尚未绑定合作医院，无法出警，可改拨预留电话")
            else -> throw IllegalStateException("该社区绑定了多家医院，请选择出警医院")
        }
        val onDuty = findOnDutyDoctors(targetHospital, now).map { it.phone }

        incidentDao.update(
            inc.copy(
                status = IncidentStatus.DISPATCH_REQUESTED,
                dispatchRequestedAt = now,
                hospitalReceivedAt = now,
                hospitalOrgId = targetHospital,
                onDutyDoctorIds = toJsonList(onDuty)
            )
        )
        // 转警时即无人在班：无需等待加急，直接走升级（只记排班空缺、不罚医生）
        if (onDuty.isEmpty()) escalate(id, now)
        return id
    }

    /** 社区自行闭环（未转警旁路） */
    suspend fun selfClose(id: Long, note: String) {
        val inc = requireIncident(id)
        check(IncidentStatus.canSelfClose(inc.status)) { "已转警事件不能自行闭环" }
        val now = System.currentTimeMillis()
        val staff = userStore.getStaffByPhone(inc.communityStaffId)
        val recordId = communityDao.insertServiceRecord(
            buildCommunityRecord(inc, staff, note, inc.communityReceivedAt, now, closedBySelf = true)
        )
        incidentDao.update(
            inc.copy(
                status = IncidentStatus.SELF_CLOSED,
                communityNote = note,
                communityDoneAt = now,
                closedAt = now,
                communityRecordId = recordId
            )
        )
        finishIncidentTodos(id, now)
    }

    /** 社区闭环（转警路径，强校验医院已完成） */
    suspend fun communityComplete(id: Long, note: String) {
        val inc = requireIncident(id)
        check(IncidentStatus.canCommunityClose(inc.status)) { "请等待医院处置完成后，社区才能闭环" }
        val now = System.currentTimeMillis()
        val staff = userStore.getStaffByPhone(inc.communityStaffId)
        val recordId = communityDao.insertServiceRecord(
            buildCommunityRecord(inc, staff, note, inc.communityReceivedAt, now, closedBySelf = false)
        )
        incidentDao.update(
            inc.copy(
                status = IncidentStatus.CLOSED,
                communityNote = note,
                communityDoneAt = now,
                closedAt = now,
                communityRecordId = recordId
            )
        )
        finishIncidentTodos(id, now)
    }

    // ==================== 医院动作 ====================

    /** 一键处警（先接先得 CAS）：true=抢到，false=已被其他医生接走 */
    suspend fun acceptByDoctor(id: Long, doctor: AppUser): Boolean {
        requireIncident(id)
        val now = System.currentTimeMillis()
        val rows = incidentDao.casAccept(id, doctor.phone, now)
        return rows > 0
    }

    /** 医院处置完成（措施必填），写医院服务记录 */
    suspend fun hospitalComplete(id: Long, treatment: String, doctor: AppUser) {
        val inc = requireIncident(id)
        check(inc.status == IncidentStatus.HOSPITAL_ACCEPTED) { "请先一键处警" }
        check(treatment.isNotBlank()) { "请填写处置措施" }
        val now = System.currentTimeMillis()
        val recordId = communityDao.insertServiceRecord(
            ServiceRecord(
                staffId = doctor.phone,
                staffName = doctor.name,
                elderlyId = inc.elderlyId,
                elderlyName = inc.elderlyName,
                serviceType = "急救处警",
                content = "值班医生 ${doctor.name} 处警，处置措施：$treatment",
                treatment = treatment,
                side = "hospital",
                incidentId = id,
                startedAt = inc.hospitalAcceptedAt,
                finishedAt = now,
                durationMinutes = minutesBetween(inc.hospitalAcceptedAt, now),
                createdAt = now
            )
        )
        incidentDao.update(
            inc.copy(
                status = IncidentStatus.HOSPITAL_DONE,
                hospitalDoneAt = now,
                hospitalTreatment = treatment,
                hospitalRecordId = recordId
            )
        )
    }

    // ==================== 加急 / 升级 / 处罚（调度器周期调用）====================

    /** 周期扫描：15s×3 加急，第 3 次后升级并对在班漏接医生记处罚。 */
    suspend fun tick(now: Long = System.currentTimeMillis()) {
        // 1) 等待/加急中
        incidentDao.getWaitingForDoctor().forEach { inc ->
            val base = inc.dispatchRequestedAt ?: return@forEach
            val elapsed = now - base
            val interval = IncidentConfig.urgentInterval
            val onDuty = fromJsonList(inc.onDutyDoctorIds)

            // 转警时即无人在班：到第一个加急节拍直接升级（不罚医生，记排班空缺）
            if (onDuty.isEmpty()) {
                if (elapsed >= interval) escalate(inc.id!!, now)
                return@forEach
            }
            if (inc.urgentCount < IncidentConfig.URGENT_MAX_TIMES &&
                elapsed >= interval * (inc.urgentCount + 1)
            ) {
                incidentDao.update(
                    inc.copy(
                        urgentCount = inc.urgentCount + 1,
                        urgentLastAt = now,
                        status = IncidentStatus.URGENT
                    )
                )
                return@forEach
            }
            if (inc.urgentCount >= IncidentConfig.URGENT_MAX_TIMES &&
                elapsed >= interval * IncidentConfig.URGENT_MAX_TIMES
            ) {
                escalate(inc.id!!, now)
            }
        }
        // 2) 已升级但承接人仍未接：宽限期后记 escalation_fault，并尝试再兜底
        incidentDao.getEscalatedWaiting().forEach { inc ->
            val escalatedAt = inc.escalatedAt ?: return@forEach
            if (now - escalatedAt >= IncidentConfig.ESCALATE_GRACE_MS) {
                val nextId = inc.escalatedToDoctorId
                if (nextId != null && !fromJsonList(inc.missedDoctorIds).contains(nextId)) {
                    val doctor = userStore.getStaffByPhone(nextId)
                    if (doctor != null) {
                        addPenalty(inc, doctor, PenaltyType.ESCALATION_FAULT, IncidentConfig.SCORE_ESCALATION_FAULT, now)
                    }
                }
                val fallback = findNextDutyDoctor(inc.hospitalOrgId ?: return@forEach, now,
                    exclude = fromJsonList(inc.onDutyDoctorIds) + listOfNotNull(inc.escalatedToDoctorId))
                if (fallback == null) {
                    incidentDao.update(inc.copy(status = IncidentStatus.ESCALATED_UNANSWERED))
                }
            }
        }
    }

    /** 升级：在班漏接处罚 → 找下一排班人承接；在班为空则只升级不罚（排班空缺）。 */
    private suspend fun escalate(id: Long, now: Long) {
        val inc = requireIncident(id)
        val onDuty = fromJsonList(inc.onDutyDoctorIds)
        // 在班漏接处罚
        onDuty.forEach { docId ->
            val doctor = userStore.getStaffByPhone(docId) ?: return@forEach
            addPenalty(inc, doctor, PenaltyType.MISSED, IncidentConfig.SCORE_MISSED, now)
        }
        val next = findNextDutyDoctor(
            inc.hospitalOrgId ?: return,
            now,
            exclude = onDuty
        )
        incidentDao.update(
            inc.copy(
                status = if (next == null && onDuty.isEmpty()) IncidentStatus.ESCALATED_UNANSWERED else IncidentStatus.ESCALATED,
                escalatedAt = now,
                escalatedToDoctorId = next?.phone,
                missedDoctorIds = toJsonList(onDuty)
            )
        )
    }

    private suspend fun addPenalty(inc: IncidentEntity, doctor: AppUser, type: String, delta: Int, now: Long) {
        // 含本次的累计漏接数定级
        val historyMissed = incidentDao.countActiveMissed(doctor.phone)
        val totalMissed = historyMissed + if (type == PenaltyType.MISSED) 1 else 0
        val level = when {
            totalMissed >= IncidentConfig.SUSPEND_THRESHOLD -> PenaltyLevel.SUSPENSION
            totalMissed >= IncidentConfig.NOTICE_THRESHOLD -> PenaltyLevel.NOTICE
            else -> PenaltyLevel.WARNING
        }
        incidentDao.insertPenalty(
            DoctorPenaltyEntity(
                incidentId = inc.id!!,
                doctorId = doctor.phone,
                doctorName = doctor.name,
                hospitalOrgId = inc.hospitalOrgId ?: doctor.organizationId.orEmpty(),
                penaltyType = type,
                urgentCountAtMiss = inc.urgentCount,
                level = level,
                scoreDelta = delta,
                createdAt = now
            )
        )
    }

    /** 管理端撤销处罚（误判申诉） */
    suspend fun revokePenalty(penaltyId: Long, reason: String) {
        incidentDao.revokePenalty(penaltyId, System.currentTimeMillis(), reason)
    }

    // ==================== 排班 ====================

    suspend fun createShift(
        staffId: String,
        title: String,
        scheduleDate: Long,
        startTime: String,
        endTime: String,
        location: String,
        scheduleMode: Int,
        weekday: Int,
        role: String = "hospital"
    ): Long = communityDao.insertSchedule(
        StaffScheduleRecord(
            staffId = staffId,
            title = title,
            scheduleDate = scheduleDate,
            startTime = startTime,
            endTime = endTime,
            location = location,
            scheduleMode = scheduleMode,
            weekday = weekday,
            role = role,
            createdAt = System.currentTimeMillis()
        )
    )

    fun observeShifts(staffId: String, role: String): Flow<List<StaffScheduleRecord>> =
        communityDao.observeSchedulesByRole(staffId, role)

    /** 当前时刻在班医生：资质通过、未停班、时间命中其班次（支持按周循环/指定日期/跨夜）。 */
    private suspend fun findOnDutyDoctors(hospitalOrgId: String, now: Long): List<AppUser> {
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val schedules = communityDao.getAllSchedulesByRole("hospital")
        val candidates = userStore.getUsersByRole(UserRole.HOSPITAL)
            .filter { it.organizationId == hospitalOrgId && isQualified(it) && !isSuspended(it.phone) }
        return candidates.filter { doctor ->
            schedules.any { it.staffId == doctor.phone && hitShift(it, cal) }
        }
    }

    /** 升级承接人：当天后续班次/其他资质医生 → 院管理员兜底；排除已漏接人员。 */
    private suspend fun findNextDutyDoctor(hospitalOrgId: String, now: Long, exclude: List<String>): AppUser? {
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val schedules = communityDao.getAllSchedulesByRole("hospital")
        val candidates = userStore.getUsersByRole(UserRole.HOSPITAL)
            .filter {
                it.organizationId == hospitalOrgId && isQualified(it) &&
                    !isSuspended(it.phone) && it.phone !in exclude
            }
        // 优先：有班次命中或即将接班的医生
        val withShift = candidates.filter { doctor ->
            schedules.any { it.staffId == doctor.phone && hitShift(it, cal) }
        }
        if (withShift.isNotEmpty()) return withShift.first()
        // 管理员/主任优先兜底
        return candidates.firstOrNull { it.title.contains("管理员") || it.title.contains("主任") }
            ?: candidates.firstOrNull()
    }

    private fun hitShift(shift: StaffScheduleRecord, cal: Calendar): Boolean {
        // 周循环：weekday（周一=1..周日=7）匹配；指定日期：同一天
        val dow = cal.get(Calendar.DAY_OF_WEEK).let {
            // Calendar：周日=1..周六=7 → 转周一=1..周日=7
            if (it == Calendar.SUNDAY) 7 else it - 1
        }
        val modeOk = when (shift.scheduleMode) {
            ScheduleMode.WEEKLY -> shift.weekday == dow
            ScheduleMode.ONCE -> isSameDay(shift.scheduleDate, cal.timeInMillis)
            else -> false
        }
        if (!modeOk) return false
        val nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val start = toMinutes(shift.startTime) ?: return false
        val end = toMinutes(shift.endTime) ?: return false
        return if (end > start) nowMin in start..end            // 普通白班
        else nowMin >= start || nowMin <= end                  // 跨夜班次
    }

    // ==================== 医院-社区绑定 ====================

    suspend fun applyHospitalCommunity(hospitalOrgId: String, communityOrgId: String, reason: String): Long {
        val exists = bindingDao.getHcBindingsByHospital(hospitalOrgId)
            .any { it.communityOrgId == communityOrgId && it.status != HcBindingStatus.REJECTED.name }
        check(!exists) { "已申请/已绑定该社区，请勿重复申请" }
        return bindingDao.insertHcBinding(
            HospitalCommunityBindingEntity(
                hospitalOrgId = hospitalOrgId,
                communityOrgId = communityOrgId,
                status = HcBindingStatus.PENDING.name,
                applyReason = reason,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    /** 管理端审批 */
    suspend fun reviewHcBinding(bindingId: Long, approve: Boolean, reviewer: String, note: String) {
        val b = bindingDao.getHcBinding(bindingId) ?: return
        bindingDao.updateHcBindingStatus(
            bindingId,
            if (approve) HcBindingStatus.ACTIVE.name else HcBindingStatus.REJECTED.name,
            reviewer, note, System.currentTimeMillis()
        )
    }

    fun observeHcBindings(hospitalOrgId: String) = bindingDao.observeHcBindingsByHospital(hospitalOrgId)
    fun observePendingHcBindings() = bindingDao.observePendingHcBindings()

    suspend fun getActiveCommunityIds(hospitalOrgId: String) =
        bindingDao.getActiveCommunityIdsByHospital(hospitalOrgId)

    // ==================== 查询 / 大屏 / 绩效 ====================

    fun observeByCommunityStaff(staffId: String): Flow<List<IncidentEntity>> =
        incidentDao.observeByCommunityStaff(staffId)

    fun observeActiveByCommunityStaff(staffId: String): Flow<List<IncidentEntity>> =
        incidentDao.observeActiveByCommunityStaff(staffId)

    fun observeByHospital(hospitalOrgId: String): Flow<List<IncidentEntity>> =
        incidentDao.observeByHospital(hospitalOrgId)

    fun observeByFamily(elderlyIds: List<String>): Flow<List<IncidentEntity>> =
        incidentDao.observeByFamily(elderlyIds)

    fun observeById(id: Long): Flow<IncidentEntity?> = incidentDao.observeById(id)
    suspend fun getById(id: Long) = incidentDao.getById(id)

    /** 社区大屏：聚合 8 栋（负责人/老人数/未闭环事件数）。profiles 由 ViewModel 从档案流提供。 */
    suspend fun buildBuildingGrid(communityOrgId: String, profiles: List<ElderlyProfile>): List<BuildingCell> {
        val staffList = userStore.getUsersByRole(UserRole.COMMUNITY)
            .filter { it.organizationId == communityOrgId }
        val active = incidentDao.getActiveByCommunity(communityOrgId)
        return (1..IncidentConfig.TOTAL_BUILDINGS).map { b ->
            val bNo = b.toString()
            val owner = staffList.firstOrNull { bNo in it.areaBuildings }
            val elderlyHere = profiles.filter {
                it.communityId == communityOrgId && it.buildingNo == bNo
            }
            BuildingCell(
                buildingNo = bNo,
                staffName = owner?.name,
                staffId = owner?.phone,
                elderlyCount = elderlyHere.size,
                activeIncidentCount = active.count { it.buildingNo == bNo }
            )
        }
    }

    /** 医院大屏：聚合已绑定社区（未闭环事件数）。 */
    suspend fun buildHospitalGrid(hospitalOrgId: String): List<CommunityCell> {
        val bindings = bindingDao.getHcBindingsByHospital(hospitalOrgId)
            .filter { it.status == HcBindingStatus.ACTIVE.name }
        val active = incidentDao.getActiveByHospital(hospitalOrgId)
        return bindings.map { b ->
            val org = bindingDao.getOrganization(b.communityOrgId)
            CommunityCell(
                communityOrgId = b.communityOrgId,
                communityName = org?.name ?: "社区",
                activeIncidentCount = active.count { it.communityOrgId == b.communityOrgId && it.hospitalOrgId == hospitalOrgId }
            )
        }
    }

    /** 医生值班绩效聚合 */
    suspend fun buildPerformance(doctor: AppUser): DoctorPerformance {
        val penalties = incidentDao.getPenalties(doctor.phone)
        val active = penalties.filter { it.status == "active" }
        val score = IncidentConfig.SCORE_INIT + incidentDao.sumActivePenalty(doctor.phone)
        val accepted = incidentDao.countAcceptedByDoctor(doctor.phone)
        val missed = active.count { it.penaltyType == PenaltyType.MISSED }
        val avgMs = incidentDao.avgResponseMsByDoctor(doctor.phone)
        return DoctorPerformance(
            doctorId = doctor.phone,
            doctorName = doctor.name,
            score = score,
            acceptedCount = accepted,
            missedCount = missed,
            avgResponseSeconds = avgMs?.let { it / 1000 }?.toLong(),
            activePenalties = active
        )
    }

    fun observePenalties(doctorId: String) = incidentDao.observePenalties(doctorId)

    /** 某老人服务记录（家属时间线） */
    fun observeElderlyServiceRecords(elderlyId: String) =
        communityDao.observeServiceRecordsByElderly(elderlyId)

    // ==================== 私有工具 ====================

    private suspend fun requireIncident(id: Long): IncidentEntity =
        incidentDao.getById(id) ?: throw IllegalStateException("事件不存在：$id")

    private suspend fun findCommunityStaff(communityOrgId: String, buildingNo: String): AppUser? {
        if (buildingNo.isBlank()) return null
        return userStore.getUsersByRole(UserRole.COMMUNITY)
            .firstOrNull { it.organizationId == communityOrgId && buildingNo in it.areaBuildings }
    }

    private fun isQualified(user: AppUser): Boolean =
        user.qualification == null || user.qualification == QualificationStatus.APPROVED.name

    private suspend fun isSuspended(doctorId: String): Boolean =
        incidentDao.getPenalties(doctorId).any {
            it.status == "active" && it.level == PenaltyLevel.SUSPENSION
        }

    private fun buildCommunityRecord(
        inc: IncidentEntity,
        staff: AppUser?,
        note: String,
        startedAt: Long?,
        finishedAt: Long,
        closedBySelf: Boolean
    ): ServiceRecord {
        val type = if (closedBySelf) "跌倒事件自行闭环" else "跌倒事件处置"
        val content = buildString {
            append("事件 ${inc.incidentNo}：${IncidentStatus.labelOf(inc.status)}")
            if (inc.familyNote.isNotBlank()) append("；联系家属：${inc.familyNote}")
            if (inc.hospitalTreatment.isNotBlank()) append("；医院处置：${inc.hospitalTreatment}")
            if (note.isNotBlank()) append("；收尾说明：$note")
        }
        return ServiceRecord(
            staffId = staff?.phone ?: inc.communityStaffId,
            staffName = staff?.name ?: "",
            elderlyId = inc.elderlyId,
            elderlyName = inc.elderlyName,
            serviceType = type,
            content = content,
            side = "community",
            incidentId = inc.id,
            startedAt = startedAt,
            finishedAt = finishedAt,
            durationMinutes = minutesBetween(startedAt, finishedAt),
            createdAt = finishedAt
        )
    }

    private suspend fun finishIncidentTodos(incidentId: Long, now: Long) {
        communityDao.getTodosByIncident(incidentId, TodoItem.STATUS_PENDING).forEach {
            communityDao.updateTodoStatus(it.id, TodoItem.STATUS_DONE, now)
        }
    }

    private fun genIncidentNo(now: Long): String =
        "INC" + noFmt.format(Date(now)) + now.toString().takeLast(4)

    private fun toMinutes(hhmm: String): Int? = runCatching {
        val parts = hhmm.split(":", "：")
        parts[0].toInt() * 60 + parts[1].toInt()
    }.getOrNull()

    private fun isSameDay(a: Long, b: Long): Boolean {
        val ca = Calendar.getInstance().apply { timeInMillis = a }
        val cb = Calendar.getInstance().apply { timeInMillis = b }
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
            ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
    }

    private fun minutesBetween(start: Long?, end: Long?): Int {
        if (start == null || end == null || end <= start) return 0
        return ((end - start) / 60000).toInt()
    }

    companion object {
        /** 演示默认社区机构 id（与 SeedData.COMMUNITY_ORG_ID 对齐） */
        const val DEFAULT_COMMUNITY_ORG_ID = "org_community_01"
        const val SUBTYPE_FALL = "FALL_DISPATCH"
    }
}
