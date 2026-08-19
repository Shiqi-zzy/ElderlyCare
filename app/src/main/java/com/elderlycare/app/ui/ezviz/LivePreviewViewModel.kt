package com.elderlycare.app.ui.ezviz

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elderlycare.app.data.ezviz.NetworkResult
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.ezviz.model.LiveStream
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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
    val showCodeInput: Boolean = true
)

class LivePreviewViewModel : ViewModel() {

    private val TAG = "LivePreviewViewModel"
    private val repo = ServiceLocator.repository

    private val _uiState = MutableStateFlow(LivePreviewUiState())
    val uiState: StateFlow<LivePreviewUiState> = _uiState.asStateFlow()

    private var currentStream: LiveStream? = null
    private var addressRefreshJob: Job? = null
    private var playJob: Job? = null

    fun initialize(deviceSerial: String, verifyCode: String) {
        _uiState.update {
            it.copy(deviceSerial = deviceSerial, verifyCode = verifyCode)
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
        // 幂等守卫：上一次取流请求仍在进行时不重复发起（防快速重试/重复初始化并发取流）
        if (playJob?.isActive == true) return
        playJob = viewModelScope.launch {
            val state = _uiState.value
            val code = state.verifyCode.takeIf { it.length == 6 }
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
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
                            // 地址为空 = 设备离线/未开启直播，与验证码无关，不再强行弹验证码框
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = "无法获取播放地址，设备可能离线或未开启直播",
                                    showCodeInput = false
                                )
                            }
                        }
                    }
                    is NetworkResult.Error -> {
                        val msg = result.message
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = msg,
                                // 结构化优先：设备加密/需验证码的已知业务码（NetworkResult.code）；
                                // 中文/英文文案兜底。不再用裸 "code" 子串判断，
                                // 避免含 "error_code" 等内容的网络错误误弹验证码框。
                                showCodeInput = isVerifyCodeRelated(result.code, msg)
                            )
                        }
                    }
                }
            } finally {
                // 兜底：任何路径（含 toPlayableUrl 抛异常）都保证 loading 消失，不卡加载态
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /** 判断是否需要弹出验证码输入框：优先业务码（60019=加密已开启），再中文/英文文案兜底。 */
    private fun isVerifyCodeRelated(code: String?, msg: String): Boolean = when {
        code == "60019" -> true
        msg.contains("验证码") || msg.contains("加密") -> true
        msg.contains("verification") || msg.contains("invalid code") -> true
        else -> false
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
     * ezopen:// 协议（萤石加密流）ExoPlayer 无法直接播放，拼萤石 JSSDK 网页播放器地址
     * （与回放页同一套方案）；http(s) 直链原样返回走 ExoPlayer。
     */
    private fun toPlayableUrl(rawUrl: String?): Pair<String, Boolean>? {
        val raw = rawUrl?.takeIf { it.isNotBlank() } ?: return null
        return if (raw.startsWith("ezopen://")) {
            val token = ServiceLocator.tokenManager.getTokenForcefully()
            val jssdkUrl = "https://open.ys7.com/console/jssdk/pc.html" +
                "?accessToken=${token ?: ""}" +
                "&url=${Uri.encode(raw)}"
            Log.d(TAG, "ezopen 协议，切换 WebView 播放")
            jssdkUrl to true
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
}
