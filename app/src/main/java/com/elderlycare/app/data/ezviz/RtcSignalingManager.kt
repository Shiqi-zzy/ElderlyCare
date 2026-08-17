package com.elderlycare.app.data.ezviz

import android.util.Log
import com.elderlycare.app.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

/**
 * 云通话信令客户端：连接后端 WebSocket，接收设备来电事件。
 *
 * 后端 webhook 收到萤石 ys.calling 后，会推 `incoming_call`(status=1) / `call_state`(status=3挂断)
 * 事件到 App，App 据此弹出来电提示。
 */
object RtcSignalingManager {

    private const val TAG = "RtcSignaling"

    data class IncomingCall(
        val deviceSerial: String,
        val callingId: String,
        val roomId: String,
    )

    private val _incomingCall = MutableStateFlow<IncomingCall?>(null)
    val incomingCall: StateFlow<IncomingCall?> = _incomingCall.asStateFlow()

    private var webSocket: WebSocket? = null

    /** 连接后端 WebSocket（家属端启动后调用） */
    fun connect(clientId: String = "family001") {
        if (webSocket != null) return
        val wsUrl = BuildConfig.RTC_BACKEND_URL
            .replaceFirst("http", "ws", ignoreCase = true)
            .trimEnd('/') + "/api/ws?clientId=$clientId"
        Log.d(TAG, "连接 WebSocket: $wsUrl")

        val request = Request.Builder().url(wsUrl).build()
        val client = OkHttpClient()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket 已连接")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "收到信令: $text")
                try {
                    val obj = JSONObject(text)
                    val type = obj.optString("type")
                    val data = obj.optJSONObject("data") ?: return
                    when (type) {
                        "incoming_call" -> {
                            _incomingCall.value = IncomingCall(
                                deviceSerial = data.optString("deviceSerial"),
                                callingId = data.optString("callingId"),
                                roomId = data.optString("roomId"),
                            )
                        }
                        "call_state" -> {
                            // 设备取消/拒接/响铃超时/挂断等 → 取消来电提示
                            _incomingCall.value = null
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "解析信令失败", e)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket 连接失败: ${t.message}")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket 关闭: $code $reason")
            }
        })
    }

    /** 消费掉当前来电（弹窗关闭/接听后调用） */
    fun consume() {
        _incomingCall.value = null
    }
}
