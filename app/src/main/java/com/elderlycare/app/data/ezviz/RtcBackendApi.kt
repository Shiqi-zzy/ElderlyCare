package com.elderlycare.app.data.ezviz

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.POST
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
}
