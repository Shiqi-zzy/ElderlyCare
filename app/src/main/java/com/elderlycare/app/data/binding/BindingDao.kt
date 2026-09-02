package com.elderlycare.app.data.binding

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * 多端绑定关系数据访问接口（五张表的基础 CRUD / Flow）。
 *
 * 全部返回 suspend 或 Flow，在 Room 自有调度器执行，不阻塞 UI。
 */
@Dao
interface BindingDao {

    // ==================== 机构 Organization ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrganization(org: OrganizationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrganizations(orgs: List<OrganizationEntity>)

    @Query("SELECT * FROM organization WHERE id = :id")
    suspend fun getOrganization(id: String): OrganizationEntity?

    @Query("SELECT * FROM organization ORDER BY createdAt ASC")
    suspend fun getAllOrganizations(): List<OrganizationEntity>

    @Query("SELECT * FROM organization ORDER BY createdAt ASC")
    fun observeAllOrganizations(): Flow<List<OrganizationEntity>>

    @Query("SELECT * FROM organization WHERE type = :type ORDER BY createdAt ASC")
    suspend fun getOrganizationsByType(type: String): List<OrganizationEntity>

    @Query("SELECT COUNT(*) FROM organization")
    suspend fun countOrganizations(): Int

    // ==================== 绑定申请 BindingRequest ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBindingRequest(request: BindingRequestEntity)

    @Update
    suspend fun updateBindingRequest(request: BindingRequestEntity)

    @Query("SELECT * FROM binding_request WHERE id = :id")
    suspend fun getBindingRequest(id: String): BindingRequestEntity?

    /** 家属侧：按家属手机号观察收到的全部申请（含待处理/已同意/已拒绝） */
    @Query("SELECT * FROM binding_request WHERE familyUserId = :familyUserId ORDER BY createdAt DESC")
    fun observeRequestsByFamilyUser(familyUserId: String): Flow<List<BindingRequestEntity>>

    @Query("SELECT * FROM binding_request WHERE familyUserId = :familyUserId ORDER BY createdAt DESC")
    suspend fun getRequestsByFamilyUser(familyUserId: String): List<BindingRequestEntity>

    @Query("SELECT * FROM binding_request WHERE requesterUserId = :requesterUserId ORDER BY createdAt DESC")
    suspend fun getRequestsByRequester(requesterUserId: String): List<BindingRequestEntity>

    @Query("SELECT * FROM binding_request WHERE status = :status ORDER BY createdAt DESC")
    suspend fun getRequestsByStatus(status: String): List<BindingRequestEntity>

    @Query(
        "UPDATE binding_request SET status = :status, reviewedAt = :reviewedAt, " +
            "updatedAt = :updatedAt WHERE id = :id"
    )
    suspend fun updateRequestStatus(id: String, status: String, reviewedAt: Long?, updatedAt: Long)

    // ==================== 绑定关系 UserElderlyBinding ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBinding(binding: UserElderlyBindingEntity)

    @Update
    suspend fun updateBinding(binding: UserElderlyBindingEntity)

    @Query("SELECT * FROM user_elderly_binding WHERE id = :id")
    suspend fun getBinding(id: String): UserElderlyBindingEntity?

    /** 某工作人员全部有效绑定（社区/医院据此获得老人访问权） */
    @Query("SELECT * FROM user_elderly_binding WHERE userId = :userId AND status = :status ORDER BY createdAt DESC")
    suspend fun getBindingsByUser(userId: String, status: String): List<UserElderlyBindingEntity>

    @Query("SELECT * FROM user_elderly_binding WHERE userId = :userId AND status = :status ORDER BY createdAt DESC")
    fun observeBindingsByUser(userId: String, status: String): Flow<List<UserElderlyBindingEntity>>

    /** 某老人被授权给了哪些用户（含家属自身可查） */
    @Query("SELECT * FROM user_elderly_binding WHERE elderlyId = :elderlyId AND status = :status ORDER BY createdAt DESC")
    suspend fun getBindingsByElderly(elderlyId: String, status: String): List<UserElderlyBindingEntity>

    /** 某机构下全部有效绑定（医院经社区看老人、大屏聚合用） */
    @Query("SELECT * FROM user_elderly_binding WHERE organizationId = :orgId AND status = :status")
    suspend fun getBindingsByOrganization(orgId: String, status: String): List<UserElderlyBindingEntity>

