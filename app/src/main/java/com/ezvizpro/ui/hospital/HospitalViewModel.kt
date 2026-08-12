package com.ezvizpro.ui.hospital

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ezvizpro.data.remote.ElderlyCareApi
import com.ezvizpro.data.remote.dto.*
import com.ezvizpro.data.repository.ElderlyCareRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HospitalUiState(
    val boundElderlyCount: Int = 0,
    val elderlyList: List<ElderlyDto> = emptyList(),
    val authRequests: List<AuthorizationDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val toastMessage: String? = null,
    // 设备管理
    val boundDevices: List<DeviceDto> = emptyList(),
    val selectedDeviceElderlyId: String = "",
    // 健康档案（Phase 3）
    val healthRecords: List<HealthRecordDto> = emptyList(),
    val selectedHealthElderlyId: String = "",
    // 急救权限（Phase 3）
    val emergencyStatus: EmergencyStatusResponse? = null,
    val activeEmergencies: List<EmergencyStatusResponse> = emptyList()
)

@HiltViewModel
class HospitalViewModel @Inject constructor(
    private val api: ElderlyCareApi,
    private val repository: ElderlyCareRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HospitalUiState())
    val state: StateFlow<HospitalUiState> = _state.asStateFlow()

    private var token: String = ""

    fun setToken(t: String) { token = t; loadDashboard() }

    fun loadDashboard() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val auth = "Bearer $token"
                val dashResp = api.getHospitalDashboard(auth)
                if (dashResp.isSuccessful) {
                    _state.value = _state.value.copy(boundElderlyCount = dashResp.body()?.boundElderlyCount ?: 0)
                }
                val listResp = api.getHospitalElderlyList(auth)
                if (listResp.isSuccessful) {
                    _state.value = _state.value.copy(elderlyList = listResp.body()?.items ?: emptyList())
                }
                _state.value = _state.value.copy(isLoading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun loadAuthRequests() {
        viewModelScope.launch {
            try {
                val resp = api.getHospitalAuthorizationRequests("Bearer $token")
                if (resp.isSuccessful && resp.body() != null) {
                    _state.value = _state.value.copy(authRequests = resp.body()!!.items)
                }
            } catch (_: Exception) {}
        }
    }

    fun requestAuthorization(elderlyId: String, permissionType: String, dataScope: String) {
        viewModelScope.launch {
            try {
                val resp = api.requestHospitalAuthorization(
                    "Bearer $token",
                    GrantAuthorizationRequest(
                        elderlyId = elderlyId,
                        granteeUserId = "",  // backend fills from JWT
                        permissionType = permissionType,
                        dataScope = dataScope,
                        effectiveUntil = ""  // backend defaults to 30 days
                    )
                )
                if (resp.isSuccessful) {
                    _state.value = _state.value.copy(toastMessage = "授权申请已提交")
                    loadAuthRequests()
                } else {
                    _state.value = _state.value.copy(toastMessage = "申请失败: ${resp.code()}")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(toastMessage = "申请失败: ${e.message}")
            }
        }
    }

    fun clearToast() { _state.value = _state.value.copy(toastMessage = null) }

    // ==================== 设备绑定 ====================

    fun bindDeviceByCode(code: String) {
        viewModelScope.launch {
            try {
                val resp = api.bindHospitalDevice("Bearer $token", BindDeviceRequestDto(code))
                if (resp.isSuccessful && resp.body() != null) {
                    val body = resp.body()!!
                    _state.value = _state.value.copy(toastMessage = body.message)
                    loadDashboard()
                    if (body.elderlyId.isNotBlank()) {
                        loadBoundDevices(body.elderlyId)
                    }
                } else {
                    _state.value = _state.value.copy(toastMessage = "绑定失败: ${resp.code()}")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(toastMessage = "绑定失败: ${e.message}")
            }
        }
    }

    fun loadBoundDevices(elderlyId: String) {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(selectedDeviceElderlyId = elderlyId)
                val resp = api.getHospitalDevices("Bearer $token", elderlyId)
                if (resp.isSuccessful && resp.body() != null) {
                    _state.value = _state.value.copy(boundDevices = resp.body()!!.items)
                }
            } catch (_: Exception) {}
        }
    }

    // ==================== 健康档案（Phase 3） ====================

    fun loadHealthRecords(elderlyId: String) {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(selectedHealthElderlyId = elderlyId)
                val result = repository.getHospitalHealthRecords(elderlyId)
                result.onSuccess { records ->
                    _state.value = _state.value.copy(healthRecords = records)
                }.onFailure { e ->
                    _state.value = _state.value.copy(toastMessage = "加载健康档案失败: ${e.message}")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(toastMessage = "加载健康档案失败: ${e.message}")
            }
        }
    }

    fun addHealthRecord(elderlyId: String, recordType: String, contentJson: String,
                        doctorName: String = "", hospitalName: String = "", visibility: String = "both") {
        viewModelScope.launch {
            try {
                val ts = java.time.LocalDateTime.now().toString().take(16).replace("T", " ")
                val req = AddHealthRecordRequest(
                    recordType = recordType, recordDate = ts,
                    doctorName = doctorName.ifEmpty { null },
                    hospitalName = hospitalName.ifEmpty { null },
                    contentJson = contentJson, visibility = visibility
                )
                val result = repository.addHealthRecord(elderlyId, req)
                result.onSuccess { msg ->
                    _state.value = _state.value.copy(toastMessage = msg)
                    loadHealthRecords(elderlyId)
                }.onFailure { e ->
                    _state.value = _state.value.copy(toastMessage = "录入失败: ${e.message}")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(toastMessage = "录入失败: ${e.message}")
            }
        }
    }

    // ==================== 急救权限（Phase 3） ====================

    fun requestEmergencyAccess(elderlyId: String, reason: String) {
        viewModelScope.launch {
            try {
                val result = repository.requestEmergencyAccess(elderlyId, reason)
                result.onSuccess { msg ->
                    _state.value = _state.value.copy(toastMessage = msg)
                    loadEmergencyStatus()
                }.onFailure { e ->
                    _state.value = _state.value.copy(toastMessage = "请求失败: ${e.message}")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(toastMessage = "请求失败: ${e.message}")
            }
        }
    }

    fun loadEmergencyStatus() {
        viewModelScope.launch {
            try {
                val result = repository.getEmergencyStatus()
                result.onSuccess { status ->
                    _state.value = _state.value.copy(emergencyStatus = status)
                }
            } catch (_: Exception) {}
        }
    }
}
