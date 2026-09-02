package com.elderlycare.app.ui.ezviz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elderlycare.app.data.ezviz.NetworkResult
import com.elderlycare.app.data.ezviz.Rk3MediaMock
import com.elderlycare.app.data.ezviz.Rk3MediaRepository
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.ezviz.model.AudioCategory
import com.elderlycare.app.data.ezviz.model.AudioTrack
import com.elderlycare.app.data.ezviz.model.MediaPlayState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class Rk3PlayUiState(
    val deviceSerial: String = "",
    val selectedCategory: AudioCategory = AudioCategory.RECOMMEND,
    val tracks: List<AudioTrack> = emptyList(),
    /** 当前播放项（null = 未播放） */
    val currentTrack: AudioTrack? = null,
    val playState: MediaPlayState = MediaPlayState.IDLE,
    val mockEnabled: Boolean = true,
    val showPremiumDialog: Boolean = false,
    val showBindDialog: Boolean = false
)

/**
 * RK3 点播页 ViewModel。
 *
 * 播放状态同步：正常链路依靠萤石 webhook 回调（Rk3MediaStateHub）；
 * Mock 模式（演示答辩）本地模拟状态机切换。
 * 网络层：Rk3MediaRepository 全部占位（待萤石商务开通权限后实现）。
 */
class Rk3PlayViewModel : ViewModel() {

    private val repo = Rk3MediaRepository

    private val _uiState = MutableStateFlow(Rk3PlayUiState())
    val uiState: StateFlow<Rk3PlayUiState> = _uiState.asStateFlow()

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
        loadTracks()
        observeWebhookState()
    }

    /** 切换分类 Tab → 重新加载列表 */
    fun selectCategory(category: AudioCategory) {
        if (category == _uiState.value.selectedCategory) return
        _uiState.update { it.copy(selectedCategory = category) }
        loadTracks()
    }

    fun setMockEnabled(enabled: Boolean) {
        ServiceLocator.settingsStore.setRk3MediaMockEnabled(enabled)
        _uiState.update {
            it.copy(mockEnabled = enabled, currentTrack = null, playState = MediaPlayState.IDLE)
        }
    }

    /** 点击音频卡片：Mock 本地切换状态；非 Mock 走占位接口（失败 toast） */
    fun onTrackClick(track: AudioTrack) {
        val state = _uiState.value
        // ①设备校验：未绑定 RK3 → 弹窗提示
        if (state.deviceSerial.isBlank()) {
            _uiState.update { it.copy(showBindDialog = true) }
            return
        }
        // ②Mock 演示分支
        if (state.mockEnabled) {
            if (track.premium) {
                // 模拟「部分音频需要智控畅享服务」权限拦截
                _uiState.update { it.copy(showPremiumDialog = true) }
                return
            }
            _uiState.update {
                it.copy(currentTrack = track, playState = MediaPlayState.PLAYING)
            }
            return
        }
        // ③真实分支：占位接口（待萤石内部文档后实现真实请求）
        viewModelScope.launch {
            when (repo.createPlay(state.deviceSerial, track.contentId)) {
                is NetworkResult.Success -> Unit // 成功状态由 webhook 回调驱动
                is NetworkResult.Error -> _toastMessage.value = "设备离线或不可达，请稍后重试"
            }
        }
    }

    /** 播放/暂停切换（PLAYING↔PAUSED）；未播放时按恢复播放 */
    fun onPlayPauseClick() {
        val state = _uiState.value
        if (state.mockEnabled) {
            val next = when (state.playState) {
                MediaPlayState.PLAYING -> MediaPlayState.PAUSED
                MediaPlayState.PAUSED -> MediaPlayState.PLAYING
                MediaPlayState.IDLE -> if (state.currentTrack != null) MediaPlayState.PLAYING else MediaPlayState.IDLE
            }
            _uiState.update { it.copy(playState = next) }
            return
        }
        // 真实分支：占位接口
        viewModelScope.launch {
            val serial = state.deviceSerial
            val result = when (state.playState) {
                MediaPlayState.PLAYING -> repo.pausePlay(serial)
                MediaPlayState.PAUSED -> repo.createPlay(serial, state.currentTrack?.contentId.orEmpty())
                MediaPlayState.IDLE -> return@launch
            }
            if (result is NetworkResult.Error) _toastMessage.value = "设备离线或不可达，请稍后重试"
        }
    }

    fun onStopClick() {
        val state = _uiState.value
        if (state.mockEnabled) {
            _uiState.update { it.copy(currentTrack = null, playState = MediaPlayState.IDLE) }
            return
        }
        viewModelScope.launch {
            if (repo.stopPlay(state.deviceSerial) is NetworkResult.Error) {
                _toastMessage.value = "设备离线或不可达，请稍后重试"
            }
        }
    }

    fun dismissPremiumDialog() {
        _uiState.update { it.copy(showPremiumDialog = false) }
    }

    fun dismissBindDialog() {
        _uiState.update { it.copy(showBindDialog = false) }
    }

    private fun loadTracks() {
        val state = _uiState.value
        if (state.mockEnabled) {
            _uiState.update {
                it.copy(tracks = Rk3MediaMock.audioTracks[state.selectedCategory].orEmpty())
            }
            return
        }
        // 真实分支：占位接口（资源列表接口待萤石内部文档确认）
        viewModelScope.launch {
            when (repo.getAudioList(state.selectedCategory)) {
                is NetworkResult.Success -> Unit // 待实现：Success 数据刷列表
                is NetworkResult.Error -> _toastMessage.value = "该功能接口待开通，敬请期待"
            }
        }
    }

    /**
     * webhook 播放状态同步（占位挂点）。
     * //【待萤石商务开通权限，拿到官方抓包报文/内部PDF文档，再实现真实请求】
     * 未来：Rk3MediaStateHub.state 非空时按 deviceSerial 匹配刷新 playState/currentTrack，
     * 不再依赖本地 Mock 状态机。
     */
    private fun observeWebhookState() {
        // TODO(萤石内部文档): collect(Rk3MediaStateHub.state) → 更新 _uiState.playState
    }
}