    @Query("UPDATE user_elderly_binding SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateBindingStatus(id: String, status: String, updatedAt: Long)

    /** 家属同意申请：原子地写 APPROVED 申请 + 建 ACTIVE 绑定（按 userId+elderlyId 去重，不重复授权）。 */
    @Transaction
    suspend fun approveAndCreateBinding(
        request: BindingRequestEntity,
        binding: UserElderlyBindingEntity
    ) {
        updateBindingRequest(request)
        if (getBindingsByUser(binding.userId, BindingLifecycle.ACTIVE.name)
                .none { it.elderlyId == binding.elderlyId }
        ) {
            insertBinding(binding)
        }
    }

    // ==================== 本地告警 LocalAlert ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: LocalAlertEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlerts(alerts: List<LocalAlertEntity>)

    @Query("SELECT * FROM local_alert WHERE id = :id")
    suspend fun getAlert(id: String): LocalAlertEntity?

    @Query("SELECT * FROM local_alert WHERE elderlyId = :elderlyId ORDER BY timestamp DESC")
    suspend fun getAlertsByElderly(elderlyId: String): List<LocalAlertEntity>

    /** 按多个老人 id 观察告警（三端按各自可访问 elderlyId 过滤用） */
    @Query("SELECT * FROM local_alert WHERE elderlyId IN (:elderlyIds) ORDER BY timestamp DESC")
    fun observeAlertsByElderlies(elderlyIds: List<String>): Flow<List<LocalAlertEntity>>

    @Query("SELECT * FROM local_alert WHERE deviceId = :deviceId ORDER BY timestamp DESC")
    suspend fun getAlertsByDevice(deviceId: String): List<LocalAlertEntity>

    @Query("UPDATE local_alert SET status = :status WHERE id = :id")
    suspend fun updateAlertStatus(id: String, status: String)

    /** 按设备标记全部未处理告警为已处理（告警详情「标记已处理」兜底：告警非 WS 通道到达时无精确 id 匹配） */
    @Query("UPDATE local_alert SET status = :status WHERE deviceId = :deviceId AND status != :status")
    suspend fun updateAlertsByDevice(deviceId: String, status: String)

    // ==================== 医院-社区绑定（多对多，管理端审批）====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHcBinding(binding: HospitalCommunityBindingEntity): Long

    @Update
    suspend fun updateHcBinding(binding: HospitalCommunityBindingEntity)

    @Query("SELECT * FROM hospital_community_binding WHERE id = :id")
    suspend fun getHcBinding(id: Long): HospitalCommunityBindingEntity?

    /** 某医院的全部绑定申请/关系 */
    @Query("SELECT * FROM hospital_community_binding WHERE hospitalOrgId = :hospitalOrgId ORDER BY createdAt DESC")
    suspend fun getHcBindingsByHospital(hospitalOrgId: String): List<HospitalCommunityBindingEntity>

    @Query("SELECT * FROM hospital_community_binding WHERE hospitalOrgId = :hospitalOrgId ORDER BY createdAt DESC")
    fun observeHcBindingsByHospital(hospitalOrgId: String): Flow<List<HospitalCommunityBindingEntity>>

    /** 某医院已 ACTIVE 绑定的社区 id 列表（医院可见老人/大屏用） */
    @Query("SELECT communityOrgId FROM hospital_community_binding WHERE hospitalOrgId = :hospitalOrgId AND status = 'ACTIVE'")
    suspend fun getActiveCommunityIdsByHospital(hospitalOrgId: String): List<String>

    /** 某社区已 ACTIVE 绑定的医院列表（社区出警选医院用） */
    @Query("SELECT * FROM hospital_community_binding WHERE communityOrgId = :communityOrgId AND status = 'ACTIVE'")
    suspend fun getActiveHospitalsByCommunity(communityOrgId: String): List<HospitalCommunityBindingEntity>

    /** 管理端：待审批列表 */
    @Query("SELECT * FROM hospital_community_binding WHERE status = 'PENDING' ORDER BY createdAt ASC")
    fun observePendingHcBindings(): Flow<List<HospitalCommunityBindingEntity>>

    @Query("UPDATE hospital_community_binding SET status = :status, reviewedBy = :reviewedBy, reviewNote = :note, reviewedAt = :reviewedAt WHERE id = :id")
    suspend fun updateHcBindingStatus(id: Long, status: String, reviewedBy: String?, note: String, reviewedAt: Long?)
}
