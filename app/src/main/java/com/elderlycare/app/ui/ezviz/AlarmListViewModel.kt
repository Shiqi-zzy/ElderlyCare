package com.elderlycare.app.ui.ezviz

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elderlycare.app.data.ezviz.NetworkResult
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.ezviz.model.AlarmMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AlarmListUiState(
    val messages: List<AlarmMessage> = emptyList(),
    val groupedMessages: Map<String, List<AlarmMessage>> = emptyMap(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val unreadCount: Int = 0
)

class AlarmListViewModel : ViewModel() {

    private val TAG = "AlarmListViewModel"
    private val repo = ServiceLocator.repository

    private val _uiState = MutableStateFlow(AlarmListUiState())
    val uiState: StateFlow<AlarmListUiState> = _uiState.asStateFlow()

    init {
        loadMessages()
    }

    fun loadMessages() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repo.getAlarmList(pageStart = 0, pageSize = 50)) {
                is NetworkResult.Success -> {
                    val messages = result.data
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            messages = messages,
                            groupedMessages = messages.groupBy { m -> m.timeGroup },
                            unreadCount = messages.count { m -> !m.isRead }
                        )
                    }
                }
                is NetworkResult.Error -> {
                    Log.e(TAG, "获取报警列表失败: ${result.message}")
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            loadMessages()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun markAsRead(alarmId: String) {
        viewModelScope.launch {
            when (repo.markAlarmRead(alarmId)) {
                is NetworkResult.Success -> {
                    _uiState.update { state ->
                        val updated = state.messages.map {
                            if (it.alarmId == alarmId) it.copy(isRead = true) else it
                        }
                        state.copy(
                            messages = updated,
                            groupedMessages = updated.groupBy { m -> m.timeGroup },
                            unreadCount = updated.count { m -> !m.isRead }
                        )
                    }
                }
                is NetworkResult.Error -> { /* 静默失败 */ }
            }
        }
    }
}
