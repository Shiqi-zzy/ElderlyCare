package com.elderlycare.app.data.ezviz

import android.content.Context
import android.util.Log
import com.elderlycare.app.BuildConfig
import com.elderlycare.app.ui.ezviz.LocalMediaCapture
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/**
 * 抓拍仓库：手动云端抓拍 / 全部抓拍列表 / 已读 / 未读数 / 设备验证码上报。
 *
 * 链路（手动抓拍）：App → 后端 /api/ezviz/capture → 萤石 device/capture →
 * 后端下载落盘 alarm_events(manual) → 回传 localPicUrl → App 下载 → 系统相册。
 * 图片数据只存后端（alarm_events + media/），App 本地 Room 不落图片，
 * 与留言/告警文字消息完全隔离。
 *
 * 所有方法返回 Result：失败时 exception.message 为可直接 toast 的用户文案。
 */
class CaptureRepository(
    private val context: Context,
    private val api: RtcBackendApi,
    private val okHttpClient: OkHttpClient,
) {

    private val appContext = context.applicationContext
    private val gson = Gson()

    /** 手动云端抓拍。成功返回「截图已保存」文案；失败 message 为 toast 文案。 */
    suspend fun capture(deviceSerial: String): Result<String> = runCatching {
        val resp = api.capture(CaptureRequestBody(deviceSerial = deviceSerial, channelNo = 1))
        if (resp.code != 200) {
            throw CaptureException(mapCaptureError(resp.code, resp.message))
        }
        val data = resp.data?.asJsonObject
        val localPicUrl = data?.get("localPicUrl")
            ?.takeUnless { it.isJsonNull }?.asString?.trim().orEmpty()
        if (localPicUrl.isBlank()) throw CaptureException("图片下载失败")
        downloadToGallery(localPicUrl)
        Log.d(TAG, "手动抓拍成功 device=$deviceSerial url=$localPicUrl")
        "截图已保存"
    }

    /** 全部抓拍列表（新→旧，进页拉取兜底）。 */
    suspend fun fetchCaptures(deviceSerial: String): Result<CaptureListData> = runCatching {
        val resp = api.getCaptures(deviceSerial)
        if (resp.code != 200) {
            throw CaptureException(resp.message.ifBlank { "获取抓拍列表失败，请检查网络" })
        }
        val data = resp.data ?: throw CaptureException("获取抓拍列表失败，请检查网络")
        gson.fromJson(data, CaptureListData::class.java) ?: CaptureListData()
    }

    /** 点击条目标记已读（后端限定设备防串读）。 */
    suspend fun markRead(recordId: String, deviceSerial: String): Result<Unit> = runCatching {
        val resp = api.markCaptureRead(recordId, CaptureMarkReadRequest(deviceSerial = deviceSerial))
        if (resp.code != 200) {
            throw CaptureException(resp.message.ifBlank { "标记已读失败" })
        }
    }

    /** 全部抓拍页未读数（首页「告警消息」图标角标数据源）。 */
    suspend fun fetchUnreadCount(deviceSerial: String): Result<Int> = runCatching {
        val resp = api.getCapturesUnreadCount(deviceSerial)
        if (resp.code != 200) {
            throw CaptureException(resp.message.ifBlank { "获取未读数失败" })
        }
        resp.data?.asJsonObject?.get("unreadCount")
            ?.takeUnless { it.isJsonNull }?.asInt ?: 0
    }

    /** 设备验证码上报（绑定成功后调用；幂等 upsert，重绑/补同步可重复调用）。 */
    suspend fun uploadDeviceAuth(deviceSerial: String, validateCode: String): Result<Unit> = runCatching {
        val resp = api.uploadDeviceAuth(
            DeviceAuthRequestBody(deviceSerial = deviceSerial, validateCode = validateCode)
        )
        if (resp.code != 200) {
            throw CaptureException(resp.message.ifBlank { "设备信息同步失败" })
        }
    }

    /** 后端相对图片路径 → 完整 URL（后端零硬编码 IP，App 用 RTC_BACKEND_URL 拼接）。 */
    fun resolveImageUrl(localPicUrl: String): String {
        if (localPicUrl.isBlank()) return ""
        return BuildConfig.RTC_BACKEND_URL.trimEnd('/') + "/" + localPicUrl.trimStart('/')
    }

    /** 下载后端图片 → 系统相册（复用 LocalMediaCapture，与预览页截图同一入口）。 */
    private suspend fun downloadToGallery(localPicUrl: String) = withContext(Dispatchers.IO) {
        val url = resolveImageUrl(localPicUrl)
        val tmp = File(appContext.cacheDir, "capture_dl_${System.currentTimeMillis()}.jpg")
        try {
            val request = Request.Builder().url(url).build()
            okHttpClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) throw CaptureException("图片下载失败")
                val body = resp.body ?: throw CaptureException("图片下载失败")
                tmp.writeBytes(body.bytes())
            }
            LocalMediaCapture.saveImageToGallery(appContext, tmp, "capture")
        } finally {
            tmp.delete()
        }
    }

    /** 后端错误码 → 用户文案（与 capture_routes 错误码约定一一对应）。 */
    private fun mapCaptureError(code: Int, message: String): String = when (code) {
        -2 -> "操作太频繁，请稍后再试"
        -3 -> "抓拍频率受限，请稍后再试"
        -4 -> "设备未响应（可能离线），请重试"
        -5 -> "图片下载失败"
        else -> message.ifBlank { "抓拍失败，请重试" }
    }

    class CaptureException(message: String) : Exception(message)

    private companion object {
        const val TAG = "CaptureRepository"
    }
}
