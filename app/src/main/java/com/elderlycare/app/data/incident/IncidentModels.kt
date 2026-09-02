package com.elderlycare.app.data.incident

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 跌倒等紧急事件四端协同处置 —— 状态机常量、配置与 Room 实体（数据库 elderly_care.db，表 incident）。
 *
 * 一条跌倒 = 一个 IncidentEntity，状态字段驱动三端按钮的显示/禁用，
 * 11 个可空时间戳保证「何时接收/开始处置/完成处置」完整可追溯。
 */

// ==================== 状态机 ====================

object IncidentStatus {
    const val RAISED = "RAISED"                               // 已触发，三方知会
    const val COMMUNITY_RECEIVED = "COMMUNITY_RECEIVED"       // 社区已接收（自动）
    const val DISPATCH_REQUESTED = "DISPATCH_REQUESTED"       // 社区已请求出警
    const val URGENT = "URGENT"                               // 加急中（15s/次）
    const val ESCALATED = "ESCALATED"                         // 3 次加急未接，已升级
    const val HOSPITAL_ACCEPTED = "HOSPITAL_ACCEPTED"         // 医生已处警（先接先得）
    const val HOSPITAL_DONE = "HOSPITAL_DONE"                 // 医院已处置完成
    const val COMMUNITY_DONE = "COMMUNITY_DONE"               // 社区已闭环
    const val CLOSED = "CLOSED"                               // 结束（转警路径）
    const val SELF_CLOSED = "SELF_CLOSED"                     // 社区自行闭环（旁路）
    const val ESCALATED_UNANSWERED = "ESCALATED_UNANSWERED"   // 升级后仍无人接

    val LABEL: Map<String, String> = mapOf(
        RAISED to "已触发",
        COMMUNITY_RECEIVED to "社区已接收",
        DISPATCH_REQUESTED to "已呼叫医院",
        URGENT to "加急中",
        ESCALATED to "已升级",
        HOSPITAL_ACCEPTED to "医院已出警",
        HOSPITAL_DONE to "医院已处置",
        COMMUNITY_DONE to "社区已闭环",
        CLOSED to "已结束",
        SELF_CLOSED to "社区自行闭环",
        ESCALATED_UNANSWERED to "医院暂无人响应"
    )

    fun labelOf(status: String): String = LABEL[status] ?: status

    /** 终态：不再流转 */
    fun isTerminal(status: String): Boolean =
        status == CLOSED || status == SELF_CLOSED

    /** 社区「处置完成」按钮仅在医院已完成（转警路径）时可用 */
    fun canCommunityClose(status: String): Boolean = status == HOSPITAL_DONE

    /** 社区「自行闭环」仅在社区刚接收、尚未转警时可用 */
    fun canSelfClose(status: String): Boolean = status == COMMUNITY_RECEIVED

    /** 社区「紧急出警」仅在社区接收后可用 */
    fun canDispatch(status: String): Boolean = status == COMMUNITY_RECEIVED

    /** 医院「一键处警」可用窗口：已请求出警 / 加急 / 已升级，且尚无医生接走 */
    fun canHospitalAccept(status: String): Boolean =
        status == DISPATCH_REQUESTED || status == URGENT || status == ESCALATED
}

// ==================== 处罚 ====================

object PenaltyType {
    const val MISSED = "missed_response"        // 在班漏接
    const val ESCALATION_FAULT = "escalation_fault" // 升级承接后仍漏接
    val LABEL = mapOf(MISSED to "值班漏接", ESCALATION_FAULT to "升级后仍未响应")
}

object PenaltyLevel {
    const val WARNING = "warning"
    const val NOTICE = "notice"
    const val SUSPENSION = "suspension"
    val LABEL = mapOf(WARNING to "警告", NOTICE to "院内通报", SUSPENSION to "暂停排班")
}

// ==================== 排班模式 ====================

object ScheduleMode {
    const val WEEKLY = 0 // 按周循环（weekday 1..7）
    const val ONCE = 1   // 指定日期（scheduleDate）
}

// ==================== 全局配置（已确认参数）====================

object IncidentConfig {
    /** 加急间隔 15 秒（演示可通过 [demoIntervalMs] 调小） */
    const val URGENT_INTERVAL_MS = 15_000L
    const val URGENT_MAX_TIMES = 3
    const val ESCALATE_GRACE_MS = 15_000L
    const val SCORE_INIT = 100
    const val SCORE_MISSED = -5
    const val SCORE_ESCALATION_FAULT = -10
    const val NOTICE_THRESHOLD = 3
    const val SUSPEND_THRESHOLD = 5
    const val TOTAL_BUILDINGS = 8

