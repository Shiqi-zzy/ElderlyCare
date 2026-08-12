package com.ezvizpro.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 直播地址 DTO
 */
@Serializable
data class LiveAddressDto(
    @SerialName("deviceSerial")
    val deviceSerial: String,
    @SerialName("channelNo")
    val channelNo: Int = 1,
    @SerialName("hls")
    val hls: String? = null,           // HLS 拉流地址 (推荐)
    @SerialName("hlsHd")
    val hlsHd: String? = null,         // HLS 高清地址
    @SerialName("rtmp")
    val rtmp: String? = null,          // RTMP 拉流地址
    @SerialName("rtmpHd")
    val rtmpHd: String? = null,        // RTMP 高清地址
    @SerialName("flv")
    val flv: String? = null,           // FLV 拉流地址 (低延迟)
    @SerialName("flvHd")
    val flvHd: String? = null,         // FLV 高清地址
    @SerialName("liveId")
    val liveId: String? = null,        // 直播会话 ID（用于关闭直播）
    @SerialName("expireTime")
    val expireTime: Long? = null       // 流地址过期时间
)

/**
 * 打开直播通道 DTO
 */
@Serializable
data class LiveOpenDto(
    @SerialName("deviceSerial")
    val deviceSerial: String,
    @SerialName("channelNo")
    val channelNo: Int = 1,
    @SerialName("ret")
    val ret: String = "0"             // 0=成功
)

/**
 * 预置位 DTO
 */
@Serializable
data class PresetDto(
    @SerialName("index")
    val index: Int,                    // 预置位编号
    @SerialName("presetName")
    val presetName: String = ""        // 预置位名称
)
