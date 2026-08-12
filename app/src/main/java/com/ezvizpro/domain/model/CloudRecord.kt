package com.ezvizpro.domain.model

/**
 * 云存储录像记录
 */
data class CloudRecord(
    val recordId: String,
    val deviceSerial: String,
    val channelNo: Int,
    val startTime: String,        // yyyy-MM-dd HH:mm:ss
    val endTime: String,
    val recordType: RecordType,
    val recordSize: Long?,
    val coverPic: String?,
    // 用于 UI 展示
    val durationSeconds: Int = 0  // 录像时长 (秒)，从 startTime/endTime 计算
)

enum class RecordType(val value: Int, val label: String) {
    MOTION_DETECT(1, "移动侦测"),
    SCHEDULED(2, "定时录像"),
    MANUAL(3, "手动录像");

    companion object {
        fun fromValue(value: Int): RecordType =
            entries.find { it.value == value } ?: MOTION_DETECT
    }
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
