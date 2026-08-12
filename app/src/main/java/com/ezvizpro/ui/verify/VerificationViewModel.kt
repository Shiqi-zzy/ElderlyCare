package com.ezvizpro.ui.verify

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ezvizpro.data.remote.ElderlyCareApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VerificationUiState(
    val status: String = "loading",  // loading / none / pending / approved / rejected
    val reviewNote: String? = null,
    val validUntil: String? = null,
    val submittedAt: String? = null,
    val isSubmitting: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class VerificationViewModel @Inject constructor(
    private val api: ElderlyCareApi
) : ViewModel() {

    private val _state = MutableStateFlow(VerificationUiState())
    val state: StateFlow<VerificationUiState> = _state.asStateFlow()

    private var authHeader: String = ""

    fun initialize(token: String) {
        authHeader = "Bearer $token"
        checkStatus()
    }

    fun checkStatus() {
        viewModelScope.launch {
            try {
                val resp = api.getVerificationStatus(authHeader)
                if (resp.isSuccessful && resp.body() != null) {
                    val body = resp.body()!!
                    _state.value = _state.value.copy(
                        status = body.qualificationStatus,
                        reviewNote = body.reviewNote,
                        validUntil = body.validUntil,
                        submittedAt = body.submittedAt
                    )
                } else {
                    _state.value = _state.value.copy(status = "none")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(status = "none", error = "检查认证状态失败: ${e.message}")
            }
        }
    }

    fun submitVerification(institutionName: String, role: String, documentUrls: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSubmitting = true, error = null)
            try {
                val resp = api.submitVerification(
                    authHeader,
                    com.ezvizpro.data.remote.dto.QualificationApplyRequest(
                        applicantUserId = "",  // backend fills from JWT
                        institutionName = institutionName,
                        institutionType = role,
                        documentUrls = documentUrls
                    )
                )
                if (resp.isSuccessful && resp.body() != null) {
                    val body = resp.body()!!
                    // 使用后端返回的真实状态（P1 AI自动放通 → approved）
                    _state.value = _state.value.copy(
                        isSubmitting = false,
                        status = body.status.ifEmpty { "pending" },
                        reviewNote = body.aiNote,
                        error = null
                    )
                } else {
                    _state.value = _state.value.copy(
                        isSubmitting = false,
                        error = "提交失败: ${resp.code()}"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isSubmitting = false,
                    error = "提交失败: ${e.message}"
                )
            }
        }
    }

    fun clearError() { _state.value = _state.value.copy(error = null) }
}
