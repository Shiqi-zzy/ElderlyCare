package com.ezvizpro.ui.reminder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ezvizpro.core.local.HomeLocalStore
import com.ezvizpro.core.local.LifeReminder
import com.ezvizpro.core.local.ReminderType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LifeReminderUiState(
    val reminders: List<LifeReminder> = emptyList()
)

@HiltViewModel
class LifeReminderViewModel @Inject constructor(
    private val homeLocalStore: HomeLocalStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(LifeReminderUiState())
    val uiState: StateFlow<LifeReminderUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            homeLocalStore.remindersFlow.collect { reminders ->
                _uiState.update { it.copy(reminders = reminders) }
            }
        }
    }

    fun addReminder(title: String, type: ReminderType) {
        viewModelScope.launch {
            homeLocalStore.addReminder(LifeReminder(title = title, type = type))
        }
    }

    fun toggleReminder(reminder: LifeReminder) {
        viewModelScope.launch {
            homeLocalStore.updateReminder(reminder.copy(enabled = !reminder.enabled))
        }
    }

    fun deleteReminder(id: Long) {
        viewModelScope.launch {
            homeLocalStore.deleteReminder(id)
        }
    }
}
