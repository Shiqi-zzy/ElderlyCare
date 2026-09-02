package com.elderlycare.app.data.model

/**
 * 社区/医院工作人员账号（本地持久化，DataStore `staff_data`）。
 *
 * 仅服务 COMMUNITY / HOSPITAL 角色；家属账号仍使用 [FamilyUser]（`family_data`）。
 * 手机号作为唯一 userId；password 为演示用明文存储（生产需后端 + 加盐哈希 + 短信验证码）。
 */
data class AppUser(
    /** 唯一 userId = 手机号 */
    val phone: String = "",
    val name: String = "",
    /** 演示用明文密码 */
    val password: String = "",
    /** 账号角色：仅 COMMUNITY / HOSPITAL（FAMILY 走 FamilyUser） */
    val role: UserRole = UserRole.COMMUNITY,
    /** 所属机构 id（OrganizationEntity.id），可为空（个人注册场景） */
    val organizationId: String? = null,
    /** 账号状态：ACTIVE / DISABLED */
    val status: String = "ACTIVE",
    /**
     * 工作资格审核状态：PENDING / APPROVED / REJECTED。
     * Gson 缺字段（旧账号）→ null，读取口径按「null 视为已通过」处理，不锁历史/演示账号。
     */
    val qualification: String? = null,
    /** 职位（如 网格员 / 主治医师），可选 */
    val title: String = "",
    /** 负责楼栋编码列表（仅社区网格员；每栋责任唯一、不共管） */
    val areaBuildings: List<String> = emptyList(),
    /** 创建时间戳（毫秒） */
    val createdAt: Long = 0L
)

/** 工作资格审核状态 */
enum class QualificationStatus(val label: String) {
    PENDING("审核中"),
    APPROVED("已通过"),
    REJECTED("已驳回")
}

/** 全局角色枚举（统一三端身份） */
enum class UserRole(val label: String) {
    FAMILY("家属"),
    COMMUNITY("社区"),
    HOSPITAL("医院")
}
