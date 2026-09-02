package com.elderlycare.app.data.incident

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * 事件与处罚 DAO（挂 AppDatabase / elderly_care.db）。
 * 跨账号「实时」靠 Room Flow：三账号共用同一库，任一端改状态，其余端切前台自动刷新。
 */
@Dao
interface IncidentDao {

    // ==================== 事件 ====================

    @Insert
    suspend fun insert(incident: IncidentEntity): Long

    @Update
    suspend fun update(incident: IncidentEntity)

    @Query("SELECT * FROM incident WHERE id = :id")
    suspend fun getById(id: Long): IncidentEntity?

    @Query("SELECT * FROM incident WHERE id = :id")
    fun observeById(id: Long): Flow<IncidentEntity?>

    /** 社区网格员：本人负责的全部事件（新→旧） */
    @Query("SELECT * FROM incident WHERE communityStaffId = :staffId ORDER BY triggeredAt DESC")
    fun observeByCommunityStaff(staffId: String): Flow<List<IncidentEntity>>

    /** 社区网格员：未关闭事件（工作台/待办用） */
    @Query(
        "SELECT * FROM incident WHERE communityStaffId = :staffId " +
            "AND status NOT IN ('CLOSED','SELF_CLOSED') ORDER BY triggeredAt DESC"
    )
    fun observeActiveByCommunityStaff(staffId: String): Flow<List<IncidentEntity>>

    /** 医院：本院事件（在班过滤放仓库层） */
    @Query("SELECT * FROM incident WHERE hospitalOrgId = :orgId ORDER BY triggeredAt DESC")
    fun observeByHospital(orgId: String): Flow<List<IncidentEntity>>

    /** 医院：本院未关闭事件（大屏/近期事件用） */
    @Query(
        "SELECT * FROM incident WHERE hospitalOrgId = :orgId " +
            "AND status NOT IN ('CLOSED','SELF_CLOSED') ORDER BY triggeredAt DESC"
    )
    fun observeActiveByHospital(orgId: String): Flow<List<IncidentEntity>>

    /** 家属：本人名下老人的事件 */
    @Query("SELECT * FROM incident WHERE elderlyId IN (:elderlyIds) ORDER BY triggeredAt DESC")
    fun observeByFamily(elderlyIds: List<String>): Flow<List<IncidentEntity>>

    /** 社区大屏：本社区未关闭事件（仓库层聚合到 8 栋） */
    @Query(
        "SELECT * FROM incident WHERE communityOrgId = :orgId " +
            "AND status NOT IN ('CLOSED','SELF_CLOSED')"
    )
    suspend fun getActiveByCommunity(orgId: String): List<IncidentEntity>

    /** 医院大屏：本院未关闭事件（仓库层聚合到绑定社区） */
    @Query(
        "SELECT * FROM incident WHERE hospitalOrgId = :orgId " +
            "AND status IN ('DISPATCH_REQUESTED','URGENT','ESCALATED','ESCALATED_UNANSWERED')"
    )
    suspend fun getActiveByHospital(orgId: String): List<IncidentEntity>

    /** 调度器扫描：已请求出警/加急中、且尚无医生接走的事件 */
    @Query(
        "SELECT * FROM incident WHERE status IN ('DISPATCH_REQUESTED','URGENT') " +
            "AND hospitalDoctorId IS NULL"
    )
    suspend fun getWaitingForDoctor(): List<IncidentEntity>

    /** 已升级、等待承接人响应的事件（用于 escalation_fault 判定） */
    @Query("SELECT * FROM incident WHERE status = 'ESCALATED' AND hospitalDoctorId IS NULL")
    suspend fun getEscalatedWaiting(): List<IncidentEntity>

    /**
     * 一键处警 CAS（先接先得）：仅当尚无医生接警时占位成功，返回受影响行数（1=抢到，0=已被接走）。
     * 用单条 UPDATE 原子占位，避免两医生并发同时接警。
     */
    @Query(
        "UPDATE incident SET hospitalDoctorId = :doctorId, status = 'HOSPITAL_ACCEPTED', " +
            "hospitalAcceptedAt = :ts WHERE id = :id AND hospitalDoctorId IS NULL"
    )
    suspend fun casAccept(id: Long, doctorId: String, ts: Long): Int

    @Query("SELECT COUNT(*) FROM incident WHERE hospitalDoctorId = :doctorId AND status IN ('CLOSED')")
    suspend fun countAcceptedByDoctor(doctorId: String): Int

    /** 已完成处警的事件平均响应毫秒（dispatchRequestedAt → hospitalAcceptedAt） */
    @Query(
        "SELECT AVG(hospitalAcceptedAt - dispatchRequestedAt) FROM incident " +
            "WHERE hospitalDoctorId = :doctorId AND hospitalAcceptedAt IS NOT NULL " +
            "AND dispatchRequestedAt IS NOT NULL"
    )
    suspend fun avgResponseMsByDoctor(doctorId: String): Double?

    // ==================== 医生处罚 ====================

    @Insert
    suspend fun insertPenalty(penalty: DoctorPenaltyEntity): Long

    @Query("SELECT * FROM doctor_penalty WHERE doctorId = :doctorId ORDER BY createdAt DESC")
    fun observePenalties(doctorId: String): Flow<List<DoctorPenaltyEntity>>

    @Query("SELECT * FROM doctor_penalty WHERE doctorId = :doctorId ORDER BY createdAt DESC")
    suspend fun getPenalties(doctorId: String): List<DoctorPenaltyEntity>

    @Query("SELECT IFNULL(SUM(scoreDelta),0) FROM doctor_penalty WHERE doctorId = :doctorId AND status='active'")
    suspend fun sumActivePenalty(doctorId: String): Int

    @Query(
        "SELECT COUNT(*) FROM doctor_penalty WHERE doctorId = :doctorId " +
            "AND status='active' AND penaltyType='missed_response'"
    )
    suspend fun countActiveMissed(doctorId: String): Int

    @Query("UPDATE doctor_penalty SET status='revoked', revokedAt=:ts, revokeReason=:reason WHERE id=:pid")
    suspend fun revokePenalty(pid: Long, ts: Long, reason: String)
}