    /** 演示加速开关：>0 时用它替代 15s（设置页可改，0 表示用正式值） */
    @Volatile
    var demoIntervalMs: Long = 0L

    val urgentInterval: Long get() = if (demoIntervalMs > 0) demoIntervalMs else URGENT_INTERVAL_MS
}

// ==================== 事件实体 ====================

@Entity(
    tableName = "incident",
    indices = [
        Index("communityStaffId"),
        Index("hospitalOrgId"),
        Index("elderlyId"),
        Index("buildingNo"),
        Index("status"),
        Index(value = ["hospitalOrgId", "status"])
    ]
)
data class IncidentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** 业务编号 INC+yyyyMMdd+3 位序列，展示用 */
    val incidentNo: String,
    /** 关联 local_alert / message 的 remoteId，可为空 */
    val alarmId: String = "",
    val elderlyId: String,
    val elderlyName: String,
    val buildingNo: String,
    val unitNo: String = "",
    val roomNo: String = "",
    /** LOW / MEDIUM / HIGH / EMERGENCY */
    val level: String = "HIGH",
    // —— 责任方 ——
    val communityOrgId: String,
    /** 责任网格员（按楼栋唯一匹配） */
    val communityStaffId: String,
    val hospitalOrgId: String? = null,
    /** JSON 数组：转警时在班医生 id 列表（处罚依据） */
    val onDutyDoctorIds: String = "[]",
    /** JSON 数组：漏接医生 id 列表 */
    val missedDoctorIds: String = "[]",
    /** 实际处警医生（先接先得） */
    val hospitalDoctorId: String? = null,
    val escalatedToDoctorId: String? = null,
    val status: String,
    val urgentCount: Int = 0,
    // —— 11 个时间戳（毫秒，未发生为 null）——
    val triggeredAt: Long,
    val communityReceivedAt: Long? = null,
    val familyContactedAt: Long? = null,
    val dispatchRequestedAt: Long? = null,
    val hospitalReceivedAt: Long? = null,
    val urgentLastAt: Long? = null,
    val escalatedAt: Long? = null,
    val hospitalAcceptedAt: Long? = null,
    val hospitalDoneAt: Long? = null,
    val communityDoneAt: Long? = null,
    val closedAt: Long? = null,
    val createdAt: Long,
    // —— 文本记录 ——
    val familyNote: String = "",
    val hospitalTreatment: String = "",
    val communityNote: String = "",
    val communityRecordId: Long? = null,
    val hospitalRecordId: Long? = null
)

// ==================== 医生处罚实体 ====================

@Entity(
    tableName = "doctor_penalty",
    indices = [Index("doctorId"), Index("incidentId")]
)
data class DoctorPenaltyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val incidentId: Long,
    val doctorId: String,
    val doctorName: String,
    val hospitalOrgId: String,
    /** PenaltyType */
    val penaltyType: String,
    val urgentCountAtMiss: Int,
    /** PenaltyLevel */
    val level: String,
    val scoreDelta: Int,
    /** active / revoked */
    val status: String = "active",
    val createdAt: Long,
    val revokedAt: Long? = null,
    val revokeReason: String = ""
)

// ==================== 大屏 / 绩效聚合（非表）====================

/** 社区大屏：单栋方块 */
data class BuildingCell(
    val buildingNo: String,
    /** 负责网格员姓名，null=未分配 */
    val staffName: String?,
    val staffId: String?,
    val elderlyCount: Int,
    val activeIncidentCount: Int
) {
    val assigned: Boolean get() = staffName != null
    val hasAlarm: Boolean get() = activeIncidentCount > 0
}

/** 医院大屏：单个绑定社区方块 */
data class CommunityCell(
    val communityOrgId: String,
    val communityName: String,
    val activeIncidentCount: Int
)

/** 医生值班绩效聚合 */
data class DoctorPerformance(
    val doctorId: String,
    val doctorName: String,
    val score: Int,
    val acceptedCount: Int,
    val missedCount: Int,
    /** 平均响应秒数（转警→处警），无记录为 null */
    val avgResponseSeconds: Long?,
    val activePenalties: List<DoctorPenaltyEntity>
) {
    val suspended: Boolean get() = activePenalties.any { it.level == PenaltyLevel.SUSPENSION && it.status == "active" }
}
