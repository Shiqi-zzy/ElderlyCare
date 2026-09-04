package com.elderlycare.app.ui.ezviz

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.elderlycare.app.data.ezviz.CaptureItem
import com.elderlycare.app.data.ezviz.NetworkResult
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.ezviz.model.AlarmMessage
import com.elderlycare.app.data.ezviz.model.AlarmType
import com.elderlycare.app.ui.shared.AuthorizedSnsProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

data class AllCapturesUiState(
    val deviceSerial: String = "",
    val deviceBound: Boolean = false,
    /** 抓拍记录（SDK 萤石云告警抓拍，新→旧） */
    val items: List<CaptureItem> = emptyList(),
    /** 未读数（isRead=false 计数） */
    val unreadCount: Int = 0,
    val isLoading: Boolean = false,
    /** 列表加载失败（列表为空时展示网络异常占位图，有数据则保留旧列表） */
    val loadFailed: Boolean = false,
)

/**
 * 全部抓拍页 VM（方案 A：SDK 萤石云直连）。
 *
 * 数据源 = 萤石云告警列表 api/lapp/alarm/list（与消息中心同一通道，公网直连，
 * 不依赖本地后端/局域网）；按授权设备 SN 集合（AuthorizedSnsProvider，角色感知）过滤；
 * SDK 返回的 alarmPicUrl 即真实抓拍图（设备人形/移动侦测触发时自动抓拍）。
 * 手动抓拍（预览页截图）暂不在此页展示。
 */
class AllCapturesViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "AllCapturesViewModel"
    private val repo = ServiceLocator.repository

    /** 已授权设备 SN 集合（角色感知，实时更新） */
    private val _authorizedSns = MutableStateFlow<Set<String>>(emptySet())

    /** 当前设备 SN（用于绑定空态判断） */
    private val _deviceSerial = MutableStateFlow("")
    private val _isLoading = MutableStateFlow(false)
    private val _loadFailed = MutableStateFlow(false)
    private val _rawAlarms = MutableStateFlow<List<AlarmMessage>?>(null)

    private val _uiState = combine(
        _authorizedSns, _deviceSerial, _rawAlarms, _isLoading, _loadFailed
    ) { sns, sn, raw, loading, failed ->
        val items = raw
            ?.filter { it.deviceSerial in sns }
            ?.map { it.toCaptureItem() }
            ?: emptyList()
        AllCapturesUiState(
            deviceSerial = sn,
            deviceBound = sns.isNotEmpty(),
            items = items,
            unreadCount = items.count { !it.isRead },
            isLoading = loading,
            loadFailed = failed && items.isEmpty()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AllCapturesUiState())

    val uiState: StateFlow<AllCapturesUiState> = _uiState

    init {
        // 授权设备集合（角色感知，与告警消息共用逻辑）
        viewModelScope.launch {
            AuthorizedSnsProvider.flow().collect { _authorizedSns.value = it }
        }
        // 当前设备 SN（未绑定显示空态）
        viewModelScope.launch {
            ServiceLocator.bindingRepository.observeCurrentUserDevice().collect { device ->
                _deviceSerial.value = device?.deviceSn.orEmpty()
            }
        }
        refresh()
    }

    /** 拉取 SDK 萤石云告警抓拍列表（进页兜底） */
    fun refresh() {
        viewModelScope.launch { doLoad() }
    }

    private suspend fun doLoad() {
        _isLoading.value = true
        _loadFailed.value = false
        when (val result = repo.getAlarmList(pageStart = 0, pageSize = 50)) {
            is NetworkResult.Success -> {
                _rawAlarms.value = result.data
            }
            is NetworkResult.Error -> {
                Log.e(TAG, "获取抓拍列表失败: ${result.message}")
                _loadFailed.value = true
            }
        }
        _isLoading.value = false
    }

    /** 点击条目 → 云端标记该告警已读 */
    fun markRead(item: CaptureItem) {
        if (item.isRead || item.recordId.isBlank()) return
        viewModelScope.launch {
            when (repo.markAlarmRead(item.recordId)) {
                is NetworkResult.Success -> {
                    _rawAlarms.value = _rawAlarms.value?.map {
                        if (it.alarmId == item.recordId) it.copy(isRead = true) else it
                    }
                }
                is NetworkResult.Error -> { /* 静默失败 */ }
            }
        }
    }

    /** AlarmMessage（SDK 萤石云告警）→ CaptureItem（抓拍列表条目） */
    private fun AlarmMessage.toCaptureItem(): CaptureItem {
        val type = AlarmType.fromCode(alarmType)
        return CaptureItem(
            recordId = alarmId,
            deviceSerial = deviceSerial,
            captureType = "auto",
            alarmName = type.label.ifBlank { alarmName },
            eventTime = parseAlarmTime(alarmTime),
            picUrl = alarmPicUrl ?: "",
            localPicUrl = "",
            isRead = isRead
        )
    }

    /** 告警时间字符串 → 毫秒时间戳（解析失败兜底当前时间） */
    private fun parseAlarmTime(alarmTime: String): Long {
        if (alarmTime.isBlank()) return System.currentTimeMillis()
        return try {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).parse(alarmTime)?.time
                ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}
