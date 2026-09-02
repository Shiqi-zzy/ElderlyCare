package com.elderlycare.app.data.binding

import com.elderlycare.app.data.community.CommunityDao
import com.elderlycare.app.data.community.StaffScheduleRecord
import com.elderlycare.app.data.incident.ScheduleMode
import com.elderlycare.app.data.local.ElderlyProfileStore
import com.elderlycare.app.data.local.FamilyUserStore
import com.elderlycare.app.data.local.UserStore
import com.elderlycare.app.data.model.AppUser
import com.elderlycare.app.data.model.ElderlyProfile
import com.elderlycare.app.data.model.FamilyUser
import com.elderlycare.app.data.model.Gender
import com.elderlycare.app.data.model.QualificationStatus
import com.elderlycare.app.data.model.UserRole

/**
 * 预置演示数据（幂等，可重复启动）。
 *
 * 机构：幸福社区养老驿站（社区）、新华社区医院（医院）。
 *
 * 工作人员（密码统一 [DEMO_PASSWORD]）：
 * - 社区网格员：李网格(1-3栋) 13810000001 / 王网格(4-6栋) 13810000002 / 赵网格(7-8栋) 13810000003
 * - 医院医生：王医生(工作日00-12) 13820000001 / 刘医生(工作日12-24) 13820000002 / 陈医生(周末全天) 13820000003
 * - 院管理员兜底：周管理员 13820000009
 *
 * 并预置：医生周循环排班、ACTIVE 医院-社区绑定、3 位分楼栋老人及其家属账号。
 */
