package com.ezvizpro.core.network

import com.ezvizpro.core.network.model.*
import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

/**
 * 萤石开放平台 REST API 接口定义
 *
 * 文档: https://open.ys7.com/doc
 *
 * 注意: 萤石 API 使用 Form-UrlEncoded 方式传参，
 * accessToken 作为 form field 而非 HTTP Header
 */
interface EzvizApi {

    // ==================== 认证 ====================

    /**
     * 获取 accessToken（原始 JSON 返回，手动解析，兼容不同 API 版本）
     * 有效期 7 天，建议在过期前 1 天刷新
     */
    @FormUrlEncoded
    @POST("api/lapp/token/get")
    suspend fun getAccessTokenRaw(
        @Field("appKey") appKey: String,
        @Field("appSecret") appSecret: String
    ): Response<JsonObject>

    /**
     * 获取 accessToken（强类型版本）
     */
    @FormUrlEncoded
    @POST("api/lapp/token/get")
    suspend fun getAccessToken(
        @Field("appKey") appKey: String,
        @Field("appSecret") appSecret: String
    ): Response<ApiResponse<TokenDto>>

    // ==================== 设备管理 ====================

    /**
     * 获取设备列表（分页）
     */
    @FormUrlEncoded
    @POST("api/lapp/device/list")
    suspend fun getDeviceList(
        @Field("accessToken") accessToken: String,
        @Field("pageStart") pageStart: Int = 0,
        @Field("pageSize") pageSize: Int = 50
    ): Response<ApiResponse<List<DeviceDto>>>

    /**
     * 获取设备详情（含能力集）
     */
    @FormUrlEncoded
    @POST("api/lapp/device/info")
    suspend fun getDeviceInfo(
        @Field("accessToken") accessToken: String,
        @Field("deviceSerial") deviceSerial: String
    ): Response<ApiResponse<DeviceDetailDto>>

    /**
     * 获取设备在线状态
     */
    @FormUrlEncoded
    @POST("api/lapp/device/status/get")
    suspend fun getDeviceStatus(
        @Field("accessToken") accessToken: String,
        @Field("deviceSerial") deviceSerial: String
    ): Response<ApiResponse<DeviceStatusDto>>

    /**
     * 修改设备名称
     */
    @FormUrlEncoded
    @POST("api/lapp/device/name/update")
    suspend fun updateDeviceName(
        @Field("accessToken") accessToken: String,
        @Field("deviceSerial") deviceSerial: String,
        @Field("deviceName") deviceName: String
    ): Response<ApiResponse<Unit>>

    // ==================== 直播预览 ====================

    /**
     * 获取直播地址
     * @param protocol 协议类型: 1=RTMP, 2=HLS, 3=FLV
     * @param code 设备验证码（设备开启加密时必填，6位大写字母）
     */
    @FormUrlEncoded
    @POST("api/lapp/live/address/get")
    suspend fun getLiveAddress(
        @Field("accessToken") accessToken: String,
        @Field("deviceSerial") deviceSerial: String,
        @Field("channelNo") channelNo: Int = 1,
        @Field("protocol") protocol: Int = 2,  // 默认 HLS
        @Field("code") code: String? = null       // 设备验证码（加密设备必填）
    ): Response<ApiResponse<LiveAddressDto>>

    /**
     * 开始直播（激活直播通道，现在通常不需要显式调用）
     */
    @FormUrlEncoded
    @POST("api/lapp/live/video/open")
    suspend fun openLive(
        @Field("accessToken") accessToken: String,
        @Field("deviceSerial") deviceSerial: String,
        @Field("channelNo") channelNo: Int = 1
    ): Response<ApiResponse<LiveOpenDto>>

    /**
     * 停止直播
     */
    @FormUrlEncoded
    @POST("api/lapp/live/video/close")
    suspend fun closeLive(
        @Field("accessToken") accessToken: String,
        @Field("deviceSerial") deviceSerial: String,
        @Field("channelNo") channelNo: Int = 1
    ): Response<ApiResponse<Unit>>

    // ==================== 云台控制 (PTZ) ====================

