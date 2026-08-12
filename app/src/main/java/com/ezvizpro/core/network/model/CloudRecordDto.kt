package com.ezvizpro.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 统一播放地址 DTO（适配新版萤石统一播放地址接口）
 *
 * 接口: POST /api/lapp/v2/live/address/get
 * 通过 type 参数区分: 1=直播预览, 2=本地录像回放, 3=云存储回放
 */
@Serializable
data class PlaybackAddressDto(
    @SerialName("id")
    val id: String? = null,            // 播放地址唯一标识
    @SerialName("url")
    val url: String                     // 播放地址（ezopen/flv/rtmp 协议）
)

// 以下为旧版云存储接口 DTO，当前已废弃，保留以备后续迁移
@Serializable
data class CloudRecordDto(
    @SerialName("recordId")
    val recordId: String,
    @SerialName("deviceSerial")
    val deviceSerial: String,
    @SerialName("channelNo")
    val channelNo: Int = 1,
    @SerialName("startTime")
    val startTime: String,
    @SerialName("endTime")
    val endTime: String,
    @SerialName("recordType")
    val recordType: Int,
    @SerialName("recordSize")
    val recordSize: Long? = null,
    @SerialName("coverPic")
    val coverPic: String? = null
)
