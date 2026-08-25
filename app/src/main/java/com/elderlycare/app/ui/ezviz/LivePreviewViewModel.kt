package com.elderlycare.app.ui.ezviz

import android.app.Application
import android.util.Log
import android.view.SurfaceHolder
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.elderlycare.app.data.ezviz.NetworkResult
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.ezviz.model.LiveStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LivePreviewUiState(
    val deviceSerial: String = "",
    val channelNo: Int = 1,
    val streamUrl: String? = null,
    /** ezopen 协议地址是否走萤石 JSSDK WebView 播放（加密设备） */
    val useWebView: Boolean = false,
    val playerState: PlayerState = PlayerState.Idle,
    val isMuted: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val verifyCode: String = "",
    val showCodeInput: Boolean = true,
    /** 手机本地录制（EZOpenSDK 隐藏会话）是否进行中 */
    val isRecording: Boolean = false,
    /** 手动抓拍（SDK 本地抓帧 + 上传后端）进行中（防连点；同设备 4s 限流由后端保证） */
    val isCapturing: Boolean = false
)

class LivePreviewViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "LivePreviewViewModel"
    private val appContext = application.applicationContext
    private val repo = ServiceLocator.repository

    private val _uiState = MutableStateFlow(LivePreviewUiState())
    val uiState: StateFlow<LivePreviewUiState> = _uiState.asStateFlow()

    private var currentStream: LiveStream? = null
    private var addressRefreshJob: Job? = null

    /** 隐藏录制会话（1x1 SurfaceView 的解码 surface 由页面回传） */
    private var recordSession: LocalMediaCapture.RecordSession? = null
    private var recordSurfaceHolder: SurfaceHolder? = null
    /** 录制会话创建中（防连点重复建会话；点击停止可打断） */
    private var recordStarting = false

    /** 隐藏抓帧会话（与录制会话互斥，串行复用同一 1x1 SurfaceView） */
    private var captureSession: LocalMediaCapture.CaptureFrameSession? = null
    /** 抓帧会话创建中（防连点重复建会话） */
    private var captureStarting = false

    init {
        // 预览断开（取流错误/流地址丢失）时若在录制，自动停止防止文件损坏
        viewModelScope.launch {
            _uiState.collect { state ->
                val lost = state.playerState is PlayerState.Error ||
                    (state.streamUrl == null && !state.isLoading)
                if (state.isRecording && lost) {
                    Log.i(TAG, "预览断开，自动停止本地录制")
                    stopRecording(auto = true)
                }
            }
        }
    }

    fun initialize(deviceSerial: String, verifyCode: String) {
        _uiState.update {
            it.copy(deviceSerial = deviceSerial, verifyCode = verifyCode)
        }
        // 幂等补传设备验证码（device_auth 兜底同步第二触发点；upsert 可重复调用）
        if (verifyCode.length == 6) {
            viewModelScope.launch {
                ServiceLocator.captureRepository.uploadDeviceAuth(deviceSerial, verifyCode)
            }
        }
        startPlay()
    }

    fun setVerifyCode(code: String) {
        _uiState.update { it.copy(verifyCode = code.take(6)) }
    }

    fun bindPlayer(player: EzvizPlayer) {
        player.setOnStateChangeListener { state ->
            _uiState.update { it.copy(playerState = state) }
            if (state is PlayerState.Error) {
                Log.e(TAG, "播放错误: ${state.message}，尝试重新获取流地址")
                viewModelScope.launch { refreshStreamAddress() }
            }
        }
    }

    private fun startPlay() {
        viewModelScope.launch {
            val state = _uiState.value
            val code = state.verifyCode.takeIf { it.length == 6 }
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = repo.getLiveAddress(state.deviceSerial, code)) {
                is NetworkResult.Success -> {
                    val stream = result.data
                    val playable = toPlayableUrl(stream.getPreferredUrl())
                    if (playable != null) {
                        currentStream = stream
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                streamUrl = playable.first,
                                useWebView = playable.second,
                                error = null,
                                showCodeInput = false
                            )
                        }
                        startAddressRefreshTimer()
                    } else {
                        Log.w(TAG, "直播地址为空: hls=${stream.hlsUrl}, flv=${stream.flvUrl}, rtmp=${stream.rtmpUrl}")
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "无法获取播放地址，设备可能离线、未开启直播或需要验证码",
                                showCodeInput = true
                            )
                        }
                    }
                }
                is NetworkResult.Error -> {
                    val raw = result.message
                    val friendly = FriendlyEzError.message(raw)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = friendly,
                            showCodeInput = raw.contains("验证码") || raw.contains("加密") || raw.contains("code", ignoreCase = true)
                        )
                    }
                }
            }
        }
    }

    private fun startAddressRefreshTimer() {
        addressRefreshJob?.cancel()
        addressRefreshJob = viewModelScope.launch {
            while (isActive) {
                delay(2 * 60 * 1000L)
                Log.d(TAG, "定时刷新流地址…")
                refreshStreamAddress()
            }
        }
    }

    private suspend fun refreshStreamAddress() {
        val state = _uiState.value
        val code = state.verifyCode.takeIf { it.length == 6 }
        when (val result = repo.getLiveAddress(state.deviceSerial, code)) {
            is NetworkResult.Success -> {
                val playable = toPlayableUrl(result.data.getPreferredUrl())
                if (playable != null && playable.first != state.streamUrl) {
                    currentStream = result.data
                    _uiState.update {
                        it.copy(streamUrl = playable.first, useWebView = playable.second, error = null)
                    }
                }
            }
            is NetworkResult.Error -> Log.e(TAG, "刷新流地址失败: ${result.message}")
        }
    }

    fun toggleMute() {
        _uiState.update { it.copy(isMuted = !it.isMuted) }
    }

    /**
     * 把取流地址转换为可播放地址：
     * ezopen:// 协议（萤石加密流）ExoPlayer 无法直接播放，交给本地 assets/ez-player.html
     * （EZUIKit JS SDK，与回放页同一套方案）；http(s) 直链原样返回走 ExoPlayer。
     */
    private fun toPlayableUrl(rawUrl: String?): Pair<String, Boolean>? {
        val raw = rawUrl?.takeIf { it.isNotBlank() } ?: return null
        return if (raw.startsWith("ezopen://")) {
            val state = _uiState.value
            val token = ServiceLocator.tokenManager.getTokenForcefully()
            val localUrl = LocalEzPlayerUrl.build(
                mode = "live",
                deviceSerial = state.deviceSerial,
                channelNo = state.channelNo,
                accessToken = token ?: "",
                rawUrl = raw
            )
            Log.d(TAG, "ezopen 协议，切换本地 EZUIKit WebView 播放")
            localUrl to true
        } else {
            raw to false
        }
    }

    fun closeLive() {
        addressRefreshJob?.cancel()
        val state = _uiState.value
        viewModelScope.launch {
            repo.closeLive(state.deviceSerial, state.channelNo)
        }
    }

    fun retry() {
        startPlay()
    }

    // ==================== 手动抓拍（SDK 本地抓帧）/ 手机本地录制 ====================

    /** 预览是否已成功连接（抓拍/录制按钮的可用判定；H5 路径取流即视为连接） */
    private fun isPreviewReady(): Boolean {
        val s = _uiState.value
        return s.streamUrl != null && !s.isLoading && s.error == null &&
            (s.useWebView || s.playerState == PlayerState.Playing)
    }

    /** 隐藏录制会话的解码 surface（页面 SurfaceView 回调） */
    fun onRecordSurfaceReady(holder: SurfaceHolder?) {
        recordSurfaceHolder = holder
        if (holder == null) {
            // surface 销毁时若在录制，自动停止（页面销毁边界）
            if (_uiState.value.isRecording) stopRecording(auto = true)
        }
    }

    /**
     * 抓拍（手动抓拍 = EZOpenSDK 本地抓帧 + 上传后端）：
     * 隐藏 CaptureFrameSession → capturePicture 抓帧 → 上传后端落 alarm_events(manual)
     * （4s 限流由后端保证）→ 图库保存 + toast「截图已保存」；
     * 「全部抓拍」页经 WS/进页拉取展示。与录制会话互斥（同一 1x1 SurfaceView）。
     */
    fun captureSnapshot() {
        val state = _uiState.value
        if (!isPreviewReady()) {
            toast("请等待视频流连接成功")
            return
        }
        if (state.isCapturing || captureStarting) {
            toast("操作进行中，请稍后再试")
            return
        }
        if (state.isRecording) {
            toast("正在录像，请先停止录像")
            return
        }
        val holder = recordSurfaceHolder
        if (holder == null) {
            toast("请等待视频流连接成功")
            return
        }
        // 先建会话对象并登记，SDK 调用异步发起
        val session = LocalMediaCapture.CaptureFrameSession(appContext)
        captureSession = session
        captureStarting = true
        _uiState.update { it.copy(isCapturing = true) }
        viewModelScope.launch(Dispatchers.IO) {
            // 抓帧会话走 SDK 内部 REST 取流：先刷新 SDK accessToken 防过期
            val token = runCatching { repo.obtainValidToken() }.getOrNull()
            ServiceLocator.sdkManager.updateToken(token)
            withContext(Dispatchers.Main) {
                val started = session.start(
                    deviceSerial = state.deviceSerial,
                    channelNo = state.channelNo,
                    verifyCode = state.verifyCode,
                    holder = holder,
                    listener = object : LocalMediaCapture.CaptureFrameSession.Listener {
                        override fun onCaptured(file: java.io.File) {
                            captureStarting = false
                            if (captureSession === session) captureSession = null
                            handleCapturedFrame(state.deviceSerial, file)
                        }

                        override fun onFailed(friendlyMessage: String) {
                            captureStarting = false
                            if (captureSession === session) captureSession = null
                            _uiState.update { it.copy(isCapturing = false) }
                            toast(friendlyMessage)
                        }
                    }
                )
                if (!started) {
                    // 会话创建失败
                    captureStarting = false
                    if (captureSession === session) captureSession = null
                    _uiState.update { it.copy(isCapturing = false) }
                    if (!session.isReleased()) toast("抓拍启动失败，请重试")
                }
            }
        }
    }

    /** 抓帧成功 → 上传后端（4s 限流）→ 存相册；上传失败不影响本机保存 */
    private fun handleCapturedFrame(deviceSerial: String, file: java.io.File) {
        viewModelScope.launch {
            val upload = ServiceLocator.captureRepository.uploadFrame(deviceSerial, file)
            upload.onSuccess { toast(it) } // 「截图已保存」
            upload.onFailure { e -> toast(e.message ?: "抓拍失败，请重试") }
            // 无论上传成败都保存到相册（本机截图用户可见）
            runCatching {
                withContext(Dispatchers.IO) {
                    LocalMediaCapture.saveImageToGallery(appContext, file, "capture")
                }
            }.onFailure { Log.e(TAG, "抓帧入库相册失败", it) }
            file.delete()
            _uiState.update { it.copy(isCapturing = false) }
        }
    }

    /** 录制按钮点击：未录制 → 开始；录制中 → 停止 */
    fun toggleRecord() {
        if (_uiState.value.isRecording) {
            stopRecording(auto = false)
        } else {
            startRecording()
        }
    }

    private fun startRecording() {
        val state = _uiState.value
        if (state.isRecording || recordStarting) return
        if (!isPreviewReady()) {
            toast("请等待视频流连接成功")
            return
        }
        val holder = recordSurfaceHolder
        if (holder == null) {
            toast("请等待视频流连接成功")
            return
        }
        // 先建会话对象并登记（点击停止可打断创建流程），SDK 调用异步发起
        val session = LocalMediaCapture.RecordSession(appContext)
        recordSession = session
        recordStarting = true
        viewModelScope.launch(Dispatchers.IO) {
            // 录制会话走 SDK 内部 REST 取流：先刷新 SDK accessToken 防过期
            val token = runCatching { repo.obtainValidToken() }.getOrNull()
            ServiceLocator.sdkManager.updateToken(token)
            withContext(Dispatchers.Main) {
                val started = session.start(
                    deviceSerial = state.deviceSerial,
                    channelNo = state.channelNo,
                    verifyCode = state.verifyCode,
                    holder = holder,
                    listener = object : LocalMediaCapture.RecordSession.Listener {
                        override fun onRecordStarted() {
                            recordStarting = false
                            _uiState.update { it.copy(isRecording = true) }
                        }

                        override fun onRecordFailed(friendlyMessage: String) {
                            recordStarting = false
                            recordSession = null
                            _uiState.update { it.copy(isRecording = false) }
                            toast(friendlyMessage)
                        }
                    }
                )
                if (!started) {
                    // 会话被停止打断或创建失败
                    recordStarting = false
                    if (recordSession === session) recordSession = null
                    _uiState.update { it.copy(isRecording = false) }
                    if (!session.isReleased()) toast("录制启动失败，请重试")
                }
            }
        }
    }

    /**
     * 停止录制并落库相册。
     * @param auto true = 预览断开/页面销毁的自动停止（静默保存，不弹 toast）
     */
    fun stopRecording(auto: Boolean) {
        recordStarting = false
        val session = recordSession ?: return
        recordSession = null
        val wasRecording = _uiState.value.isRecording
        _uiState.update { it.copy(isRecording = false) }
        viewModelScope.launch(Dispatchers.IO) {
            val saved = runCatching { session.stop() }.getOrElse {
                Log.e(TAG, "停止录制异常", it)
                null
            }
            if (auto) return@launch // 自动停止静默保存，不打扰用户
            withContext(Dispatchers.Main) {
                if (saved != null) toast("录像已保存")
                else if (wasRecording) toast("录制已停止")
            }
        }
    }

    /** H5 播放器报错（预览断开）→ 录制中则自动停止 */
    fun notifyPreviewDisconnected() {
        Log.i(TAG, "H5 播放器报告错误，检查录制会话")
        if (_uiState.value.isRecording) stopRecording(auto = true)
    }

    private fun toast(msg: String) {
        Toast.makeText(appContext, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onCleared() {
        super.onCleared()
        // 页面销毁兜底：正在录制必须停止会话，防止文件损坏
        recordSession?.let {
            runCatching { it.stop() }
        }
        recordSession = null
        // 抓帧会话直接回收（不落库、不回调）
        captureSession?.let {
            runCatching { it.release() }
        }
        captureSession = null
    }
}
