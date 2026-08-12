package com.ezvizpro.ui.live

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ezvizpro.core.player.EzvizPlayer
import com.ezvizpro.core.player.PlayerState
import com.ezvizpro.core.util.NetworkResult
import com.ezvizpro.domain.model.*
import com.ezvizpro.domain.usecase.ControlPTZUseCase
import com.ezvizpro.domain.usecase.GetLiveAddressUseCase
import com.ezvizpro.domain.repository.LiveRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class LivePreviewUiState(
    val deviceSerial: String = "",
    val channelNo: Int = 1,
    val streamUrl: String? = null,
    val playerState: PlayerState = PlayerState.Idle,
    val isMuted: Boolean = false,
    val isPtzEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val presets: List<Preset> = emptyList(),
    val deviceDetail: Device? = null,
    // 设备验证码（加密设备需要，6位大写字母）
    val verifyCode: String = "",
    val showCodeInput: Boolean = true  // 默认显示验证码输入，加密设备必填
)

@HiltViewModel
class LivePreviewViewModel @Inject constructor(
    private val getLiveAddressUseCase: GetLiveAddressUseCase,
    private val controlPTZUseCase: ControlPTZUseCase,
    private val liveRepository: LiveRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LivePreviewUiState())
    val uiState: StateFlow<LivePreviewUiState> = _uiState.asStateFlow()

    private var addressRefreshJob: Job? = null
    private var currentStream: LiveStream? = null

    fun initialize(deviceSerial: String, channelNo: Int, supportPtz: Boolean = false) {
        _uiState.update {
            it.copy(
                deviceSerial = deviceSerial,
                channelNo = channelNo,
                isPtzEnabled = supportPtz
            )
        }
        startPlay()
    }

    fun setVerifyCode(code: String) {
        _uiState.update { it.copy(verifyCode = code.take(6)) }
    }

    fun toggleCodeInput() {
        _uiState.update { it.copy(showCodeInput = !it.showCodeInput) }
    }

    fun bindPlayer(player: EzvizPlayer) {
        player.setOnStateChangeListener { state ->
            _uiState.update { it.copy(playerState = state) }

            // 播放出错时尝试重新获取流地址
            if (state is PlayerState.Error) {
                Timber.e("播放错误: ${state.message}，尝试重新获取流地址")
                viewModelScope.launch {
                    refreshStreamAddress()
                }
            }
        }
    }

    private fun startPlay() {
        viewModelScope.launch {
            val state = _uiState.value
            val code = state.verifyCode.takeIf { it.length == 6 }
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = getLiveAddressUseCase(state.deviceSerial, state.channelNo, code)) {
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
                            // 如果错误提示需要验证码，显示输入框
                            showCodeInput = msg.contains("验证码") || msg.contains("加密") || msg.contains("code")
                        )
                    }
                }
            }
        }
    }

    /**
     * 定时刷新流地址（每 2 分钟）
     */
    private fun startAddressRefreshTimer() {
        addressRefreshJob?.cancel()
        addressRefreshJob = viewModelScope.launch {
            while (isActive) {
                delay(2 * 60 * 1000L) // 2 分钟
                Timber.d("定时刷新流地址...")
                refreshStreamAddress()
            }
        }
    }

    private suspend fun refreshStreamAddress() {
        val state = _uiState.value
        val code = state.verifyCode.takeIf { it.length == 6 }
        when (val result = getLiveAddressUseCase(state.deviceSerial, state.channelNo, code)) {
            is NetworkResult.Success -> {
                val stream = result.data
                val url = stream.getPreferredUrl()
                if (url != null && url != state.streamUrl) {
                    currentStream = stream
                    _uiState.update { it.copy(streamUrl = url, error = null) }
                }
            }
            is NetworkResult.Error -> {
                Timber.e("刷新流地址失败: ${result.message}")
            }
        }
    }

    // ==================== 云台控制 ====================

    fun onPtzStart(direction: PtzDirection, speed: PtzSpeed = PtzSpeed.NORMAL) {
        val state = _uiState.value
        viewModelScope.launch {
            controlPTZUseCase.start(
                deviceSerial = state.deviceSerial,
                channelNo = state.channelNo,
                direction = direction,
                speed = speed
            )
        }
    }

    fun onPtzStop(direction: PtzDirection? = null) {
        val state = _uiState.value
        viewModelScope.launch {
            controlPTZUseCase.stop(
                deviceSerial = state.deviceSerial,
                channelNo = state.channelNo,
                direction = direction
            )
        }
    }

    // ==================== 预置位 ====================

    fun loadPresets() {
        val state = _uiState.value
        viewModelScope.launch {
            when (val result = liveRepository.getPresetList(
                deviceSerial = state.deviceSerial,
                channelNo = state.channelNo
            )) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(presets = result.data) }
                }
                is NetworkResult.Error -> {
                    Timber.e("获取预置位失败: ${result.message}")
                }
            }
        }
    }

    fun moveToPreset(presetIndex: Int) {
        val state = _uiState.value
        viewModelScope.launch {
            liveRepository.moveToPreset(
                deviceSerial = state.deviceSerial,
                channelNo = state.channelNo,
                presetIndex = presetIndex
            )
        }
    }

    fun addPreset(name: String) {
        val state = _uiState.value
        viewModelScope.launch {
            when (val result = liveRepository.addPreset(
                deviceSerial = state.deviceSerial,
                channelNo = state.channelNo,
                presetName = name
            )) {
                is NetworkResult.Success -> {
                    loadPresets() // 重新加载列表
                }
                is NetworkResult.Error -> {
                    Timber.e("添加预置位失败: ${result.message}")
                }
            }
        }
    }

    // ==================== 其他 ====================

    fun toggleMute() {
        _uiState.update { it.copy(isMuted = !it.isMuted) }
    }

    /**
     * 页面退出时关闭直播
     */
    fun closeLive() {
        addressRefreshJob?.cancel()
        val state = _uiState.value
        viewModelScope.launch {
            liveRepository.closeLive(
                deviceSerial = state.deviceSerial,
                channelNo = state.channelNo
            )
        }
    }

    fun retry() {
        startPlay()
    }
}
