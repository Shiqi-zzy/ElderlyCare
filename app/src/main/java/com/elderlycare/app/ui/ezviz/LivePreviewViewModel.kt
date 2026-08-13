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
                    val url = stream.getPreferredUrl()
                    if (url != null) {
                        currentStream = stream
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                streamUrl = url,
                                error = null,
                                showCodeInput = false
                            )
                        }
                        startAddressRefreshTimer()
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "无法获取播放地址，设备可能需要验证码",
                                showCodeInput = true
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
                            showCodeInput = msg.contains("验证码") || msg.contains("加密") || msg.contains("code", ignoreCase = true)
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
                val url = result.data.getPreferredUrl()
                if (url != null && url != state.streamUrl) {
                    currentStream = result.data
                    _uiState.update { it.copy(streamUrl = url, error = null) }
                }
            }
            is NetworkResult.Error -> Log.e(TAG, "刷新流地址失败: ${result.message}")
        }
    }

    fun toggleMute() {
        _uiState.update { it.copy(isMuted = !it.isMuted) }
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
