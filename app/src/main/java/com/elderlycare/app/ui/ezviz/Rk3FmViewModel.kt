package com.elderlycare.app.ui.ezviz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elderlycare.app.data.ezviz.NetworkResult
import com.elderlycare.app.data.ezviz.Rk3MediaMock
import com.elderlycare.app.data.ezviz.Rk3MediaRepository
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.ezviz.model.FmGroup
import com.elderlycare.app.data.ezviz.model.FmStation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class Rk3FmUiState(
    val deviceSerial: String = "",
    val stations: Map<FmGroup, List<FmStation>> = emptyMap(),
    /** 当前收听电台（null = 未播放） */
    val currentStation: FmStation? = null,
    val isPlaying: Boolean = false,
    val mockEnabled: Boolean = true,
    val showBindDialog: Boolean = false
)

/**
 * RK3 广播FM页 ViewModel。
 *
 * 限制：第三方开放 API 不支持自定义外部电台 URL，只使用平台内置电台 ID
 * （POST /v2/device/fm/create，入参待萤石内部文档后实现）。
 * 播放状态正常链路靠 webhook 回调；Mock 模式本地模拟。
 */
class Rk3FmViewModel : ViewModel() {

    private val repo = Rk3MediaRepository

    private val _uiState = MutableStateFlow(Rk3FmUiState())
    val uiState: StateFlow<Rk3FmUiState> = _uiState.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    fun consumeToast() {
        _toastMessage.value = null
    }

    fun initialize(deviceSerial: String) {
        _uiState.update {
            it.copy(
                deviceSerial = deviceSerial,
                mockEnabled = ServiceLocator.settingsStore.isRk3MediaMockEnabled()
            )
        }
        loadStations()
    }

    fun setMockEnabled(enabled: Boolean) {
        ServiceLocator.settingsStore.setRk3MediaMockEnabled(enabled)
        _uiState.update { it.copy(mockEnabled = enabled, currentStation = null, isPlaying = false) }
        loadStations()
    }

    /** 点击电台：Mock 直接开播；非 Mock 走占位接口（失败 toast） */
    fun onStationClick(station: FmStation) {
        val state = _uiState.value
        // ①设备校验：未绑定 RK3 → 弹窗提示
        if (state.deviceSerial.isBlank()) {
            _uiState.update { it.copy(showBindDialog = true) }
            return
        }
        // ②Mock 演示分支
        if (state.mockEnabled) {
            _uiState.update { it.copy(currentStation = station, isPlaying = true) }
            return
        }
        // ③真实分支：占位接口（待萤石内部文档后实现真实请求）
        viewModelScope.launch {
            when (repo.createFm(state.deviceSerial, station.fmId)) {
                is NetworkResult.Success -> Unit // 成功状态由 webhook 回调驱动
                is NetworkResult.Error -> _toastMessage.value = "设备离线或不可达，请稍后重试"
            }
        }
    }

    /** 停止播放（状态栏按钮） */
    fun onStopClick() {
        val state = _uiState.value
        if (state.mockEnabled) {
            _uiState.update { it.copy(currentStation = null, isPlaying = false) }
            return
        }
        viewModelScope.launch {
            if (repo.stopFm(state.deviceSerial) is NetworkResult.Error) {
                _toastMessage.value = "设备离线或不可达，请稍后重试"
            }
        }
    }

    fun dismissBindDialog() {
        _uiState.update { it.copy(showBindDialog = false) }
    }

    private fun loadStations() {
        val state = _uiState.value
        if (state.mockEnabled) {
            _uiState.update { it.copy(stations = Rk3MediaMock.fmStations) }
            return
        }
        // 真实分支：占位接口（电台列表接口待萤石内部文档确认）
        viewModelScope.launch {
            when (repo.getFmList(FmGroup.RECOMMEND)) {
                is NetworkResult.Success -> Unit // 待实现：Success 数据刷列表
                is NetworkResult.Error -> _toastMessage.value = "该功能接口待开通，敬请期待"
            }
        }
    }
}
