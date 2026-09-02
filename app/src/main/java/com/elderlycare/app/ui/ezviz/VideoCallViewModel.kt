package com.elderlycare.app.ui.ezviz

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ez.basertc.RTCConstant
import com.ez.basertc.RTCListener
import com.ez.basertc.view.VideoCanvasView
import com.elderlycare.app.BuildConfig
import com.elderlycare.app.data.ezviz.ErtcManager
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.ezviz.TokenRequestBody
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CallState { IDLE, INIT, ENTERING, CONNECTED, ENDED, ERROR }

data class VideoCallUiState(
    val state: CallState = CallState.IDLE,
    val remoteUserId: String? = null,
    val error: String? = null,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = true,
    val isCameraOn: Boolean = true,
    val elapsedSeconds: Int = 0,
)

/**
 * 云通话 ViewModel：初始化 ERTCEngine → 入会 → （客户端呼叫时）邀请设备。
 * 设备呼叫 App 的方向：App 直接入会即可，设备已在房间。
 */
class VideoCallViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "VideoCallViewModel"

    private val _uiState = MutableStateFlow(VideoCallUiState())
    val uiState: StateFlow<VideoCallUiState> = _uiState.asStateFlow()

    private var param: ErtcManager.RoomParam? = null
    private var isClientCall = false
    private var remoteEntered = false
    private var timerJob: Job? = null

    private val listener = object : RTCListener() {
        override fun onEnterRoomSuccess() {
            Log.d(TAG, "进入房间成功")
            _uiState.update { it.copy(state = CallState.ENTERING) }
            // 入会后重新确保本地音视频开启（时序/模拟器防御）
            ErtcManager.enableLocalAudio(!_uiState.value.isMuted)
            ErtcManager.enableLocalVideo(_uiState.value.isCameraOn)
            if (isClientCall) {
                // 客户端呼叫设备：邀请设备入会
                param?.let { ErtcManager.inviteDevice(it) }
            }
        }

        override fun onRemoteUserEnterRoom(userId: String) {
            Log.d(TAG, "远端用户进入房间 userId=$userId")
            remoteEntered = true
            _uiState.update { it.copy(state = CallState.CONNECTED, remoteUserId = userId) }
            startTimer()
        }

        override fun onRemoteUserLeaveRoom(userId: String, reason: Int) {
            Log.d(TAG, "远端用户离开 userId=$userId reason=$reason")
            endCall("对方已挂断")
        }

        override fun onUserVideoAvailable(userId: String, available: Boolean, streamType: Int) {
            Log.d(TAG, "远端视频 $userId available=$available")
            if (available) {
                ErtcManager.setRemoteView(userId, remoteView)
            } else {
                ErtcManager.setRemoteView(userId, null)
            }
        }

        override fun onError(errorCode: Int) {
            Log.e(TAG, "ERTC 错误 errorCode=$errorCode")
            endCall("通话出错($errorCode)")
        }
    }

    private var localView: VideoCanvasView? = null
    private var remoteView: VideoCanvasView? = null

    /** 由导航层调用。tokens 从后端 /api/rtc/token 获取（联调时补齐）。 */
    fun startCall(param: ErtcManager.RoomParam, clientCallDevice: Boolean) {
        this.param = param
        this.isClientCall = clientCallDevice
        _uiState.update { it.copy(state = CallState.INIT) }

        ErtcManager.init(
            context = getApplication(),
            appId = param.appId,
            onReady = { engine ->
                ErtcManager.setListener(listener)
                // 视频编码参数（与官方 demo 一致）
                ErtcManager.setupVideo()
                // 硬件回音消除（必须）
                ErtcManager.enableHardAec(true)
                ErtcManager.enableLocalVideo(true)
                ErtcManager.enableLocalAudio(true)
                // 扬声器外放
                ErtcManager.setSpeakerPhoneOn(true)
                ErtcManager.setLocalView(localView)
                ErtcManager.enterRoom(param)
            },
            onError = { code -> endCall("ERTC 初始化失败($code)") },
        )
    }

    /** 从后端取 clientToken + deviceToken 后发起通话。
     *  [roomId] 为空 → App 主动呼叫设备（后端生成新房间）；非空 → 设备呼叫 App（加入设备已创建的房间）。 */
    fun startCallFromBackend(deviceSerial: String, account: String, customId: String, roomId: String, isClientCall: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(state = CallState.INIT, error = null) }
            try {
                val resp = ServiceLocator.rtcBackendApi.getToken(
                    TokenRequestBody(room_id = roomId, custom_id = customId, device_serial = deviceSerial)
                )
                val data = resp.data
                if (resp.code != 200 || data == null || data.client_token.isBlank()) {
                    _uiState.update {
                        it.copy(state = CallState.ERROR, error = resp.message.ifBlank { "获取通话凭证失败" })
                    }
                    return@launch
                }
                val param = ErtcManager.RoomParam(
                    appId = data.app_id,
                    roomId = data.room_id,
                    userId = data.user_id.ifBlank { customId },
                    clientToken = data.client_token,
                    deviceToken = data.device_token,
                    deviceSerial = data.device_serial.ifBlank { deviceSerial },
                )
                startCall(param, clientCallDevice = isClientCall)
            } catch (e: Exception) {
                Log.e(TAG, "获取通话凭证失败", e)
                _uiState.update { it.copy(state = CallState.ERROR, error = "获取通话凭证失败: ${e.message}") }
            }
        }
    }

    /** 由 UI 层在 AndroidView 创建后回调，绑定本地/远端视频画布 */
    fun bindLocalView(view: VideoCanvasView?) {
        localView = view
        view?.setRenderType(RTCConstant.ERTC_VIDEO_RENDER_MODE_FILL)
        if (_uiState.value.state != CallState.IDLE) {
            ErtcManager.setLocalView(view)
        }
    }

    fun bindRemoteView(view: VideoCanvasView?) {
        remoteView = view
        view?.setRenderType(RTCConstant.ERTC_VIDEO_RENDER_MODE_FIT)
        _uiState.value.remoteUserId?.let { uid ->
            if (view != null) ErtcManager.setRemoteView(uid, view)
        }
    }

    fun toggleMic() {
        val muted = !_uiState.value.isMuted
        _uiState.update { it.copy(isMuted = muted) }
        ErtcManager.enableLocalAudio(!muted)
    }

    fun toggleSpeaker() {
        val on = !_uiState.value.isSpeakerOn
        _uiState.update { it.copy(isSpeakerOn = on) }
        ErtcManager.setSpeakerPhoneOn(on)
    }

    fun toggleCamera() {
        val on = !_uiState.value.isCameraOn
        _uiState.update { it.copy(isCameraOn = on) }
        ErtcManager.enableLocalVideo(on)
    }

    fun switchCamera() = ErtcManager.switchCamera()

    fun hangUp() {
        // 客户端呼叫设备且设备未接听 → 取消邀请
        if (isClientCall && !remoteEntered) {
            param?.let { ErtcManager.cancelInviteDevice(it) }
        }
        endCall("通话结束")
    }

    private fun endCall(message: String) {
        if (_uiState.value.state == CallState.ENDED) return
        timerJob?.cancel()
        _uiState.update {
            it.copy(
                state = CallState.ENDED,
                error = if (it.state == CallState.CONNECTED) null else message,
            )
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var sec = 0
            while (true) {
                delay(1000)
                sec++
                _uiState.update { it.copy(elapsedSeconds = sec) }
                // 15 秒仍未接通视为失败
                if (sec >= 15 && !remoteEntered) {
                    endCall("设备无人接听")
                    break
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        ErtcManager.release()
    }
}
