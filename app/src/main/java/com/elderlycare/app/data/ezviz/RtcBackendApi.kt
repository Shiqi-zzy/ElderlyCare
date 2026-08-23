package com.elderlycare.app.data.ezviz

import com.google.gson.JsonElement
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

/** 后端 /api/rtc/token 请求体 */
data class TokenRequestBody(
    val room_id: String = "",
    val custom_id: String = "family001",
    val device_serial: String = "",
)

/** 后端统一返回 {code, message, data} */
data class RtcBackendResponse(
    val code: Int = -1,
    val message: String = "",
    val data: TokenData? = null,
)

/** token 返回数据（对应后端 /api/rtc/token 的 data） */
data class TokenData(
    val app_id: String = "",
    val room_id: String = "",
    val device_serial: String = "",
    val user_id: String = "",
    val client_token: String = "",
    val device_token: String = "",
)

/** 文字留言请求体（后端 /api/leave-message/text） */
data class LeaveMessageTextRequest(
    val device_serial: String = "",
    val text: String = "",
)

/** 文字留言响应：后端透传萤石云广播接口原始 code/msg（App UI 直接展示） */
data class LeaveMessageTextResponse(
    val code: Int = -1,
    val message: String = "",
    val ezviz_code: String = "",
    val ezviz_msg: String = "",
)

/** 手机试听请求体（后端 /api/leave-message/tts-preview，提醒计划表单试听用） */
data class TtsPreviewRequest(
    val text: String = "",
    val voice: String = "zh-CN-XiaoxiaoNeural",
)

/**
 * ElderlyCare 云通话后端接口（baseUrl = BuildConfig.RTC_BACKEND_URL）。
 * 负责向后端取 RTC token 等信令数据，以及文字留言（后端做云端 TTS + 萤石云广播下发）。
 */
interface RtcBackendApi {

    @POST("api/rtc/token")
    suspend fun getToken(@Body body: TokenRequestBody): RtcBackendResponse

    @POST("api/leave-message/text")
    suspend fun sendTextMessage(@Body body: LeaveMessageTextRequest): LeaveMessageTextResponse

    /** 手机试听：文本 + 音色 → 后端 edge-tts 合成 mp3 字节流（仅试听，不下发设备） */
    @Streaming
    @POST("api/leave-message/tts-preview")
    suspend fun ttsPreview(@Body body: TtsPreviewRequest): ResponseBody

    // ===== 抓拍模块（后端 capture_routes：手动抓拍/列表/已读/未读数/验证码上报）=====

    /** 手动云端抓拍（后端→萤石 device/capture→落盘 alarm_events(manual)→回传 localPicUrl） */
    @POST("api/ezviz/capture")
    suspend fun capture(@Body body: CaptureRequestBody): BackendBaseResponse

    /** 全部抓拍列表（manual+auto，新→旧） */
    @GET("api/captures")
    suspend fun getCaptures(
        @Query("deviceSerial") deviceSerial: String,
        @Query("pageStart") pageStart: Int = 0,
        @Query("pageSize") pageSize: Int = 100,
    ): BackendBaseResponse

    /** 点击条目标记该条已读（后端限定设备防串读） */
    @POST("api/captures/{recordId}/read")
    suspend fun markCaptureRead(
        @Path("recordId") recordId: String,
        @Body body: CaptureMarkReadRequest,
    ): BackendBaseResponse

    /** 全部抓拍页未读数（首页告警消息图标角标数据源） */
    @GET("api/captures/unread-count")
    suspend fun getCapturesUnreadCount(
        @Query("deviceSerial") deviceSerial: String,
    ): BackendBaseResponse

    /** 设备验证码上报（绑定成功后调用；后端存 device_auth 供告警图片解密） */
    @POST("api/device/auth")
    suspend fun uploadDeviceAuth(@Body body: DeviceAuthRequestBody): BackendBaseResponse
}

/** 后端统一返回信封（抓拍/验证码接口通用；data 结构随接口而异，用 JsonElement 承接） */
data class BackendBaseResponse(
    val code: Int = -1,
    val message: String = "",
    val data: JsonElement? = null,
)

/** 手动抓拍请求体（后端 POST /api/ezviz/capture） */
data class CaptureRequestBody(
    val deviceSerial: String = "",
    val channelNo: Int = 1,
)

/** 标记已读请求体（后端 POST /api/captures/{recordId}/read） */
data class CaptureMarkReadRequest(
    val deviceSerial: String = "",
)

/** 设备验证码上报请求体（后端 POST /api/device/auth） */
data class DeviceAuthRequestBody(
    val deviceSerial: String = "",
    val validateCode: String = "",
)

/** 抓拍记录（后端 GET /api/captures 的 list 条目） */
data class CaptureItem(
    val recordId: String = "",
    val deviceSerial: String = "",
    val captureType: String = "auto",
    val alarmName: String = "",
    val eventTime: Long = 0L,
    val picUrl: String = "",
    val localPicUrl: String = "",
    val isRead: Boolean = false,
)

/** 抓拍列表数据（后端 GET /api/captures 的 data） */
data class CaptureListData(
    val list: List<CaptureItem> = emptyList(),
    val total: Int = 0,
)
