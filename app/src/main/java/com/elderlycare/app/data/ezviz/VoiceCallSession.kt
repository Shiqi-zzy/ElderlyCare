package com.elderlycare.app.data.ezviz

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.ezviz.sdk.videotalk.EvcErrorMessage
import com.ezviz.sdk.videotalk.EvcMsgCallback
import com.ezviz.sdk.videotalk.EvcNotifyMessage
import com.ezviz.sdk.videotalk.EzvizVoiceCall
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 语音通话状态 */
sealed class VoiceCallState {
    /** 空闲 */
    data object Idle : VoiceCallState()

    /** 呼叫中（设备振铃） */
    data object Calling : VoiceCallState()

    /** 已接通（双向对讲） */
    data object Connected : VoiceCallState()

    /** 已结束 */
    data object Ended : VoiceCallState()

    /** 发起失败 */
    data class Failed(val reason: String) : VoiceCallState()
}

/**
 * 萤石 videotalk 语音通话会话 —— 留言模块「App → 设备」实时语音通路（通路①）。
 *
 * 使用前提：
 * 1. EZOpenSDK 已初始化且 accessToken 已注入（由 EzvizSdkManager 保证）；
 * 2. 设备能力集 support_talk = 1 或 3（调用方先做能力判断）。
 */
class VoiceCallSession {

    companion object {
        private const val TAG = "VoiceCallSession"
        private const val AUTO_HANG_UP_MS = 60_000L
    }

    private var voiceCall: EzvizVoiceCall? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val hangUpRunnable = Runnable { stop() }

    private val _state = MutableStateFlow<VoiceCallState>(VoiceCallState.Idle)
    val state: StateFlow<VoiceCallState> = _state.asStateFlow()

    /**
     * 发起语音通话（openAudio 打开本地麦克风，设备侧听到实时语音）。
     * 建议在主线程调用（SDK 内部要求）。返回 false 表示发起失败。
     */
    fun start(context: Context, deviceSerial: String): Boolean {
        val current = _state.value
        if (current is VoiceCallState.Calling || current is VoiceCallState.Connected) return true
        return try {
            val call = EzvizVoiceCall(object : EvcMsgCallback() {
                override fun onCallEstablished(p0: Int, p1: Int, p2: Int) {
                    // 通话已建立
                    Log.i(TAG, "通话已建立: $p0/$p1/$p2")
                    if (_state.value is VoiceCallState.Calling) {
                        _state.value = VoiceCallState.Connected
                        scheduleAutoHangUp()
                    }
                }

                override fun onOtherRefused() {
                    Log.i(TAG, "对方拒绝接听")
                    if (_state.value is VoiceCallState.Calling) {
                        _state.value = VoiceCallState.Ended
                        releaseCall()
                    }
                }

                override fun onOtherNoneAnswered() {
                    Log.i(TAG, "对方无人接听")
                    if (_state.value is VoiceCallState.Calling) {
                        _state.value = VoiceCallState.Ended
                        releaseCall()
                    }
                }

                override fun onOtherHangedUp() {
                    Log.i(TAG, "对方已挂断")
                    if (_state.value is VoiceCallState.Connected) {
                        _state.value = VoiceCallState.Ended
                        releaseCall()
                    }
                }

                override fun onNotify(message: EvcNotifyMessage) {
                    Log.i(TAG, "通话通知: origin=${message.origin} code=${message.code} desc=${message.desc}")
                }

                override fun onError(error: EvcErrorMessage) {
                    Log.e(TAG, "通话错误: ${error.name} desc=${error.desc} solution=${error.solution}")
                    val reason = error.solution?.takeIf { it.isNotBlank() }
                        ?: error.desc?.takeIf { it.isNotBlank() }
                        ?: error.name
                    _state.value = VoiceCallState.Failed(reason)
                    releaseCall()
                }

                // ===== 以下回调本模块未使用（多人通话/透传消息），仅记录日志 =====

                override fun onMessage(p0: Int, p1: String) {
                    Log.d(TAG, "onMessage: code=$p0 desc=$p1")
                }

                override fun onRcvLucidMsg(p0: String) {
                    Log.d(TAG, "onRcvLucidMsg: $p0")
                }

                override fun onRoomCreated(p0: Int) {
                    Log.d(TAG, "onRoomCreated: $p0")
                }

                override fun onJoinRoom(p0: Int, p1: Int, p2: String) {
                    Log.d(TAG, "onJoinRoom: $p0/$p1/$p2")
                }

                override fun onQuitRoom(p0: Int, p1: Int) {
                    Log.d(TAG, "onQuitRoom: $p0/$p1")
                }
            })
            voiceCall = call
            _state.value = VoiceCallState.Calling
            call.startVideoTalk(context.applicationContext, deviceSerial)
            // 打开本地麦克风，否则设备侧听不到声音
            call.openAudio()
            true
        } catch (e: Exception) {
            Log.e(TAG, "发起语音通话失败", e)
            _state.value = VoiceCallState.Failed(e.message ?: "发起语音通话失败")
            releaseCall()
            false
        }
    }

    /** 结束通话（幂等，任何状态都可调用） */
    fun stop() {
        mainHandler.removeCallbacks(hangUpRunnable)
        try {
            voiceCall?.stopVoiceTalk()
        } catch (e: Exception) {
            Log.w(TAG, "stopVoiceTalk 异常", e)
        }
        releaseCall()
        val current = _state.value
        if (current !is VoiceCallState.Idle && current !is VoiceCallState.Ended) {
            _state.value = VoiceCallState.Ended
        }
    }

    private fun releaseCall() {
        try {
            voiceCall?.release()
        } catch (e: Exception) {
            Log.w(TAG, "release 异常", e)
        }
        voiceCall = null
    }

    private fun scheduleAutoHangUp() {
        mainHandler.removeCallbacks(hangUpRunnable)
        mainHandler.postDelayed(hangUpRunnable, AUTO_HANG_UP_MS)
    }
}
