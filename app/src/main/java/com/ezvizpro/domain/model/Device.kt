package com.ezvizpro.domain.model

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
    // 能力集
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