    /**
     * 开始云台控制
     * @param direction 方向: 0=上, 1=下, 2=左, 3=右, 4=左上, 5=左下, 6=右上, 7=右下
     * @param speed 速度: 0=慢, 1=适中, 2=快
     */
    @FormUrlEncoded
    @POST("api/lapp/device/ptz/start")
    suspend fun startPtz(
        @Field("accessToken") accessToken: String,
        @Field("deviceSerial") deviceSerial: String,
        @Field("channelNo") channelNo: Int = 1,
        @Field("direction") direction: Int,
        @Field("speed") speed: Int = 1
    ): Response<ApiResponse<Unit>>

    /**
     * 停止云台控制
     */
    @FormUrlEncoded
    @POST("api/lapp/device/ptz/stop")
    suspend fun stopPtz(
        @Field("accessToken") accessToken: String,
        @Field("deviceSerial") deviceSerial: String,
        @Field("channelNo") channelNo: Int = 1,
        @Field("direction") direction: Int? = null  // null 表示停止所有方向
    ): Response<ApiResponse<Unit>>

    /**
     * 获取预置位列表
     */
    @FormUrlEncoded
    @POST("api/lapp/device/preset/list")
    suspend fun getPresetList(
        @Field("accessToken") accessToken: String,
        @Field("deviceSerial") deviceSerial: String,
        @Field("channelNo") channelNo: Int = 1
    ): Response<ApiResponse<List<PresetDto>>>

    /**
     * 调用预置位
     */
    @FormUrlEncoded
    @POST("api/lapp/device/preset/move")
    suspend fun moveToPreset(
        @Field("accessToken") accessToken: String,
        @Field("deviceSerial") deviceSerial: String,
        @Field("channelNo") channelNo: Int = 1,
        @Field("index") presetIndex: Int
    ): Response<ApiResponse<Unit>>

    /**
     * 添加预置位
     */
    @FormUrlEncoded
    @POST("api/lapp/device/preset/add")
    suspend fun addPreset(
        @Field("accessToken") accessToken: String,
        @Field("deviceSerial") deviceSerial: String,
        @Field("channelNo") channelNo: Int = 1,
        @Field("presetName") presetName: String
    ): Response<ApiResponse<PresetDto>>

    // ==================== 录像回放 ====================

    /**
     * 获取回放地址（统一播放地址接口 v2）
     *
     * type: 1=直播预览, 2=本地录像回放(SD卡), 3=云存储回放
     * protocol: 1=ezopen(推荐), 3=rtmp, 4=flv（hls不支持回放）
     */
    @FormUrlEncoded
    @POST("api/lapp/v2/live/address/get")
    suspend fun getPlaybackAddress(
        @Field("accessToken") accessToken: String,
        @Field("deviceSerial") deviceSerial: String,
        @Field("channelNo") channelNo: Int = 1,
        @Field("type") type: Int = 2,           // 默认本地录像回放
        @Field("protocol") protocol: Int = 1,    // 默认 ezopen 协议
        @Field("startTime") startTime: String,   // yyyy-MM-dd HH:mm:ss
        @Field("stopTime") stopTime: String,      // yyyy-MM-dd HH:mm:ss
        @Field("code") code: String? = null       // 设备验证码（设备开启加密时必填，6位大写字母）
    ): Response<ApiResponse<PlaybackAddressDto>>

    // ==================== 报警消息 (Phase 2) ====================

    /**
     * 获取报警消息列表
     * @param alarmType 告警类型，不填为全部
     */
    @FormUrlEncoded
    @POST("api/lapp/alarm/list")
    suspend fun getAlarmList(
        @Field("accessToken") accessToken: String,
        @Field("pageStart") pageStart: Int = 0,
        @Field("pageSize") pageSize: Int = 20,
        @Field("alarmType") alarmType: Int? = null
    ): Response<ApiResponse<List<AlarmDto>>>

    /**
     * 标记报警消息为已读
     */
    @FormUrlEncoded
    @POST("api/lapp/alarm/read")
    suspend fun markAlarmRead(
        @Field("accessToken") accessToken: String,
        @Field("alarmId") alarmId: String
    ): Response<ApiResponse<Unit>>
}
