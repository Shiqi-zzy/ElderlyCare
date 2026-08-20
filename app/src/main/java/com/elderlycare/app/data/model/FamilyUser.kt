package com.elderlycare.app.data.model

/**
 * 家属用户账号（本地持久化）。
 * 手机号作为唯一 userId，老人档案与业务数据都通过 userId 绑定。
 */
data class FamilyUser(
    val phone: String = "",        // 唯一 userId
    val name: String = "",
    val password: String = "",     // 演示用明文存储；生产需后端 + 加盐哈希 + 短信验证
    val identityCard: String = "", // 身份信息（可选）
    val contact: String = ""       // 联系方式（可选）
)
