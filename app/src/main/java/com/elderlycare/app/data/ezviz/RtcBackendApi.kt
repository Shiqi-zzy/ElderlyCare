package com.elderlycare.app.data.ezviz

import retrofit2.http.Body
import retrofit2.http.POST

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

/**
 * ElderlyCare 云通话后端接口（baseUrl = BuildConfig.RTC_BACKEND_URL）。
 * 负责向后端取 RTC token 等信令数据。
 */
interface RtcBackendApi {

    @POST("api/rtc/token")
    suspend fun getToken(@Body body: TokenRequestBody): RtcBackendResponse
}
