package com.elderlycare.app.ui.ezviz

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.elderlycare.app.data.ezviz.CaptureItem
import com.elderlycare.app.data.ezviz.RtcSignalingManager
import com.elderlycare.app.data.ezviz.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AllCapturesUiState(
    val deviceSerial: String = "",
    val deviceBound: Boolean = false,
    /** 抓拍记录（manual 手动 + auto 设备告警自动，新→旧） */
    val items: List<CaptureItem> = emptyList(),
    /** 未读数（is_read=0 计数；全部抓拍页独立角标，与消息 Tab 角标互不干扰） */
    val unreadCount: Int = 0,
    val isLoading: Boolean = false,
    /** 列表加载失败（列表为空时展示网络异常占位图，有数据则保留旧列表） */
    val loadFailed: Boolean = false,
    /** 手动抓拍请求进行中（FAB 防连点；同设备 4s 限流由后端保证） */
    val isCapturing: Boolean = false,
    /** 一次性 toast 文案（Screen 消费后调用 [consumeToast] 清除） */
    val toastMessage: String? = null,
)

/**
 * 全部抓拍页 VM：数据源=后端 SQLite alarm_events（图片只存后端，
 * App 本地 Room 不落图片）。授权链路当前设备 → 拉列表；WS 新告警/图片就绪
 * （captureFeed）→ 实时刷新；点击条目 → 后端标已读。
 */
class AllCapturesViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AllCapturesUiState())
    val uiState: StateFlow<AllCapturesUiState> = _uiState.asStateFlow()

    init {
        // 授权链路当前设备（切换设备自动过滤；未绑定显示空态）
        viewModelScope.launch {
            ServiceLocator.bindingRepository.observeCurrentUserDevice().collect { device ->
                _uiState.update {
                    it.copy(
                        deviceSerial = device?.deviceSn.orEmpty(),
                        deviceBound = device?.deviceBound == true
                    )
                }
                refresh()
            }
        }
        // WS 实时刷新：新告警落库（alarm）与告警图片就绪（captureUpdated）
        viewModelScope.launch {
            RtcSignalingManager.captureFeed.collect { refresh() }
        }
    }

    /** 拉取全部抓拍列表（进页兜底 + WS 信号触发的实时刷新共用） */
    fun refresh() {
        val sn = _uiState.value.deviceSerial
        if (sn.isBlank()) {
            _uiState.update {
                it.copy(items = emptyList(), unreadCount = 0, isLoading = false, loadFailed = false)
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            ServiceLocator.captureRepository.fetchCaptures(sn)
                .onSuccess { data ->
                    _uiState.update {
                        it.copy(
                            items = data.list,
                            unreadCount = data.list.count { c -> !c.isRead },
                            isLoading = false,
                            loadFailed = false
                        )
                    }
                }
                .onFailure { _uiState.update { it.copy(isLoading = false, loadFailed = true) } }
        }
    }

    /** 点击条目 → 后端标该条已读（防串读由后端限定设备保证） */
    fun markRead(item: CaptureItem) {
        if (item.isRead || item.recordId.isBlank()) return
        viewModelScope.launch {
            ServiceLocator.captureRepository.markRead(item.recordId, item.deviceSerial)
                .onSuccess { refresh() }
        }
    }

    /** FAB 手动抓拍：App → 后端 → 萤石 device/capture → 下载存相册（4s 限流走后端） */
    fun manualCapture() {
        val state = _uiState.value
        val sn = state.deviceSerial
        if (sn.isBlank() || state.isCapturing) return
        _uiState.update { it.copy(isCapturing = true) }
        viewModelScope.launch {
            ServiceLocator.captureRepository.capture(sn)
                .onSuccess { msg -> _uiState.update { it.copy(toastMessage = msg) } }
                .onFailure { e ->
                    _uiState.update { it.copy(toastMessage = e.message ?: "抓拍失败，请重试") }
                }
            _uiState.update { it.copy(isCapturing = false) }
            refresh() // 手动抓拍成功即落库 manual 记录，立即刷新列表
        }
    }

    fun consumeToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }
}
