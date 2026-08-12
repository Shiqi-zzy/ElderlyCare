package com.ezvizpro.domain.model

/**
 * 报警消息
 */
data class AlarmMessage(
    val alarmId: String,
    val deviceSerial: String,
    val channelNo: Int,
    val alarmName: String,
    val alarmType: Int,
    val alarmTime: String,            // yyyy-MM-dd HH:mm:ss
    val alarmPicUrl: String?,
    val alarmVideoUrl: String?,
    val isRead: Boolean,
    val isChecked: Boolean,
    val deviceName: String?,
    val preRecordUrl: String?,
    // 格式化后的时间 (用于 UI 分组)
    val formattedTime: String = "",   // "今天 14:30"、"昨天 08:15"、"2024-01-15"
    val timeGroup: String = ""        // "今天"、"昨天"、"更早"
)

/**
 * 告警类型
 */
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
