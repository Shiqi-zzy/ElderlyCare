package com.elderlycare.app.data.binding

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 多端绑定关系 Room 实体 + 枚举（数据库 elderly_binding.db）。
 *
 * 注意：本文件只包含关系数据实体 —— Organization / BindingRequest /
 * UserElderlyBinding / LocalAlert。账号不落 Room：家属走 FamilyUserStore、
 * 社区/医院走 UserStore（DataStore）。严禁新增 User 表。
 */

// ===== 枚举 =====

/** 机构类型 */
enum class OrganizationType(val label: String) {
    COMMUNITY("社区"),
    HOSPITAL("医院")
}

/** 绑定申请状态 */
enum class BindingStatus(val label: String) {
    PENDING("待处理"),
    APPROVED("已同意"),
    REJECTED("已拒绝"),
    CANCELLED("已取消")
}

/** 绑定关系生命周期状态 */
enum class BindingLifecycle(val label: String) {
    ACTIVE("生效"),
    REVOKED("已解除")
}

/** 访问权限（当前统一 VIEW，预留扩展） */
enum class Permission(val label: String) {
    VIEW("查看")
}

/** 本地告警处理状态 */
enum class AlertStatus(val label: String) {
    UNREAD("未读"),
    READ("已读"),
    HANDLED("已处理")
}

/** 本地告警风险等级 */
enum class AlertLevel(val label: String) {
    LOW("低"),
    NORMAL("一般"),
    ATTENTION("关注"),
    RISK("风险")
}

// ===== 机构 =====

/**
 * 社区/医院机构。一个机构可有多个工作人员（AppUser.organizationId 关联）。
 */
@Entity(tableName = "organization")
data class OrganizationEntity(
    @PrimaryKey val id: String,
    val name: String,
    /** 机构类型：OrganizationType.name（COMMUNITY / HOSPITAL） */
    val type: String = OrganizationType.COMMUNITY.name,
    val createdAt: Long = 0L
)

// ===== 绑定申请 =====

/**
 * 社区/医院 → 家属 的绑定申请。
 * 未批准前申请方不获得任何老人数据；家属同意后经 BindingRepository 建立
 * UserElderlyBinding 并授权。
 */
@Entity(
    tableName = "binding_request",
    indices = [
        Index(value = ["familyUserId"]),
        Index(value = ["elderlyId"]),
        Index(value = ["requesterUserId"])
    ]
)
data class BindingRequestEntity(
    @PrimaryKey val id: String,
    /** 申请方（社区/医院工作人员手机号） */
    val requesterUserId: String,
    /** 申请方角色：UserRole.name（COMMUNITY / HOSPITAL） */
    val requesterRole: String,
    /** 申请方所属机构 id */
    val organizationId: String,
    /** 目标老人所在家属手机号 */
    val familyUserId: String,
    /**
     * 目标老人 id。
     * 当前值 = 家属手机号 / profile.userId（一人一档）；业务层统一用 elderlyId 命名，
     * 为将来「一家属多老人」预留。
     */
    val elderlyId: String,
    /** 目标老人设备序列号（来自档案 deviceSn，可为空） */
    val deviceId: String = "",
    /** 申请状态：BindingStatus.name */
    val status: String = BindingStatus.PENDING.name,
    /** 申请说明（可选） */
    val message: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    /** 家属审核时间（同意/拒绝时写入） */
    val reviewedAt: Long? = null
)

// ===== 正式绑定关系 =====

/**
 * 家属同意绑定申请后建立的访问关系。
 * 社区/医院据此获得老人档案、设备、健康档案与告警的访问权。
 */
@Entity(
    tableName = "user_elderly_binding",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["elderlyId"])
    ]
)
data class UserElderlyBindingEntity(
    @PrimaryKey val id: String,
    /** 获权方（社区/医院工作人员手机号） */
    val userId: String,
    /** 获权方角色：UserRole.name（COMMUNITY / HOSPITAL） */
    val userRole: String,
    /** 获权方所属机构 id */
    val organizationId: String,
    /** 老人 id（当前 = 家属手机号 / profile.userId） */
    val elderlyId: String,
    /** 老人设备序列号 */
    val deviceId: String = "",
    /** 权限：Permission.name，默认 VIEW */
    val permission: String = Permission.VIEW.name,
    /** 生命周期状态：BindingLifecycle.name（ACTIVE / REVOKED） */
    val status: String = BindingLifecycle.ACTIVE.name,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

// ===== 本地告警（未来三端共享的唯一告警源） =====

/**
 * 设备告警的本地镜像，三端（家属/社区/医院）都通过绑定关系读取同一份数据，
 * 按各自有权限的 elderlyId 过滤，不复制三套告警。
 */
@Entity(
    tableName = "local_alert",
    indices = [
        Index(value = ["deviceId"]),
        Index(value = ["elderlyId"])
    ]
)
data class LocalAlertEntity(
    @PrimaryKey val id: String,
    /** 设备序列号 */
    val deviceId: String,
    /** 老人 id（当前 = 家属手机号 / profile.userId） */
    val elderlyId: String,
    /** 告警类型（如 跌倒 / 久坐 / 设备离线） */
    val type: String = "",
    /** 风险等级：AlertLevel.name */
    val level: String = AlertLevel.NORMAL.name,
    /** 告警内容 */
    val content: String = "",
    /** 告警时间戳（毫秒） */
    val timestamp: Long = 0L,
    /** 处理状态：AlertStatus.name */
    val status: String = AlertStatus.UNREAD.name
)
