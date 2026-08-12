package com.ezvizpro.ui.playback

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ezvizpro.core.local.TokenManager
import com.ezvizpro.core.util.NetworkResult
import com.ezvizpro.domain.repository.PlaybackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import javax.inject.Inject

data class PlaybackUiState(
    val deviceSerial: String = "",
    val channelNo: Int = 1,
    val selectedDate: String = "",         // yyyy-MM-dd
    val startTime: String = "",            // yyyy-MM-dd HH:mm:ss
    val stopTime: String = "",             // yyyy-MM-dd HH:mm:ss
    val verifyCode: String = "",           // 设备验证码（6位大写字母）
    val playbackUrl: String? = null,
    val useWebView: Boolean = false,       // ezopen 协议需要 WebView 播放
    val isLoading: Boolean = false,
    val isPlaying: Boolean = false,
    val error: String? = null,
    val lastLoadedDate: String = "",       // 上次加载的日期，避免重复加载
    val lastLoadedCode: String = ""        // 上次加载的验证码，避免重复加载
)

@HiltViewModel
class PlaybackViewModel @Inject constructor(
    private val playbackRepository: PlaybackRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaybackUiState())
    val uiState: StateFlow<PlaybackUiState> = _uiState.asStateFlow()

    fun initialize(deviceSerial: String, channelNo: Int) {
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
        // 不自动加载，等用户输入验证码
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
        // 小写自动转大写，只保留大写字母和数字，最多6位
        val filtered = code.uppercase().filter { it in 'A'..'Z' || it in '0'..'9' }.take(6)
        _uiState.update { it.copy(verifyCode = filtered) }

        // 输入满6位自动加载
        if (filtered.length == 6) {
            loadPlayback()
        }
    }

    fun loadPlayback() {
        viewModelScope.launch {
            val state = _uiState.value

            // 必须有验证码才加载（加密设备必填）
            if (state.verifyCode.length != 6) {
                _uiState.update { it.copy(error = null, playbackUrl = null) }
                return@launch
            }

            // 避免重复加载相同参数
            if (state.selectedDate == state.lastLoadedDate && state.verifyCode == state.lastLoadedCode && state.playbackUrl != null) {
                return@launch
            }

            _uiState.update {
                it.copy(isLoading = true, error = null, playbackUrl = null, useWebView = false)
            }

            // 有验证码 → ezopen 协议（code 仅对该协议生效）
            val protocol = 1  // ezopen

            when (val result = playbackRepository.getPlaybackAddress(
                deviceSerial = state.deviceSerial,
                channelNo = state.channelNo,
                startTime = state.startTime,
                stopTime = state.stopTime,
                code = state.verifyCode,
                protocol = protocol
            )) {
                is NetworkResult.Success -> {
                    val rawUrl = result.data.url
                    if (rawUrl.isBlank()) {
                        Timber.e("API 返回了空的播放地址")
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
                    Timber.d("回放地址获取成功, 原始URL前30字符: ${rawUrl.take(30)}")

                    // ezopen 协议需要萤石 JSSDK 网页播放器
                    if (rawUrl.startsWith("ezopen://")) {
                        val token = tokenManager.getTokenForcefully()
                        val jssdkUrl = "https://open.ys7.com/console/jssdk/pc.html" +
                            "?accessToken=${token ?: ""}" +
                            "&url=${Uri.encode(rawUrl)}"
                        Timber.d("ezopen WebView 播放, JSSDK URL: ${jssdkUrl.take(80)}...")
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
                        Timber.d("非ezopen协议, 使用ExoPlayer播放: $rawUrl")
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
                    Timber.e("获取回放地址失败: ${result.message}")
                }
            }
        }
    }

    fun retry() {
        _uiState.update { it.copy(lastLoadedCode = "", lastLoadedDate = "") }
        loadPlayback()
    }
}
