package com.ezvizpro.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 设备基本信息 DTO
 */
@Serializable
data class DeviceDto(
    @SerialName("deviceSerial")
    val deviceSerial: String,
    @SerialName("deviceName")
    val deviceName: String,
    @SerialName("deviceType")
    val deviceType: String,
    @SerialName("status")
    val status: Int,              // 1=在线, 0=离线
    @SerialName("deviceCover")
    val deviceCover: String? = null,  // 设备封面图 URL
    @SerialName("category")
    val category: String? = null,     // 设备类别
    @SerialName("channelNo")
    val channelNo: Int = 1,
    @SerialName("isEncrypt")
    val isEncrypt: Int = 0,
    @SerialName("defence")
    val defence: Int = 0,         // 是否布防: 1=布防, 0=撤防
)

/**
 * 设备详情 DTO
 */
@Serializable
data class DeviceDetailDto(
    @SerialName("deviceSerial")
    val deviceSerial: String,
    @SerialName("deviceName")
    val deviceName: String,
    @SerialName("model")
    val model: String? = null,
    @SerialName("status")
    val status: Int,
    @SerialName("deviceCover")
    val deviceCover: String? = null,
    @SerialName("channelNo")
    val channelNo: Int = 1,
    @SerialName("version")
    val version: String? = null,
    @SerialName("capacity")
    val capacity: DeviceCapacityDto? = null,  // 设备能力集
)

/**
 * 设备能力集 DTO
 */
@Serializable
data class DeviceCapacityDto(
    @SerialName("supportPtz")
    val supportPtz: Int = 0,       // 是否支持云台: 1=支持
    @SerialName("supportTalk")
    val supportTalk: Int = 0,      // 是否支持对讲
    @SerialName("supportCloud")
    val supportCloud: Int = 0,     // 是否支持云存储
    @SerialName("supportWifi")
    val supportWifi: Int = 0,      // 是否支持 WiFi
)

/**
 * 设备在线状态 DTO
 */
@Serializable
data class DeviceStatusDto(
    @SerialName("deviceSerial")
    val deviceSerial: String,
    @SerialName("status")
    val status: Int
)
