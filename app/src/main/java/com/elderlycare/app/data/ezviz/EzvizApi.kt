package com.elderlycare.app.data.ezviz

import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

/**
 * 萤石开放平台 REST API 接口定义
 * 文档: https://open.ys7.com/doc
 *
 * 注意: 萤石 API 使用 Form-UrlEncoded 方式传参，
 * accessToken 作为 form field 而非 HTTP Header。
 */
interface EzvizApi {

    // ==================== 认证 ====================

    @FormUrlEncoded
    @POST("api/lapp/token/get")
    suspend fun getAccessToken(
        @Field("appKey") appKey: String,
        @Field("appSecret") appSecret: String
    ): Response<ApiResponse<TokenDto>>

    // ==================== 设备管理 ====================

    /**
     * 添加设备（绑定 RK3 到账号）
     * @param deviceSerial 设备序列号
     * @param validateCode 设备验证码（6 位大写字母，设备标签上）
     */
    @FormUrlEncoded
    @POST("api/lapp/device/add")
    suspend fun addDevice(
        @Field("accessToken") accessToken: String,
        @Field("deviceSerial") deviceSerial: String,
        @Field("validateCode") validateCode: String
    ): Response<ApiResponse<Any>>

    @FormUrlEncoded
    @POST("api/lapp/device/list")
    suspend fun getDeviceList(
        @Field("accessToken") accessToken: String,
        @Field("pageStart") pageStart: Int = 0,
        @Field("pageSize") pageSize: Int = 50
    ): Response<ApiResponse<List<DeviceDto>>>

    @FormUrlEncoded
    @POST("api/lapp/device/info")
    suspend fun getDeviceInfo(
        @Field("accessToken") accessToken: String,
        @Field("deviceSerial") deviceSerial: String
    ): Response<ApiResponse<DeviceDetailDto>>

    /**
     * 获取设备能力集（support_talk 等）。
     * 官方文档：https://open.ys7.com/help/77（返回 data.support_talk 为字符串 "0"/"1"/"3"）
     */
    @FormUrlEncoded
    @POST("api/lapp/device/capacity")
    suspend fun getDeviceCapacity(
        @Field("accessToken") accessToken: String,
        @Field("deviceSerial") deviceSerial: String,
        @Field("channelNo") channelNo: Int = 1
    ): Response<ApiResponse<DeviceTalkCapacityDto>>

    // ==================== 直播预览 ====================

    /**
     * 获取直播预览地址（统一播放地址接口 v2）。
     * 老接口 api/lapp/live/address/get 已被服务端强制要求未公开的 source 参数（报 10001
     * 「source为空」），故改用官方现行 v2 接口。
     * protocol 默认 1（ezopen）：返回 ezopen:// 地址，加密设备凭 code 也能取到
     * （实测 hls/rtmp/flv 对加密设备返回 60019「加密已开启」），需配合萤石 JSSDK WebView 播放。
     */
    @FormUrlEncoded
    @POST("api/lapp/v2/live/address/get")
    suspend fun getLiveAddress(
        @Field("accessToken") accessToken: String,
        @Field("deviceSerial") deviceSerial: String,
        @Field("channelNo") channelNo: Int = 1,
        @Field("type") type: Int = 1, // 1=预览 2=本地录像回放 3=云存储回放
        @Field("protocol") protocol: Int = 1, // 1=ezopen 2=hls 3=rtmp 4=flv
        @Field("code") code: String? = null
    ): Response<ApiResponse<LiveAddressDto>>

    @FormUrlEncoded
    @POST("api/lapp/live/video/close")
    suspend fun closeLive(
        @Field("accessToken") accessToken: String,
        @Field("deviceSerial") deviceSerial: String,
        @Field("channelNo") channelNo: Int = 1
    ): Response<ApiResponse<Any>>

    // ==================== 录像回放 ====================

    /**
     * 获取回放地址（统一播放地址接口 v2）
     * type: 2=本地录像回放(SD卡), protocol: 1=ezopen
     */
    @FormUrlEncoded
    @POST("api/lapp/v2/live/address/get")
    suspend fun getPlaybackAddress(
        @Field("accessToken") accessToken: String,
        @Field("deviceSerial") deviceSerial: String,
        @Field("channelNo") channelNo: Int = 1,
        @Field("type") type: Int = 2,
        @Field("protocol") protocol: Int = 1,
        @Field("startTime") startTime: String,
        @Field("stopTime") stopTime: String,
        @Field("code") code: String? = null
    ): Response<ApiResponse<PlaybackAddressDto>>

    // ==================== 报警消息 ====================

    @FormUrlEncoded
    @POST("api/lapp/alarm/list")
    suspend fun getAlarmList(
        @Field("accessToken") accessToken: String,
        @Field("pageStart") pageStart: Int = 0,
        @Field("pageSize") pageSize: Int = 20,
        @Field("alarmType") alarmType: Int? = null
    ): Response<ApiResponse<List<AlarmDto>>>

    @FormUrlEncoded
    @POST("api/lapp/alarm/read")
    suspend fun markAlarmRead(
        @Field("accessToken") accessToken: String,
        @Field("alarmId") alarmId: String
    ): Response<ApiResponse<Any>>
}
