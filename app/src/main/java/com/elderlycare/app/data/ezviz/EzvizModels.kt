package com.elderlycare.app.data.ezviz

import com.google.gson.annotations.SerializedName

/**
 * 萤石开放平台 API 统一返回格式
 * {
 *   "code": "200",
 *   "msg": "操作成功",
 *   "data": { ... }
 * }
 */
data class ApiResponse<T>(
    val code: String,
    val msg: String = "",
    val data: T? = null
)

/**
 * 获取 accessToken 的返回数据
 */
data class TokenDto(
    val accessToken: String = "",
    val expireTime: Long = 0
)

/**
 * 设备基本信息
 */
data class DeviceDto(
    val deviceSerial: String = "",
    val deviceName: String = "",
    val deviceType: String = "",
    val status: Int = 0,               // 1=在线, 0=离线
    val deviceCover: String? = null,
    val category: String? = null,
    val channelNo: Int = 1,
    val isEncrypt: Int = 0,
    val defence: Int = 0
)

/**
 * 设备详情
 */
data class DeviceDetailDto(
    val deviceSerial: String = "",
    val deviceName: String = "",
    val model: String? = null,
    val status: Int = 0,
    val deviceCover: String? = null,
    val channelNo: Int = 1,
    val version: String? = null,
    val capacity: DeviceCapacityDto? = null
)

data class DeviceCapacityDto(
    val supportPtz: Int = 0,
    val supportTalk: Int = 0,
    val supportCloud: Int = 0,
    val supportWifi: Int = 0
)

/**
 * 设备能力集（api/lapp/device/capacity 返回，字段值为字符串 "0"/"1"/"3"）。
 * 官方文档：https://open.ys7.com/help/77
 * 注意：该接口能力字段均为字符串类型，且字段名为下划线风格（如 support_talk），
 * 与 device/info 里的 capacity 对象不同，故单独建模，用 @SerializedName 精确映射。
 *
 * supportTalk 可空：RK3 固件适配缺陷——HTTP200 但返回空对象 `{}` 时该字段缺失（null），
 * 用于区分「明确不支持 support_talk=0」与「接口未返回能力字段（按规则放行）」，
 * 见 EzvizRepository.getDeviceSupportTalkExplicit。
 */
data class DeviceTalkCapacityDto(
    @SerializedName("support_talk") val supportTalk: String? = null
)

/**
 * 直播地址
 */
data class LiveAddressDto(
    val deviceSerial: String = "",
    val channelNo: Int = 1,
    /** v2 接口统一返回的播放地址（protocol=1 时为 ezopen://，其余协议为 http(s) 直链） */
    val url: String? = null,
    val hls: String? = null,
    val hlsHd: String? = null,
    val rtmp: String? = null,
    val rtmpHd: String? = null,
    val flv: String? = null,
    val flvHd: String? = null,
    val liveId: String? = null,
    val expireTime: Long? = null
)

/**
 * 回放地址（统一播放地址接口 v2）
 */
data class PlaybackAddressDto(
    val id: String? = null,
    val url: String = ""
)

/**
 * 报警消息
 */
data class AlarmDto(
    val alarmId: String = "",
    val deviceSerial: String = "",
    val channelNo: Int = 1,
    val alarmName: String = "",
    val alarmType: Int = 0,            // 10000=移动侦测, 10001=人形检测, 10002=越界侦测等
    val alarmTime: String = "",        // yyyy-MM-dd HH:mm:ss
    val alarmPicUrl: String? = null,
    val alarmVideoUrl: String? = null,
    val isRead: Int = 0,
    val isChecked: Int = 0,
    val deviceName: String? = null,
    val preRecordUrl: String? = null
)

/**
 * 录像文件列表（api/lapp/video/by/time 非分页返回，data 为数组）。
 * startTime/endTime 为毫秒时间戳。
 */
data class RecordFileDto(
    val recType: Int = 0,               // 2=本地录像
    val startTime: Long = 0,            // 毫秒
    val endTime: Long = 0,              // 毫秒
    val deviceSerial: String? = null,
    val channelNo: Int? = null,
    val localType: String? = null,      // ALARM / TIMING / IO
    val videoLong: Long = 0             // 录像时长（毫秒）
)

/**
 * 录像文件列表（分页结构返回，version=2.0 且 pageSize 非空时）。
 */
data class RecordFilePageDto(
    val files: List<RecordFileDto>? = null,
    val isAll: Boolean? = null,
    val nextFileTime: Long? = null
)
