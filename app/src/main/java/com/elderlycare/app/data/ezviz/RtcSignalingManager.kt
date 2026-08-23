package com.elderlycare.app.data.ezviz

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.elderlycare.app.BuildConfig
import com.elderlycare.app.data.ezviz.model.AlarmMessage
import com.elderlycare.app.util.AppForegroundTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * 云通话信令 + 告警推送客户端：连接后端 WebSocket，接收设备来电/告警事件。
 *
 * 后端 webhook 收到萤石 ys.calling 后，会推 `incoming_call`(status=1) / `call_state`(status=3挂断)
 * 事件到 App，App 据此弹出来电提示。
 *
 * 告警通道（2026-08 新增）：App 连接后上报已授权设备 SN 订阅（{"type":"subscribe","sns":[...]}，
 * 仅家属端上报；医院端不订阅、靠消息中心 60s 轮询兜底），后端按订阅精准推 `alarm` 事件，
 * App 落库（复用 saveAlertMessages，alarmId 幂等——与 60s 轮询双通道重复到达靠
 * remoteId 唯一索引 + insertIgnore 去重）。
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

    /** 抓拍更新信号：新告警落库/告警图片就绪（captureUpdated）后 emit，全部抓拍页/首页角标据此刷新 */
    private val _captureFeed = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    val captureFeed: SharedFlow<Unit> = _captureFeed.asSharedFlow()

    private var webSocket: WebSocket? = null
    private val reconnectScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var reconnectScheduled = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var appContext: Context? = null

    /** 当前已授权设备 SN 集合（ElderlyCareApp 订阅 AuthorizedSnsProvider 流后更新；仅家属端上报非空） */
    private val authorizedSns = AtomicReference<Set<String>>(emptySet())

    /** 初始化（ElderlyCareApp.onCreate 调用，先于 connect）。 */
    fun init(appContext: Context) {
        this.appContext = appContext.applicationContext
    }

    /** 连接后端 WebSocket（家属端启动后调用）。断线自动重连，App 存活期间保持在线。 */
    fun connect(clientId: String = "family001") {
        if (webSocket != null) return
        val wsUrl = BuildConfig.RTC_BACKEND_URL
            .replaceFirst("http", "ws", ignoreCase = true)
            .trimEnd('/') + "/api/ws?clientId=$clientId"
        Log.d(TAG, "连接 WebSocket: $wsUrl")

        val request = Request.Builder().url(wsUrl).build()
        // pingInterval：每 20s 发协议层心跳，防止熄屏/Doze 把空闲连接断开
        val client = OkHttpClient.Builder()
            .pingInterval(20, TimeUnit.SECONDS)
            .build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket 已连接")
                // 连接建立（含断线重连）后立即上报授权订阅
                sendSubscribe()
                // 幂等补传设备验证码（存量已绑定设备兜底同步 device_auth；upsert 可重复调用）
                resyncDeviceAuth()
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
                        "alarm" -> {
                            handleAlarm(data)
                        }
                        "captureUpdated" -> {
                            // 告警图片在后端下载/解密落盘完成 → 通知全部抓拍页/角标刷新
                            _captureFeed.tryEmit(Unit)
                        }
                        // TODO【RK3点播/广播FM】: 萤石播放状态 webhook 事件
                        // （事件 type 与 body 结构待萤石商务开通权限后按官方抓包报文/内部PDF文档确认，
                        //   禁止猜测字段）。事件到达后在此解析 → Rk3MediaStateHub.onWebhookState(...)，
                        //   点播/FM 页面订阅 Rk3MediaStateHub.state 自动刷新播放状态（不轮询）。
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "解析信令失败", e)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket 连接失败: ${t.message}")
                this@RtcSignalingManager.webSocket = null
                scheduleReconnect(clientId)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket 关闭: $code $reason")
                this@RtcSignalingManager.webSocket = null
                scheduleReconnect(clientId)
            }
        })
    }

    /**
     * 更新告警订阅并上报后端。
     * ElderlyCareApp 订阅 AuthorizedSnsProvider.flow() 后调用；
     * 仅家属端上报非空（医院端不实时收告警，靠 60s 轮询兜底，发空列表清订阅）。
     */
    fun updateAuthorizedSns(sns: Set<String>) {
        authorizedSns.set(sns)
        sendSubscribe()
    }

    /** 上报订阅：{"type":"subscribe","sns":[...]}（连接已建立才发；onOpen 重连后自动补发） */
    private fun sendSubscribe() {
        val ws = webSocket ?: return
        val payload = JSONObject().apply {
            put("type", "subscribe")
            put("sns", JSONArray(authorizedSns.get().toList()))
        }
        runCatching { ws.send(payload.toString()) }
            .onFailure { Log.w(TAG, "告警订阅上报失败", it) }
    }

    /** 后端推送的告警事件 → 复用现有 saveAlertMessages 落库（alarmId 幂等，Room 流自动刷新 UI） */
    private fun handleAlarm(data: JSONObject) {
        val alarmId = data.optString("alarmId")
        val deviceSerial = data.optString("deviceSerial")
        if (alarmId.isBlank() || deviceSerial.isBlank()) {
            Log.w(TAG, "告警事件缺少 alarmId/deviceSerial，丢弃: $data")
            return
        }
        // 图片只进后端 alarm_events（全部抓拍页独享），Room 报警文字不落图字段
        val alarm = AlarmMessage(
            alarmId = alarmId,
            deviceSerial = deviceSerial,
            channelNo = data.optInt("channelNo", 1),
            alarmName = data.optString("alarmName").ifBlank { "设备报警" },
            alarmType = data.optInt("alarmType", 0),
            alarmTime = data.optString("alarmTime"),
            alarmPicUrl = null,
            alarmVideoUrl = null,
            isRead = false,
            isChecked = false,
            deviceName = data.optString("deviceName").takeIf { it.isNotBlank() },
            preRecordUrl = null
        )
        // onMessage 运行在 OkHttp 线程，直接 runBlocking（saveAlertMessages 内部切 IO + dedupMutex 幂等）
        runCatching {
            runBlocking { ServiceLocator.messageRepository.saveAlertMessages(listOf(alarm)) }
        }.onFailure { e ->
            Log.w(TAG, "告警落库失败: alarmId=$alarmId", e)
            return
        }
        // 前台 toast 提醒；后台只落库/角标不打扰
        if (AppForegroundTracker.isForeground) {
            val text = "收到新告警：${alarm.alarmName}"
            mainHandler.post {
                appContext?.let { Toast.makeText(it, text, Toast.LENGTH_SHORT).show() }
            }
        }
        // 全部抓拍页/首页角标刷新信号
        _captureFeed.tryEmit(Unit)
    }

    /** 幂等补传设备验证码（绑定存储 → 后端 device_auth；upsert，重连/重绑可重复调用）。 */
    private fun resyncDeviceAuth() {
        val bound = runCatching { ServiceLocator.deviceBindingStore.load() }.getOrNull() ?: return
        val serial = bound.deviceSerial
        val code = bound.validateCode
        if (serial.isBlank() || code.isBlank()) return
        reconnectScope.launch {
            ServiceLocator.captureRepository.uploadDeviceAuth(serial, code)
                .onFailure { Log.w(TAG, "设备验证码补传失败: $serial", it) }
        }
    }

    /** 5s 后重连（去重：onFailure 与 onClosed 会成对触发） */
    private fun scheduleReconnect(clientId: String) {
        if (reconnectScheduled) return
        reconnectScheduled = true
        reconnectScope.launch {
            delay(5_000)
            reconnectScheduled = false
            connect(clientId)
        }
    }

    /** 消费掉当前来电（弹窗关闭/接听后调用） */
    fun consume() {
        _incomingCall.value = null
    }
}
