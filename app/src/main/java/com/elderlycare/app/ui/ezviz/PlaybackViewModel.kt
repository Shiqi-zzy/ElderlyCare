package com.elderlycare.app.ui.ezviz

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elderlycare.app.data.ezviz.NetworkResult
import com.elderlycare.app.data.ezviz.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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

    fun initialize(deviceSerial: String, channelNo: Int = 1, startAtTime: String = "") {
        // startAtTime（告警发生时间 yyyy-MM-dd HH:mm:ss）：以其为中心取 ±30 秒回放窗口，
        // 让播放器尽量定位到告警发生的时间戳；未传或解析失败则默认今天全天。
        val window = alarmWindow(startAtTime) ?: run {
            val today = LocalDate.now().toString()
            Triple(today, "$today 00:00:00", "$today 23:59:59")
        }
        _uiState.update {
            it.copy(
                deviceSerial = deviceSerial,
                channelNo = channelNo,
                selectedDate = window.first,
                startTime = window.second,
                stopTime = window.third
            )
        }
    }

    /** 解析告警时间，返回 (回放日期, 开始时间, 结束时间)；格式非法时返回 null。 */
    private fun alarmWindow(alarmTime: String): Triple<String, String, String>? = try {
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val t = LocalDateTime.parse(alarmTime.trim(), fmt)
        Triple(
            t.toLocalDate().toString(),
            t.minusSeconds(30).format(fmt),
            t.plusSeconds(30).format(fmt)
        )
    } catch (e: Exception) {
        null
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
        viewModelScope.launch {
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
                        val localUrl = LocalEzPlayerUrl.build(
                            mode = "rec",
                            deviceSerial = state.deviceSerial,
                            channelNo = state.channelNo,
                            accessToken = token ?: "",
                            rawUrl = rawUrl,
                            beginSec = LocalEzPlayerUrl.toEpochSeconds(state.startTime),
                            endSec = LocalEzPlayerUrl.toEpochSeconds(state.stopTime)
                        )
                        Log.d(TAG, "ezopen 本地 EZUIKit WebView 回放")
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                playbackUrl = localUrl,
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
                            error = FriendlyEzError.message(result.message),
                            lastLoadedDate = state.selectedDate,
                            lastLoadedCode = state.verifyCode
                        )
                    }
                }
            }
        }
    }

    fun retry() {
        _uiState.update { it.copy(lastLoadedCode = "", lastLoadedDate = "") }
        loadPlayback()
    }
}
