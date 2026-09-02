package com.elderlycare.app.data.ezviz.model

/**
 * 设备领域模型
 */
data class Device(
    val deviceSerial: String,
    val deviceName: String,
    val deviceType: String,
    val status: DeviceStatus,
    val deviceCover: String?,
    val category: String?,
    val channelNo: Int,
    val isEncrypt: Boolean,
    val defence: Boolean,
    val supportPtz: Boolean = false,
    val supportTalk: Boolean = false,
    val supportCloud: Boolean = false,
    val supportWifi: Boolean = false
)

enum class DeviceStatus(val value: Int, val label: String) {
    ONLINE(1, "在线"),
    OFFLINE(0, "离线");

    companion object {
        fun fromValue(value: Int): DeviceStatus =
            entries.find { it.value == value } ?: OFFLINE
    }
}

/**
 * 直播流信息
 */
data class LiveStream(
    val deviceSerial: String,
    val channelNo: Int,
    /** v2 接口统一播放地址（ezopen:// 或 http(s) 直链） */
    val url: String?,
    val hlsUrl: String?,
    val hlsHdUrl: String?,
    val rtmpUrl: String?,
    val rtmpHdUrl: String?,
    val flvUrl: String?,
    val flvHdUrl: String?,
    val liveId: String?,
    val expireTime: Long?
) {
    fun getPreferredUrl(): String? =
        url?.takeIf { it.isNotBlank() }
            ?: hlsUrl?.takeIf { it.isNotBlank() }
            ?: flvUrl?.takeIf { it.isNotBlank() }
            ?: rtmpUrl?.takeIf { it.isNotBlank() }

    fun getHdUrl(): String? =
        hlsHdUrl?.takeIf { it.isNotBlank() }
            ?: flvHdUrl?.takeIf { it.isNotBlank() }
            ?: rtmpHdUrl?.takeIf { it.isNotBlank() }
}

/**
 * 回放流信息
 */
data class PlaybackStream(
    val deviceSerial: String,
    val channelNo: Int,
    val url: String,
    val expireTime: Long?
)

/**
 * 报警消息
 */
data class AlarmMessage(
    val alarmId: String,
    val deviceSerial: String,
    val channelNo: Int,
    val alarmName: String,
    val alarmType: Int,
    val alarmTime: String,
    val alarmPicUrl: String?,
    val alarmVideoUrl: String?,
    val isRead: Boolean,
    val isChecked: Boolean,
    val deviceName: String?,
    val preRecordUrl: String?,
    val formattedTime: String = "",
    val timeGroup: String = ""
)

enum class AlarmType(val code: Int, val label: String) {
    MOTION_DETECT(10000, "移动侦测"),
    HUMAN_DETECT(10001, "人形检测"),
    INTRUSION(10002, "越界侦测"),
    LINE_CROSSING(10003, "区域入侵"),
    OTHER(-1, "其他");

    companion object {
        fun fromCode(code: Int): AlarmType =
            entries.find { it.code == code } ?: OTHER
    }
}
