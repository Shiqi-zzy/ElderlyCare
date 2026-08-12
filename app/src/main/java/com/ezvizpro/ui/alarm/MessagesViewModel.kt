package com.ezvizpro.ui.alarm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ezvizpro.core.util.NetworkResult
import com.ezvizpro.domain.model.AlarmMessage
import com.ezvizpro.domain.repository.AlarmRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class MessagesUiState(
    val messages: List<AlarmMessage> = emptyList(),
    val groupedMessages: Map<String, List<AlarmMessage>> = emptyMap(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val unreadCount: Int = 0
)

@HiltViewModel
class MessagesViewModel @Inject constructor(
    private val alarmRepository: AlarmRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessagesUiState())
    val uiState: StateFlow<MessagesUiState> = _uiState.asStateFlow()

    init {
        loadMessages()
    }

    fun loadMessages() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = alarmRepository.getAlarmList(pageStart = 0, pageSize = 50)) {
                is NetworkResult.Success -> {
                    val messages = result.data
                    val grouped = messages.groupBy { it.timeGroup }
                    val unread = messages.count { !it.isRead }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            messages = messages,
                            groupedMessages = grouped,
                            unreadCount = unread
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = result.message)
                    }
                    Timber.e("获取报警列表失败: ${result.message}")
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
            when (alarmRepository.markAsRead(alarmId)) {
                is NetworkResult.Success -> {
                    _uiState.update { state ->
                        val updated = state.messages.map {
                            if (it.alarmId == alarmId) it.copy(isRead = true) else it
                        }
                        state.copy(
                            messages = updated,
                            groupedMessages = updated.groupBy { it.timeGroup },
                            unreadCount = updated.count { !it.isRead }
                        )
                    }
                }
                is NetworkResult.Error -> { /* 静默失败 */ }
            }
        }
    }
}
