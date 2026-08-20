package com.elderlycare.app.ui.ezviz

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
}
