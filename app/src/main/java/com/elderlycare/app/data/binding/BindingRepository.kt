package com.elderlycare.app.data.binding

import com.elderlycare.app.data.local.ElderlyProfileStore
import com.elderlycare.app.data.local.FamilyUserStore
import com.elderlycare.app.data.local.UserStore
import com.elderlycare.app.data.message.MessageDao
import com.elderlycare.app.data.message.MessageEntity
import com.elderlycare.app.data.model.AppUser
import com.elderlycare.app.data.model.ElderlyProfile
import com.elderlycare.app.data.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * 多端绑定核心业务层（第三阶段）。
 *
 * UI → BindingRepository → BindingDao / ProfileStore / UserStore。
 * 负责：发起绑定申请、家属同意/拒绝、解除授权、以及三端列表的富化（机构名/申请人名/老人名）。
 *
 * 多端语义：同一老人可被**不同**申请方（社区/医院，或多位工作人员）各自授权，
 * 授权关系按 `userId + elderlyId` 唯一；「不允许重复授权」按同一申请方判断。
 *
 * 返回约定：`String?` —— null 表示成功，非 null 为错误文案（展示给用户）。
 */
class BindingRepository(
    private val staffUserStore: UserStore,
    private val familyUserStore: FamilyUserStore,
    private val profileStore: ElderlyProfileStore,
    private val bindingDao: BindingDao,
    // 绑定申请 → 家属 SYSTEM 消息（我的页授权管理红角标数据源）
    private val messageDao: MessageDao
) {

    // ==================== 富化后的 UI 数据 ====================

    /** 一条绑定申请（家属待处理/已同意/已拒绝、工作人员我的申请共用） */
    data class RequestUi(
        val id: String,
        val requesterUserId: String,
        val requesterName: String,
        val requesterRole: String,
        val organizationId: String,
        val orgName: String,
        val orgTypeLabel: String,
        val familyUserId: String,
        val elderlyId: String,
        val elderlyName: String,
        val deviceId: String,
        val status: String,
        val message: String,
        val createdAt: Long,
        val reviewedAt: Long?
    )

    /** 一条生效/已解除的绑定关系（家属授权管理、工作人员已绑定用户共用） */
    data class BindingUi(
        val id: String,
        val userId: String,
        val userRoleLabel: String,
        val organizationId: String,
        val orgName: String,
        val orgTypeLabel: String,
        val elderlyId: String,
        val elderlyName: String,
        val status: String,
        val createdAt: Long,
        val updatedAt: Long
    )

    /** 当前用户有权访问的老人（档案 + 绑定信息），社区/医院各页统一消费（第四阶段权限过滤）。 */
    data class AccessibleElderlyUi(
        val elderlyId: String,        // = profile.userId
        val profile: ElderlyProfile,
        val bindingCreatedAt: Long,   // 绑定时间（家属侧无绑定，为 0）
        val orgName: String           // 授权机构名（家属侧为空）
    )

    // ==================== 内部工具 ====================

    private fun newId(): String = UUID.randomUUID().toString()

    private fun roleLabel(roleName: String): String =
        UserRole.entries.firstOrNull { it.name == roleName }?.label ?: roleName

    private suspend fun orgEnrich(organizationId: String): Pair<String, String> {
        val org = bindingDao.getOrganization(organizationId)
        if (org == null) return "未知机构" to ""
        val type = runCatching { OrganizationType.valueOf(org.type) }.getOrNull()
        return (org.name) to (type?.label ?: org.type)
    }

    private suspend fun enrichRequests(list: List<BindingRequestEntity>): List<RequestUi> =
        list.map { req ->
            val (orgName, orgType) = orgEnrich(req.organizationId)
            val requesterName = staffUserStore.getStaffByPhone(req.requesterUserId)?.name
                ?: req.requesterUserId
            val elderlyName = profileStore.getPrimaryProfile(req.elderlyId)?.name ?: req.elderlyId
            RequestUi(
                id = req.id,
                requesterUserId = req.requesterUserId,
                requesterName = requesterName,
                requesterRole = req.requesterRole,
                organizationId = req.organizationId,
                orgName = orgName,
                orgTypeLabel = orgType,
                familyUserId = req.familyUserId,
                elderlyId = req.elderlyId,
                elderlyName = elderlyName,
                deviceId = req.deviceId,
                status = req.status,
                message = req.message,
                createdAt = req.createdAt,
                reviewedAt = req.reviewedAt
            )
        }

    private suspend fun enrichBindings(list: List<UserElderlyBindingEntity>): List<BindingUi> =
        list.map { b ->
            val (orgName, orgType) = orgEnrich(b.organizationId)
            val elderlyName = profileStore.getPrimaryProfile(b.elderlyId)?.name ?: b.elderlyId
            BindingUi(
                id = b.id,
                userId = b.userId,
                userRoleLabel = roleLabel(b.userRole),
                organizationId = b.organizationId,
                orgName = orgName,
                orgTypeLabel = orgType,
                elderlyId = b.elderlyId,
                elderlyName = elderlyName,
                status = b.status,
                createdAt = b.createdAt,
                updatedAt = b.updatedAt
            )
        }

    // ==================== 提交绑定申请 ====================

    /** 社区/医院发起绑定申请，前置 7 项校验全部通过才写入 PENDING。 */
    suspend fun submitBindingRequest(
        requester: AppUser,
        elderlyProfile: ElderlyProfile,
        message: String?
    ): String? {
        // ① 申请方必须是社区/医院工作人员
        if (requester.role != UserRole.COMMUNITY && requester.role != UserRole.HOSPITAL) {
            return "仅社区/医院工作人员可发起绑定申请"
        }
        // ② elderlyId 必须存在
        val elderlyId = elderlyProfile.userId
        if (elderlyId.isBlank()) return "照护对象档案无效"
        // ③ 照护对象档案必须存在
        val profile = profileStore.getPrimaryProfile(elderlyId) ?: return "照护对象档案不存在"
        // ④ 家属账号必须存在
        if (!familyUserStore.phoneExists(elderlyId)) return "家属账号不存在"
        // ⑤ 同申请方已有 ACTIVE 绑定 → 不允许重复授权
        if (bindingDao.getBindingsByUser(requester.phone, BindingLifecycle.ACTIVE.name)
                .any { it.elderlyId == elderlyId }
        ) {
            return "您已绑定该照护对象，无需重复授权"
        }
        // ⑥ 同申请方已有 PENDING → 不允许重复提交
        if (bindingDao.getRequestsByRequester(requester.phone)
                .any { it.elderlyId == elderlyId && it.status == BindingStatus.PENDING.name }
        ) {
            return "已有待处理的绑定申请"
        }
        // ⑦ REVOKED / REJECTED / CANCELLED 均可重新申请，不拦截

        val now = System.currentTimeMillis()
        val requestId = newId()
        bindingDao.insertBindingRequest(
            BindingRequestEntity(
                id = requestId,
                requesterUserId = requester.phone,
                requesterRole = requester.role.name,
                organizationId = requester.organizationId ?: "",
                familyUserId = elderlyId,
                elderlyId = elderlyId,
                deviceId = profile.deviceSn,
                status = BindingStatus.PENDING.name,
                message = message?.trim().orEmpty(),
                createdAt = now,
                updatedAt = now
            )
        )
        // 家属 SYSTEM 消息（我的页授权管理红角标数据源，进入授权管理页批量已读；
        // remoteId 前缀 binding_ 与提醒计划等系统消息隔离，getByRemoteId 去重兜底）
        val remoteId = "binding_$requestId"
        if (messageDao.getByRemoteId(remoteId) == null) {
            val orgName = orgEnrich(requester.organizationId ?: "").first
            val elderlyName = profile.name.ifBlank { elderlyId }
            messageDao.insert(
                MessageEntity(
                    msgType = MessageEntity.MSG_TYPE_SYSTEM,
                    senderName = orgName,
                    content = "${orgName}申请绑定照护对象「${elderlyName}」，请前往授权管理处理",
                    createTime = now,
                    isRead = false,
                    deviceSerial = profile.deviceSn,
                    remoteId = remoteId,
                    sendStatus = MessageEntity.SEND_STATUS_SUCCESS,
                    sendChannel = MessageEntity.CHANNEL_NONE
                )
            )
        }
        return null
    }

    // ==================== 家属同意 / 拒绝 ====================

    /** 家属同意：PENDING → APPROVED，并原子创建 ACTIVE 绑定（重复授权去重）。 */
    suspend fun approve(requestId: String, reviewerFamilyUserId: String): String? {
        val request = bindingDao.getBindingRequest(requestId) ?: return "申请不存在"
        if (request.status != BindingStatus.PENDING.name) return "该申请已处理"
        if (request.familyUserId != reviewerFamilyUserId) return "无权审核该申请"
        val now = System.currentTimeMillis()
        bindingDao.approveAndCreateBinding(
            request.copy(
                status = BindingStatus.APPROVED.name,
                reviewedAt = now,
                updatedAt = now
            ),
            UserElderlyBindingEntity(
                id = newId(),
                userId = request.requesterUserId,
                userRole = request.requesterRole,
                organizationId = request.organizationId,
                elderlyId = request.elderlyId,
                deviceId = request.deviceId,
                permission = Permission.VIEW.name,
                status = BindingLifecycle.ACTIVE.name,
                createdAt = now,
                updatedAt = now
            )
        )
        return null
    }

    /** 家属拒绝：PENDING → REJECTED，不创建绑定。 */
    suspend fun reject(requestId: String, reviewerFamilyUserId: String): String? {
        val request = bindingDao.getBindingRequest(requestId) ?: return "申请不存在"
        if (request.status != BindingStatus.PENDING.name) return "该申请已处理"
        if (request.familyUserId != reviewerFamilyUserId) return "无权审核该申请"
        val now = System.currentTimeMillis()
        bindingDao.updateRequestStatus(requestId, BindingStatus.REJECTED.name, now, now)
        return null
    }

    // ==================== 解除授权 ====================

    /** 解除绑定：操作者为绑定所属工作人员本人，或该老人的家属。ACTIVE → REVOKED。 */
    suspend fun revoke(bindingId: String, operatorUserId: String): String? {
        val binding = bindingDao.getBinding(bindingId) ?: return "绑定不存在"
        if (binding.userId != operatorUserId && binding.elderlyId != operatorUserId) {
            return "无权解除该绑定"
        }
        if (binding.status != BindingLifecycle.ACTIVE.name) return "绑定已解除"
        bindingDao.updateBindingStatus(bindingId, BindingLifecycle.REVOKED.name, System.currentTimeMillis())
        return null
    }

    // ==================== 查询（家属侧） ====================

    suspend fun getIncomingRequests(familyUserId: String): List<RequestUi> =
        enrichRequests(bindingDao.getRequestsByFamilyUser(familyUserId))

    /** 家属侧实时观察全部申请（待处理/已同意/已拒绝），Room 失效自动刷新。 */
    fun observeIncomingRequests(familyUserId: String): Flow<List<RequestUi>> =
        bindingDao.observeRequestsByFamilyUser(familyUserId).map { enrichRequests(it) }

    /** 家属授权管理：该老人的全部绑定（ACTIVE + REVOKED，按创建时间倒序）。 */
    suspend fun getBindingsForFamily(elderlyId: String): List<BindingUi> =
        enrichBindings(
            bindingDao.getBindingsByElderly(elderlyId, BindingLifecycle.ACTIVE.name) +
                bindingDao.getBindingsByElderly(elderlyId, BindingLifecycle.REVOKED.name)
        ).sortedByDescending { it.createdAt }

    // ==================== 查询（工作人员侧） ====================

    /** 我的申请（全部状态，用于社区/医院资质管理）。 */
    suspend fun getSentRequests(requesterUserId: String): List<RequestUi> =
        enrichRequests(bindingDao.getRequestsByRequester(requesterUserId))

    /** 已绑定用户（ACTIVE，社区/医院资质管理解除绑定用）。 */
    suspend fun getBindingsForStaff(userId: String): List<BindingUi> =
        enrichBindings(bindingDao.getBindingsByUser(userId, BindingLifecycle.ACTIVE.name))

    /**
     * 可申请绑定的老人列表（社区/医院选择老人用）。
     * 过滤掉本申请方已 ACTIVE 绑定、或有 PENDING 申请的老人；REVOKED 后可重新申请，保留在列表中。
     * 跨申请方互不影响：社区和医院可分别授权同一老人。
     */
    suspend fun getAvailableElderly(requester: AppUser): List<ElderlyProfile> {
        val activeElderly = bindingDao
            .getBindingsByUser(requester.phone, BindingLifecycle.ACTIVE.name)
            .map { it.elderlyId }
            .toSet()
        val pendingElderly = bindingDao
            .getRequestsByRequester(requester.phone)
            .filter { it.status == BindingStatus.PENDING.name }
            .map { it.elderlyId }
            .toSet()
        return profileStore.getAllProfiles()
            .filter { it.userId.isNotBlank() && it.userId !in activeElderly && it.userId !in pendingElderly }
            .sortedBy { it.name }
    }

    // ==================== 权限访问（第四阶段） ====================

    /**
     * 当前用户有权访问的老人列表。
     * FAMILY → 本人档案（一人一档）；COMMUNITY/HOSPITAL → 本人 ACTIVE 绑定的老人（按 userId 隔离，与机构无关）。
     */
    suspend fun getAccessibleElderly(currentUser: AppUser): List<AccessibleElderlyUi> {
        return if (currentUser.role == UserRole.FAMILY) {
            profileStore.getAllProfiles()
                .filter { it.userId == currentUser.phone }
                .map { AccessibleElderlyUi(it.userId, it, 0L, "") }
        } else {
            bindingDao.getBindingsByUser(currentUser.phone, BindingLifecycle.ACTIVE.name)
                .mapNotNull { b ->
                    val p = profileStore.getPrimaryProfile(b.elderlyId) ?: return@mapNotNull null
                    AccessibleElderlyUi(p.userId, p, b.createdAt, orgEnrich(b.organizationId).first)
                }
        }
    }

    /**
     * 实时观察可访问老人（权限过滤核心）。
     * COMMUNITY/HOSPITAL：ACTIVE 绑定 Flow × 档案 Flow 组合 —— 解绑立即消失，家属改档案自动刷新。
     * FAMILY：观察本人档案流（含修改自动刷新）。
     */
    fun observeAccessibleElderly(currentUser: AppUser): Flow<List<AccessibleElderlyUi>> {
        return if (currentUser.role == UserRole.FAMILY) {
            profileStore.observeProfiles().map { list ->
                list.filter { it.userId == currentUser.phone }
                    .map { AccessibleElderlyUi(it.userId, it, 0L, "") }
            }
        } else {
            combine(
                profileStore.observeProfiles(),
                bindingDao.observeBindingsByUser(currentUser.phone, BindingLifecycle.ACTIVE.name)
            ) { profiles, bindings ->
                val byId = profiles.associateBy { it.userId }
                bindings.mapNotNull { b ->
                    val p = byId[b.elderlyId] ?: return@mapNotNull null
                    AccessibleElderlyUi(p.userId, p, b.createdAt, orgEnrich(b.organizationId).first)
                }
            }
        }
    }

    /**
     * 按 elderlyId 读取老人档案（带权限校验）。
     * FAMILY → 仅本人档案；COMMUNITY/HOSPITAL → 仅本人 ACTIVE 绑定的老人。无权限返回 null。
     * 社区/医院共享 UserDetail 走此方法；禁止 UI 层直接 getAllProfiles 绕过权限。
     */
    suspend fun getAccessibleElderlyById(currentUser: AppUser, elderlyId: String): ElderlyProfile? {
        val profile = profileStore.getPrimaryProfile(elderlyId) ?: return null
        if (currentUser.role == UserRole.FAMILY) {
            return if (profile.userId == currentUser.phone) profile else null
        }
        val authorized = bindingDao.getBindingsByUser(currentUser.phone, BindingLifecycle.ACTIVE.name)
            .any { it.elderlyId == elderlyId }
        return if (authorized) profile else null
    }

    /** 设备阶段前的关联准备：从老人档案读取设备 SN（社区/医院页面设备状态展示用）。 */
    fun getDeviceForElderly(profile: ElderlyProfile): String = profile.deviceSn

    // ==================== 设备访问链路（第五阶段） ====================

    /**
     * 当前用户可访问的设备（收口链路：userId → ACTIVE 绑定 → elderlyId → 档案 → deviceSn）。
     * 家属侧无自身绑定行，本人档案（userId = 手机号）即设备所有者（与第四阶段 FAMILY 语义一致）。
     */
    data class AccessibleDevice(
        val elderlyId: String,            // = profile.userId
        val deviceSn: String,
        val deviceValidateCode: String,
        val deviceBound: Boolean
    )

    /**
     * 解析当前登录用户可访问的设备（家属优先，其次社区/医院工作人员）。
     * 家属：本人档案且 deviceSn 非空；工作人员：本人 ACTIVE 绑定老人的档案且 deviceSn 非空（取第一个）。
     * 任一步骤失败 / deviceSn 为空 → null（无可访问设备）。禁止回退 DeviceBindingStore 旧缓存值。
     */
    suspend fun getCurrentUserDevice(): AccessibleDevice? {
        val familyUid = familyUserStore.getCurrentUserId()
        if (familyUid != null) {
            return profileStore.getPrimaryProfile(familyUid)
                ?.takeIf { it.deviceSn.isNotBlank() }
                ?.let { AccessibleDevice(it.userId, it.deviceSn, it.deviceValidateCode, it.deviceBound) }
        }
        val staff = staffUserStore.getCurrentStaffUser() ?: return null
        return getAccessibleElderly(staff).firstNotNullOfOrNull { a ->
            val p = a.profile
            if (p.deviceSn.isBlank()) null
            else AccessibleDevice(a.elderlyId, p.deviceSn, p.deviceValidateCode, p.deviceBound)
        }
    }

    /** 校验 deviceSerial 是否属于当前用户可访问的设备（路由/入口统一权限闸门）。 */
    suspend fun isDeviceAccessible(deviceSerial: String): Boolean =
        getCurrentUserDevice()?.deviceSn == deviceSerial

    /**
     * 观察当前登录用户可访问设备（实时流）。
     * 家属：观察本人档案流；工作人员：观察 ACTIVE 绑定 × 档案组合流（复用 observeAccessibleElderly）。
     * 解绑 → 流发 null；切换老人 → 旧设备消失、新设备出现。禁止回退 DeviceBindingStore。
     */
    suspend fun observeCurrentUserDevice(): Flow<AccessibleDevice?> {
        val familyUid = familyUserStore.getCurrentUserId()
        if (familyUid != null) {
            return profileStore.observeProfiles().map { profiles ->
                profiles.firstOrNull { it.userId == familyUid }
                    ?.takeIf { it.deviceSn.isNotBlank() }
                    ?.let { AccessibleDevice(it.userId, it.deviceSn, it.deviceValidateCode, it.deviceBound) }
            }
        }
        val staff = staffUserStore.getCurrentStaffUser() ?: return flowOf(null)
        return observeAccessibleElderly(staff).map { list ->
            list.firstNotNullOfOrNull { a ->
                val p = a.profile
                if (p.deviceSn.isBlank()) null
                else AccessibleDevice(a.elderlyId, p.deviceSn, p.deviceValidateCode, p.deviceBound)
            }
        }
    }
}