class SeedData(
    private val userStore: UserStore,
    private val bindingDao: BindingDao,
    private val communityDao: CommunityDao? = null,
    private val profileStore: ElderlyProfileStore? = null,
    private val familyUserStore: FamilyUserStore? = null
) {

    companion object {
        const val COMMUNITY_ORG_ID = "org_community_01"
        const val HOSPITAL_ORG_ID = "org_hospital_01"

        /** 演示账号统一密码（演示用明文，生产需后端加盐哈希） */
        const val DEMO_PASSWORD = "123456"
    }

    suspend fun ensureSeeded() {
        val now = System.currentTimeMillis()
        seedOrganizations(now)
        seedStaff(now)
        seedDoctorShifts()
        seedHospitalCommunityBinding(now)
        seedElderlyAndFamily(now)
    }

    // ==================== 机构 ====================
    private suspend fun seedOrganizations(now: Long) {
        // REPLACE 幂等：同时为旧库机构补齐展示 5 字段
        bindingDao.insertOrganizations(
            listOf(
                OrganizationEntity(
                    id = COMMUNITY_ORG_ID,
                    name = "幸福社区养老驿站",
                    type = OrganizationType.COMMUNITY.name,
                    contactPerson = "李网格",
                    contactPhone = "13810000001",
                    address = "幸福社区服务中心1号楼",
                    serviceArea = "1-8栋",
                    intro = "提供社区居家养老巡访、应急联动服务",
                    createdAt = now
                ),
                OrganizationEntity(
                    id = HOSPITAL_ORG_ID,
                    name = "新华社区医院",
                    type = OrganizationType.HOSPITAL.name,
                    contactPerson = "周管理员",
                    contactPhone = "13820000009",
                    address = "幸福路120号",
                    serviceArea = "幸福社区",
                    intro = "社区急救处警与健康管理合作医院",
                    createdAt = now
                ),
                OrganizationEntity(
                    id = "org_community_02",
                    name = "安康社区养老驿站",
                    type = OrganizationType.COMMUNITY.name,
                    contactPerson = "社区前台",
                    contactPhone = "13810000004",
                    address = "安康路8号",
                    serviceArea = "安康小区",
                    intro = "可申请合作的周边社区",
                    createdAt = now
                ),
                OrganizationEntity(
                    id = "org_community_03",
                    name = "民乐社区养老服务站",
                    type = OrganizationType.COMMUNITY.name,
                    contactPerson = "社区前台",
                    contactPhone = "13810000005",
                    address = "民乐巷16号",
                    serviceArea = "民乐小区",
                    intro = "可申请合作的周边社区",
                    createdAt = now
                )
            )
        )
    }

    // ==================== 工作人员 ====================
    private suspend fun seedStaff(now: Long) {
        // 社区网格员（固定 8 栋，每人可管多栋、不共管）
        registerIfAbsent(
            AppUser(
                phone = "13810000001", name = "李网格", password = DEMO_PASSWORD,
                role = UserRole.COMMUNITY, organizationId = COMMUNITY_ORG_ID,
                title = "网格员", areaBuildings = listOf("1", "2", "3"),
                qualification = QualificationStatus.APPROVED.name, createdAt = now
            )
        )
        registerIfAbsent(
            AppUser(
                phone = "13810000002", name = "王网格", password = DEMO_PASSWORD,
                role = UserRole.COMMUNITY, organizationId = COMMUNITY_ORG_ID,
                title = "网格员", areaBuildings = listOf("4", "5", "6"),
                qualification = QualificationStatus.APPROVED.name, createdAt = now
            )
        )
        registerIfAbsent(
            AppUser(
                phone = "13810000003", name = "赵网格", password = DEMO_PASSWORD,
                role = UserRole.COMMUNITY, organizationId = COMMUNITY_ORG_ID,
                title = "网格员", areaBuildings = listOf("7", "8"),
                qualification = QualificationStatus.APPROVED.name, createdAt = now
            )
        )
        // 医院医生（资质已通过，保证可接单）
        registerIfAbsent(
            AppUser(
                phone = "13820000001", name = "王医生", password = DEMO_PASSWORD,
                role = UserRole.HOSPITAL, organizationId = HOSPITAL_ORG_ID,
                title = "主治医师", qualification = QualificationStatus.APPROVED.name, createdAt = now
            )
        )
        registerIfAbsent(
            AppUser(
                phone = "13820000002", name = "刘医生", password = DEMO_PASSWORD,
                role = UserRole.HOSPITAL, organizationId = HOSPITAL_ORG_ID,
                title = "主治医师", qualification = QualificationStatus.APPROVED.name, createdAt = now
            )
        )
        registerIfAbsent(
            AppUser(
                phone = "13820000003", name = "陈医生", password = DEMO_PASSWORD,
                role = UserRole.HOSPITAL, organizationId = HOSPITAL_ORG_ID,
                title = "值班医师", qualification = QualificationStatus.APPROVED.name, createdAt = now
            )
        )
        registerIfAbsent(
            AppUser(
                phone = "13820000009", name = "周管理员", password = DEMO_PASSWORD,
                role = UserRole.HOSPITAL, organizationId = HOSPITAL_ORG_ID,
                title = "院管理员", qualification = QualificationStatus.APPROVED.name, createdAt = now
            )
        )
    }

    // ==================== 医生周循环排班（工作日全天分段、周末全天）====================
    private suspend fun seedDoctorShifts() {
        val dao = communityDao ?: return
        if (dao.getAllSchedulesByRole("hospital").isNotEmpty()) return
        val now = System.currentTimeMillis()
        val shifts = mutableListOf<StaffScheduleRecord>()
        // 王医生：工作日 00:00-12:00
        for (wd in 1..5) {
            shifts += shift("13820000001", "王医生上午班", wd, "00:00", "12:00", now)
        }
        // 刘医生：工作日 12:00-23:59
        for (wd in 1..5) {
            shifts += shift("13820000002", "刘医生下午/夜班", wd, "12:00", "23:59", now)
        }
        // 陈医生：周末全天
        shifts += shift("13820000003", "陈医生周六班", 6, "00:00", "23:59", now)
        shifts += shift("13820000003", "陈医生周日班", 7, "00:00", "23:59", now)
        shifts.forEach { dao.insertSchedule(it) }
    }

    private fun shift(staffId: String, title: String, weekday: Int, start: String, end: String, now: Long) =
        StaffScheduleRecord(
            staffId = staffId,
            title = title,
            scheduleDate = now,
            startTime = start,
            endTime = end,
            location = "新华社区医院急诊前台",
            scheduleMode = ScheduleMode.WEEKLY,
            weekday = weekday,
            role = "hospital",
            createdAt = now
        )

    // ==================== 医院-社区绑定（管理端已审批 ACTIVE）====================
    private suspend fun seedHospitalCommunityBinding(now: Long) {
        val exist = bindingDao.getHcBindingsByHospital(HOSPITAL_ORG_ID)
        if (exist.none { it.communityOrgId == COMMUNITY_ORG_ID }) {
            bindingDao.insertHcBinding(
                HospitalCommunityBindingEntity(
                    hospitalOrgId = HOSPITAL_ORG_ID,
                    communityOrgId = COMMUNITY_ORG_ID,
                    status = HcBindingStatus.ACTIVE.name,
                    applyReason = "申请为幸福社区提供急救处警与健康服务",
                    reviewedBy = "平台管理端",
                    reviewNote = "演示预置：自动审批通过",
                    createdAt = now,
                    reviewedAt = now
                )
            )
        }
    }

    // ==================== 演示老人 + 家属账号 ====================
    private suspend fun seedElderlyAndFamily(now: Long) {
        val store = profileStore ?: return
        val existed = store.getAllProfiles().map { it.userId }.toSet()
        val elders = listOf(
            ElderlyProfile(
                userId = "13800001111", name = "张德福", gender = Gender.MALE, age = "78",
                phone = "13800001111", emergencyContactName = "张小军", emergencyContactPhone = "13800001111",
                communityId = COMMUNITY_ORG_ID, buildingNo = "1", unitNo = "1", roomNo = "201",
                deviceSn = "RK3-DEMO-0001", deviceBound = true, privacyConsentGiven = true
            ),
            ElderlyProfile(
                userId = "13800001112", name = "李秀兰", gender = Gender.FEMALE, age = "82",
                phone = "13800001112", emergencyContactName = "李华", emergencyContactPhone = "13800001112",
                communityId = COMMUNITY_ORG_ID, buildingNo = "4", unitNo = "2", roomNo = "102",
                deviceSn = "RK3-DEMO-0002", deviceBound = true, privacyConsentGiven = true
            ),
            ElderlyProfile(
                userId = "13800001113", name = "陈桂英", gender = Gender.FEMALE, age = "75",
                phone = "13800001113", emergencyContactName = "陈强", emergencyContactPhone = "13800001113",
                communityId = COMMUNITY_ORG_ID, buildingNo = "8", unitNo = "1", roomNo = "303",
                deviceSn = "RK3-DEMO-0003", deviceBound = true, privacyConsentGiven = true
            )
        )
        elders.forEach { elder ->
            if (elder.userId !in existed) store.saveProfile(elder)
            familyUserStore?.register(
                FamilyUser(
                    phone = elder.userId,
                    name = "${elder.name}家属",
                    password = DEMO_PASSWORD,
                    contact = elder.emergencyContactPhone
                )
            )
        }
    }

    private suspend fun registerIfAbsent(user: AppUser) {
        if (userStore.getStaffByPhone(user.phone) == null) {
            userStore.register(user)
        }
    }
}
