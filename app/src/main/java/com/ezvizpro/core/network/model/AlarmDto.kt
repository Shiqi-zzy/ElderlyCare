package com.ezvizpro.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 报警消息 DTO
 */
@Serializable
data class AlarmDto(
    @SerialName("alarmId")
    val alarmId: String,
    @SerialName("deviceSerial")
    val deviceSerial: String,
    @SerialName("channelNo")
    val channelNo: Int = 1,
    @SerialName("alarmName")
    val alarmName: String,           // 告警类型名称
    @SerialName("alarmType")
    val alarmType: Int,              // 10000=移动侦测, 10001=人形检测, 10002=越界侦测等
    @SerialName("alarmTime")
    val alarmTime: String,           // yyyy-MM-dd HH:mm:ss
    @SerialName("alarmPicUrl")
    val alarmPicUrl: String? = null, // 告警截图
    @SerialName("alarmVideoUrl")
    val alarmVideoUrl: String? = null, // 告警短视频 (如有)
    @SerialName("isRead")
    val isRead: Int = 0,             // 0=未读, 1=已读
    @SerialName("isChecked")
    val isChecked: Int = 0,          // 0=未确认, 1=已确认
    @SerialName("deviceName")
    val deviceName: String? = null,   // 设备名称
    @SerialName("preRecordUrl")
    val preRecordUrl: String? = null  // 预录视频 URL
)
