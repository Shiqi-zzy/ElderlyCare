package com.elderlycare.app.network.ezviz

import android.util.Log
import com.elderlycare.app.data.ezviz.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

/**
 * 萤石云广播 REST 封装 —— 留言模块「App → 设备」文件语音通路（通路②）。
 *
 * 官方文档：https://icnopen.ezviz.com/help/1430（API-云通话-云广播 类目）
 *
 * 下发流程：
 * 1. [uploadVoiceFile] 上传音频文件，得到 fileUrl（有效期 1 天）；
 * 2. [sendVoiceToDevice] 把 fileUrl 下发到设备扬声器播放；
 * 3. [queryDeviceVoices] 查询设备语音列表/同步状态（0=同步完成 1=同步中 2=失败）。
 *
 * 约束：
 * - 云广播为付费服务（按条计费），需在开放平台申请开通；
 * - 音频格式仅支持 wav/mp3/aac，≤5M、≤60s；
 * - 设备需支持能力集 support_talk = 1 或 3（见 [checkDeviceTalkSupport]）。
 *
 * @param tokenProvider          复用项目 accessToken（EzvizRepository.obtainValidToken）
 * @param talkCapabilityProvider 设备能力查询（返回 support_talk 原始值）
 */
class EZCloudBroadcastManager(
    private val api: EzvizVoiceApi,
    private val tokenProvider: suspend () -> String?,
    private val talkCapabilityProvider: suspend (String) -> NetworkResult<Int>
) {

    companion object {
        private const val TAG = "EZCloudBroadcast"

        /** 云广播音频上限：5M */
        private const val MAX_FILE_BYTES = 5L * 1024 * 1024

        /** voiceName 长度上限 */
        private const val MAX_VOICE_NAME_LEN = 50
    }

    /**
     * 上传语音文件，返回 fileUrl（有效期 1 天）。
     * 上传前校验文件存在性与 5M 上限；≤60s 由录音/TTS 侧保证。
     */
    suspend fun uploadVoiceFile(file: File, voiceName: String): NetworkResult<String> =
        withContext(Dispatchers.IO) {
            val token = tokenProvider()
                ?: return@withContext NetworkResult.Error(message = "未登录或 Token 已过期，请重试")
            if (!file.exists() || file.length() == 0L) {
                return@withContext NetworkResult.Error(message = "音频文件不存在")
            }
            if (file.length() > MAX_FILE_BYTES) {
                return@withContext NetworkResult.Error(message = "音频超过 5M 上限")
            }
            try {
                val mime = when (file.extension.lowercase()) {
                    "wav" -> "audio/wav"
                    "mp3" -> "audio/mpeg"
                    "aac" -> "audio/aac"
                    else -> "application/octet-stream"
                }
                val response = api.uploadVoiceFile(
                    accessToken = token.toRequestBody("text/plain".toMediaType()),
                    voiceFile = MultipartBody.Part.createFormData(
                        "voiceFile",
                        file.name,
                        file.asRequestBody(mime.toMediaType())
                    ),
                    voiceName = voiceName.take(MAX_VOICE_NAME_LEN)
                        .toRequestBody("text/plain".toMediaType())
                )
                val body = response.body()
                val url = body?.data?.url
                if (response.isSuccessful && body != null && body.code == "200" && !url.isNullOrBlank()) {
                    NetworkResult.Success(url)
                } else {
                    NetworkResult.Error(
                        code = body?.code ?: response.code().toString(),
                        message = body?.msg?.takeIf { it.isNotBlank() } ?: "语音上传失败"
                    )
                }
            } catch (e: java.net.UnknownHostException) {
                NetworkResult.Error(message = "网络连接失败，请检查网络", throwable = e)
            } catch (e: java.net.SocketTimeoutException) {
                NetworkResult.Error(message = "请求超时，请稍后重试", throwable = e)
            } catch (e: Exception) {
                Log.e(TAG, "语音上传异常", e)
                NetworkResult.Error(message = e.message ?: "语音上传失败", throwable = e)
            }
        }

    /**
     * 下发语音到设备扬声器播放。
     */
    suspend fun sendVoiceToDevice(deviceSerial: String, fileUrl: String): NetworkResult<Unit> =
        withContext(Dispatchers.IO) {
            val token = tokenProvider()
                ?: return@withContext NetworkResult.Error(message = "未登录或 Token 已过期，请重试")
            try {
                val response = api.sendVoiceToDevice(
                    accessToken = token,
                    deviceSerial = deviceSerial,
                    fileUrl = fileUrl
                )
                val body = response.body()
                if (response.isSuccessful && body != null && body.code == "200") {
                    NetworkResult.Success(Unit)
                } else {
                    NetworkResult.Error(
                        code = body?.code ?: response.code().toString(),
                        message = body?.msg?.takeIf { it.isNotBlank() } ?: "语音下发失败"
                    )
                }
            } catch (e: java.net.UnknownHostException) {
                NetworkResult.Error(message = "网络连接失败，请检查网络", throwable = e)
            } catch (e: java.net.SocketTimeoutException) {
                NetworkResult.Error(message = "请求超时，请稍后重试", throwable = e)
            } catch (e: Exception) {
                Log.e(TAG, "语音下发异常", e)
                NetworkResult.Error(message = e.message ?: "语音下发失败", throwable = e)
            }
        }

    /**
     * 查询设备语音列表 / 云广播下发状态。
     * 兼容新版返回（meta+voiceInfos）与老版返回（code+data）两种格式。
     */
    suspend fun queryDeviceVoices(deviceSerial: String): NetworkResult<List<VoiceInfoDto>> =
        withContext(Dispatchers.IO) {
            val token = tokenProvider()
                ?: return@withContext NetworkResult.Error(message = "未登录或 Token 已过期，请重试")
            try {
                val response = api.getDeviceVoices(accessToken = token, deviceSerial = deviceSerial)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    // 新版接口：{meta:{code,message}, voiceInfos:[...]}
                    body.meta?.let { meta ->
                        return@withContext if (meta.code == 200) {
                            NetworkResult.Success(body.voiceInfos ?: emptyList())
                        } else {
                            NetworkResult.Error(
                                code = meta.code.toString(),
                                message = meta.message.ifBlank { "查询语音列表失败" }
                            )
                        }
                    }
                    // 老版接口：{code,msg,data:[...]}
                    if (body.code != null) {
                        return@withContext if (body.code == "200") {
                            NetworkResult.Success(body.data ?: emptyList())
                        } else {
                            NetworkResult.Error(
                                code = body.code,
                                message = body.msg?.takeIf { it.isNotBlank() } ?: "查询语音列表失败"
                            )
                        }
                    }
                    NetworkResult.Error(message = "语音列表响应格式无法解析")
                } else {
                    NetworkResult.Error(
                        code = response.code().toString(),
                        message = "HTTP ${response.code()}"
                    )
                }
            } catch (e: java.net.UnknownHostException) {
                NetworkResult.Error(message = "网络连接失败，请检查网络", throwable = e)
            } catch (e: java.net.SocketTimeoutException) {
                NetworkResult.Error(message = "请求超时，请稍后重试", throwable = e)
            } catch (e: Exception) {
                Log.e(TAG, "查询语音列表异常", e)
                NetworkResult.Error(message = e.message ?: "查询语音列表失败", throwable = e)
            }
        }

    /**
     * 判断设备是否支持云广播/语音对讲。
     * 萤石文档：设备能力集 support_talk = 1（全双工）或 3（半双工）才支持。
     *
     * ⚠️ 不可靠，业务已弃用：实测 RK3 的 device/info 返回 capacity 为空对象
     * （API 未回能力字段），本判断恒为 false，会误伤实际可用的云广播下发。
     * 保留仅为记录语义；能力真正不支持时下发接口会直接报错。
     */
    suspend fun checkDeviceTalkSupport(deviceSerial: String): NetworkResult<Boolean> =
        when (val result = talkCapabilityProvider(deviceSerial)) {
            is NetworkResult.Success -> NetworkResult.Success(result.data == 1 || result.data == 3)
            is NetworkResult.Error -> result
        }
}
