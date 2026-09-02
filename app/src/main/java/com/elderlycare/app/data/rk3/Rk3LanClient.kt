package com.elderlycare.app.data.rk3

import android.util.Log
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * RK3 局域网 HTTP 客户端（OkHttp 直连 + Gson，零新依赖）。
 *
 * RK3 设备开机自动启动 HTTP Server（端口 8080），**无公网 IP**，
 * 仅手机与设备同一局域网 WiFi 时可访问；baseUrl 每次请求前从设置现读（动态地址，
 * 故不用 baseUrl 固定的 Retrofit，项目已有 OkHttp 直连先例）。
 *
 * 异常统一 [Rk3LanException]：message 为可直接展示的用户文案，失败必有 Log.e 完整日志。
 */
class Rk3LanException(message: String) : Exception(message)

class Rk3LanClient(private val okHttpClient: OkHttpClient) {

    /**
     * GET {baseUrl}/{path}?query → 响应 data（JsonObject）。
     * data 为 JsonNull/缺失 → 返回 null（业务层转「暂无数据」空态）。
     */
    suspend fun get(baseUrl: String, path: String, query: Map<String, String> = emptyMap()): JsonObject? {
        val normalized = baseUrl.trim().trimEnd('/')
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            Log.e(TAG, "RK3 服务器地址非法: $baseUrl")
            throw Rk3LanException(MSG_SERVER_NOT_SET)
        }
        val url = "$normalized/${path.trimStart('/')}".toHttpUrlOrNull()
            ?.newBuilder()
            ?.apply { query.forEach { (k, v) -> addQueryParameter(k, v) } }
            ?.build()
        if (url == null) {
            Log.e(TAG, "RK3 服务器地址非法: $baseUrl")
            throw Rk3LanException(MSG_SERVER_NOT_SET)
        }

        return withContext(Dispatchers.IO) {
            val request = Request.Builder().url(url).get().build()
            try {
                okHttpClient.newCall(request).execute().use { resp ->
                    val bodyText = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        Log.e(TAG, "HTTP ${resp.code} $url")
                        throw Rk3LanException(MSG_CONNECT_FAILED)
                    }
                    val json = try {
                        JsonParser.parseString(bodyText).asJsonObject
                    } catch (e: Exception) {
                        Log.e(TAG, "JSON 解析失败 $url body=$bodyText", e)
                        throw Rk3LanException(MSG_CONNECT_FAILED)
                    }
                    // 业务 code：字符串/数字都容错（asInt 失败按 -1 处理）
                    val code = runCatching {
                        json.get("code")?.takeUnless { it.isJsonNull }?.asInt
                    }.getOrNull() ?: -1
                    if (code != 200) {
                        val msg = runCatching {
                            json.get("message")?.takeUnless { it.isJsonNull }?.asString.orEmpty()
                        }.getOrDefault("")
                        Log.e(TAG, "RK3 业务错误 code=$code message=$msg url=$url")
                        throw Rk3LanException(msg.ifBlank { MSG_CONNECT_FAILED })
                    }
                    val data = json.get("data")
                    if (data == null || data.isJsonNull) null else data.asJsonObject
                }
            } catch (e: Rk3LanException) {
                throw e
            } catch (e: SocketTimeoutException) {
                Log.e(TAG, "RK3 请求超时 $url", e)
                throw Rk3LanException(MSG_CONNECT_FAILED)
            } catch (e: UnknownHostException) {
                Log.e(TAG, "RK3 DNS 失败 $url", e)
                throw Rk3LanException(MSG_CONNECT_FAILED)
            } catch (e: ConnectException) {
                Log.e(TAG, "RK3 连接失败 $url", e)
                throw Rk3LanException(MSG_CONNECT_FAILED)
            } catch (e: IOException) {
                Log.e(TAG, "RK3 IO 异常 $url", e)
                throw Rk3LanException(MSG_CONNECT_FAILED)
            } catch (e: Exception) {
                Log.e(TAG, "RK3 请求异常 $url", e)
                throw Rk3LanException(MSG_CONNECT_FAILED)
            }
        }
    }

    companion object {
        const val TAG = "Rk3Lan"

        /** 服务器地址为空/非法（与设置页「RK3服务器地址」行联动） */
        const val MSG_SERVER_NOT_SET = "请前往设置填写RK3服务器地址"

        /** 网络 IO/超时/http 非 200/业务 code≠200 的统一局域网降级文案 */
        const val MSG_CONNECT_FAILED = "连接RK3设备失败，请确认手机和RK3设备连接同一个局域网WiFi"
    }
}
