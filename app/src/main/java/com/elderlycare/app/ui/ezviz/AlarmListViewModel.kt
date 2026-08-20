package com.elderlycare.app.ui.ezviz

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elderlycare.app.data.ezviz.NetworkResult
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.ezviz.model.AlarmMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AlarmListUiState(
    val messages: List<AlarmMessage> = emptyList(),
    val groupedMessages: Map<String, List<AlarmMessage>> = emptyMap(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val unreadCount: Int = 0
)

/**
 * 告警列表 ViewModel（角色感知权限过滤）。
 *
 * 云端 getAlarmList 按 AppKey 账号全量返回、无 deviceSn 参数，因此本层按「当前用户可访问设备的
 * deviceSn 集合」过滤：家属 = 本人档案 deviceSn；社区/医院 = 本人 ACTIVE 绑定老人的 deviceSn。
 * REVOKED 解绑 → 授权集合实时缩小 → 对应告警立即从列表消失（Room Flow 自动失效）。
 */
class AlarmListViewModel : ViewModel() {

    private val TAG = "AlarmListViewModel"
    private val repo = ServiceLocator.repository
    private val bindingRepository = ServiceLocator.bindingRepository

    /** 已授权设备 SN 集合（角色感知，实时更新） */
    private val _authorizedSns = MutableStateFlow<Set<String>>(emptySet())

    /** 云端原始告警（null = 尚未加载），过滤只在此层派生，不污染云端数据 */
    private val _rawMessages = MutableStateFlow<List<AlarmMessage>?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _isRefreshing = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    private val _uiState = combine(
        _authorizedSns, _rawMessages, _isLoading, _isRefreshing, _error
    ) { sns, raw, loading, refreshing, error ->
        val filtered = raw?.filter { it.deviceSerial in sns } ?: emptyList()
        AlarmListUiState(
            messages = filtered,
            groupedMessages = filtered.groupBy { m -> m.timeGroup },
            isLoading = loading,
            isRefreshing = refreshing,
            error = error,
            unreadCount = filtered.count { m -> !m.isRead }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AlarmListUiState())

    val uiState: StateFlow<AlarmListUiState> = _uiState

    init {
        // 解析当前角色并订阅授权设备集合
        viewModelScope.launch {
            val familyUid = ServiceLocator.userStore.getCurrentUserId()
            if (familyUid != null) {
                // 家属：本人档案的 deviceSn
                ServiceLocator.profileStore.observeProfiles()
                    .map { profiles ->
                        profiles.filter { it.userId == familyUid }
                            .mapNotNull { it.deviceSn.takeIf { sn -> sn.isNotBlank() } }
                            .toSet()
                    }
                    .collect { _authorizedSns.value = it }
            } else {
                val staff = ServiceLocator.staffUserStore.getCurrentStaffUser()
                if (staff != null) {
                    // 社区/医院：本人 ACTIVE 绑定老人的 deviceSn（REVOKED 实时消失）
                    bindingRepository.observeAccessibleElderly(staff)
                        .map { list ->
                            list.mapNotNull { it.profile.deviceSn.takeIf { sn -> sn.isNotBlank() } }.toSet()
                        }
                        .collect { _authorizedSns.value = it }
                }
            }
        }
        loadMessages()
    }

    fun loadMessages() {
        viewModelScope.launch { doLoad() }
    }

    private suspend fun doLoad() {
        _isLoading.value = true
        _error.value = null
        when (val result = repo.getAlarmList(pageStart = 0, pageSize = 50)) {
            is NetworkResult.Success -> {
                _rawMessages.value = result.data
            }
            is NetworkResult.Error -> {
                Log.e(TAG, "获取报警列表失败: ${result.message}")
                _error.value = result.message
            }
        }
        _isLoading.value = false
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            doLoad()
            _isRefreshing.value = false
        }
    }

    fun markAsRead(alarmId: String) {
        viewModelScope.launch {
            when (repo.markAlarmRead(alarmId)) {
                is NetworkResult.Success -> {
                    _rawMessages.value = _rawMessages.value?.map {
                        if (it.alarmId == alarmId) it.copy(isRead = true) else it
                    }
                }
                is NetworkResult.Error -> { /* 静默失败 */ }
            }
        }
    }
}
