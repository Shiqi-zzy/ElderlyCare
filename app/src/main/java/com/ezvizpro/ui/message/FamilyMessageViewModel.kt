package com.ezvizpro.ui.message

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ezvizpro.core.local.FamilyMessage
import com.ezvizpro.core.local.HomeLocalStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FamilyMessageUiState(
    val messages: List<FamilyMessage> = emptyList()
)

@HiltViewModel
class FamilyMessageViewModel @Inject constructor(
    private val homeLocalStore: HomeLocalStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(FamilyMessageUiState())
    val uiState: StateFlow<FamilyMessageUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            homeLocalStore.messagesFlow.collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
    }

    fun sendMessage(content: String) {
        viewModelScope.launch {
            homeLocalStore.addMessage(FamilyMessage(content = content))
        }
    }

    fun deleteMessage(id: Long) {
        viewModelScope.launch {
            homeLocalStore.deleteMessage(id)
        }
    }
}
