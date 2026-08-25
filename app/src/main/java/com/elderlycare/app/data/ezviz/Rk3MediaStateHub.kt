package com.elderlycare.app.data.ezviz

import com.elderlycare.app.data.ezviz.model.MediaPlayState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * RK3 点播/FM 播放状态中枢（webhook 回调 → UI 状态同步）。
 *
 * 需求约束：播放状态不轮询拉接口，依靠萤石 webhook 回调更新 UI。
 * 链路设计：萤石 webhook → FastAPI 后端 → 既有 WebSocket → App
 * → RtcSignalingManager.onMessage 解析 → 写入本 Hub → 页面 StateFlow 自动刷新。
 *
 * ⚠️ 萤石播放状态 webhook 事件的 type 与 body 字段【待萤石商务开通权限，
 * 拿到官方抓包报文/内部PDF文档后确认】，当前禁止猜测字段——
 * onWebhookState 为未来接入点，事件解析代码在 RtcSignalingManager 留 TODO 挂点。
 */
object Rk3MediaStateHub {

    data class MediaState(
        val deviceSerial: String = "",
        val state: MediaPlayState = MediaPlayState.IDLE,
        /** 当前播放内容标题（点播=音频名 / FM=电台名），空=未知 */
        val title: String = ""
    )

    private val _state = MutableStateFlow<MediaState?>(null)
    val state: StateFlow<MediaState?> = _state.asStateFlow()

    /** 未来接入点：webhook 事件解析后调用（事件结构待萤石内部文档，禁止猜测字段） */
    fun onWebhookState(mediaState: MediaState) {
        _state.value = mediaState
    }
}
