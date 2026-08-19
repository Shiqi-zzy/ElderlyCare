package com.elderlycare.app.network.ezviz

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

/**
 * 萤石云广播 REST 接口定义。
 *
 * 官方文档：https://icnopen.ezviz.com/help/1430（API-云通话-云广播 类目）
 * 服务说明：云广播为付费服务（按条计费），需在开放平台申请开通；
 * 设备需支持能力集 support_talk（详见 EZCloudBroadcastManager 能力判断）。
 *
 * 语音下发流程：先调用 [uploadVoiceFile] 上传音频得到 fileUrl（有效期 1 天），
 * 再调用 [sendVoiceToDevice] 把 fileUrl 下发到设备扬声器播放。
 */
interface EzvizVoiceApi {

    /**
     * 语音文件上传。
     * @param accessToken 访问令牌（复用项目 token 逻辑）
     * @param voiceFile   音频文件，支持 wav/mp3/aac，最大 5M、最长 60s
     * @param voiceName   语音名称（≤50 字符，可选）
     * @param force       存在同名文件时是否强制替换（可选，默认 false）
     * @return 成功返回 name 与 url（url 有效期仅 1 天）
     */
    @Multipart
    @POST("api/lapp/voice/upload")
    suspend fun uploadVoiceFile(
        @Part("accessToken") accessToken: RequestBody,
        @Part voiceFile: MultipartBody.Part,
        @Part("voiceName") voiceName: RequestBody? = null,
        @Part("force") force: RequestBody? = null
    ): Response<VoiceUploadResponse>

    /**
     * 语音文件下发（让设备扬声器播放）。
     * @param fileUrl 上传接口返回的 url
     * @param channelNo 通道号，默认 1（可选）
     * @return 云广播 API 通用响应（code=200 为成功）
     */
    @POST("api/lapp/voice/send")
    suspend fun sendVoiceToDevice(
        @Query("accessToken") accessToken: String,
        @Query("deviceSerial") deviceSerial: String,
        @Query("fileUrl") fileUrl: String,
        @Query("channelNo") channelNo: Int = 1
    ): Response<VoiceSendResponse>

    /**
     * 查询设备语音列表 / 云广播下发状态。
     * 新版接口（域名 icnopen.ezviz.com），返回 voiceInfos 中 status 字段：
     * 0=同步完成，1=同步中，2=失败。
     * TODO(用户需确认): 该新域名接口在部分应用上需单独开通，老接口为
     *  POST open.ys7.com/api/lapp/voice/list；按你的控制台文档为准。
     */
    @GET("api/route/voice/v3/devices/voices")
    suspend fun getDeviceVoices(
        @Query("accessToken") accessToken: String,
        @Query("deviceSerial") deviceSerial: String
    ): Response<DeviceVoicesResponse>

    /**
     * 删除已上传的语音文件。
     * TODO(用户需确认): 接口路径以官方文档为准，此处为推测占位（api/lapp/voice/delete）。
     */
    @POST("api/lapp/voice/delete")
    suspend fun deleteVoiceFile(
        @Query("accessToken") accessToken: String,
        @Query("voiceId") voiceId: String
    ): Response<VoiceSendResponse>
}

// ==================== 云广播返回模型 ====================

/** 语音上传返回 */
data class VoiceUploadResponse(
    val code: String = "",
    val msg: String = "",
    val data: VoiceUploadData? = null
)

data class VoiceUploadData(
    /** 语音名称 */
    val name: String? = null,
    /** 下载地址（有效期 1 天），下发时使用 */
    val url: String? = null
)

/** 语音下发/删除通用返回 */
data class VoiceSendResponse(
    val code: String = "",
    val msg: String = ""
)

/** 设备语音列表（新版接口返回，meta 包一层） */
data class DeviceVoicesResponse(
    val meta: VoiceMeta? = null,
    val voiceInfos: List<VoiceInfoDto>? = null,
    // 老接口返回格式兼容
    val code: String? = null,
    val msg: String? = null,
    val data: List<VoiceInfoDto>? = null
)

data class VoiceMeta(
    val code: Int = -1,
    val message: String = ""
)

data class VoiceInfoDto(
    /** 语音文件 id */
    val voiceId: String = "",
    /** 语音名称 */
    val voiceName: String = "",
    /** 语音文件 url */
    val voiceUrl: String = "",
    /** 平台和设备同步状态：0=同步完成，1=同步中，2=失败 */
    val status: Int = 0,
    /** 创建时间 */
    val time: Long = 0
)
