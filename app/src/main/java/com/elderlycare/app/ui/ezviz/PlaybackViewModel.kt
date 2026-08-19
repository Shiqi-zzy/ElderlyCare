package com.elderlycare.app.ui.ezviz

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elderlycare.app.data.ezviz.NetworkResult
import com.elderlycare.app.data.ezviz.ServiceLocator
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class PlaybackUiState(
    val deviceSerial: String = "",
    val channelNo: Int = 1,
    val selectedDate: String = "",
    val startTime: String = "",
    val stopTime: String = "",
    val verifyCode: String = "",
    val playbackUrl: String? = null,
    val useWebView: Boolean = false,
    val isLoading: Boolean = false,
    val isPlaying: Boolean = false,
    val error: String? = null,
    val lastLoadedDate: String = "",
    val lastLoadedCode: String = ""
)

class PlaybackViewModel : ViewModel() {

    private val TAG = "PlaybackViewModel"
    private val repo = ServiceLocator.repository

    private val _uiState = MutableStateFlow(PlaybackUiState())
    val uiState: StateFlow<PlaybackUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    fun initialize(deviceSerial: String, channelNo: Int = 1) {
        val today = LocalDate.now().toString()
        _uiState.update {
            it.copy(
                deviceSerial = deviceSerial,
                channelNo = channelNo,
                selectedDate = today,
                startTime = "$today 00:00:00",
                stopTime = "$today 23:59:59"
            )
        }
    }

    fun onDateSelected(date: String) {
        _uiState.update {
            it.copy(
                selectedDate = date,
                startTime = "$date 00:00:00",
                stopTime = "$date 23:59:59"
            )
        }
        loadPlayback()
    }

    fun onVerifyCodeChange(code: String) {
        val filtered = code.uppercase().filter { it in 'A'..'Z' || it in '0'..'9' }.take(6)
        _uiState.update { it.copy(verifyCode = filtered) }
        if (filtered.length == 6) {
            loadPlayback()
        }
    }

    fun loadPlayback() {
        // 幂等守卫：上一次加载仍在途时不重复发起（防快速重试/连续选日期并发请求）
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            val state = _uiState.value

            if (state.verifyCode.length != 6) {
                _uiState.update { it.copy(error = null, playbackUrl = null) }
                return@launch
            }

            if (state.selectedDate == state.lastLoadedDate &&
                state.verifyCode == state.lastLoadedCode &&
                state.playbackUrl != null
            ) {
                return@launch
            }

            _uiState.update {
                it.copy(isLoading = true, error = null, playbackUrl = null, useWebView = false)
            }

            try {
                when (val result = repo.getPlaybackAddress(
                    deviceSerial = state.deviceSerial,
                    startTime = state.startTime,
                    stopTime = state.stopTime,
                    code = state.verifyCode
                )) {
                    is NetworkResult.Success -> {
                        val rawUrl = result.data.url
                        if (rawUrl.isBlank()) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = "获取播放地址为空，请检查设备是否支持回放或验证码是否正确",
                                    lastLoadedDate = state.selectedDate,
                                    lastLoadedCode = state.verifyCode
                                )
                            }
                            return@launch
                        }

                        if (rawUrl.startsWith("ezopen://")) {
                            val token = ServiceLocator.tokenManager.getTokenForcefully()
                            val jssdkUrl = "https://open.ys7.com/console/jssdk/pc.html" +
                                "?accessToken=${token ?: ""}" +
                                "&url=${Uri.encode(rawUrl)}"
                            Log.d(TAG, "ezopen WebView 播放")
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    playbackUrl = jssdkUrl,
                                    useWebView = true,
                                    lastLoadedDate = state.selectedDate,
                                    lastLoadedCode = state.verifyCode
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    playbackUrl = rawUrl,
                                    lastLoadedDate = state.selectedDate,
                                    lastLoadedCode = state.verifyCode
                                )
                            }
                        }
                    }
                    is NetworkResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = result.message,
                                lastLoadedDate = state.selectedDate,
                                lastLoadedCode = state.verifyCode
                            )
                        }
                    }
                }
            } finally {
                // 兜底：任何路径（含 getTokenForcefully 抛异常）都保证 loading 消失，不卡加载态
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun retry() {
        _uiState.update { it.copy(lastLoadedCode = "", lastLoadedDate = "") }
        loadPlayback()
    }
}
