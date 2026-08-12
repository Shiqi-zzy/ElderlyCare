package com.ezvizpro.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 获取 accessToken 的返回数据
 */
@Serializable
data class TokenDto(
    @SerialName("accessToken")
    val accessToken: String,
    @SerialName("expireTime")
    val expireTime: Long  // 过期时间戳（毫秒）
)

/**
 * Token 信息（应用内使用）
 */
data class TokenInfo(
    val accessToken: String,
    val expireTime: Long,        // 过期时间戳（毫秒）
    val fetchTime: Long          // 获取时间戳（毫秒）
)
