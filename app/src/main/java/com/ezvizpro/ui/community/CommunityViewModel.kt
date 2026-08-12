package com.ezvizpro.ui.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ezvizpro.data.remote.ElderlyCareApi
import com.ezvizpro.data.remote.dto.*
import com.ezvizpro.data.remote.dto.GrantAuthorizationRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CommunityUiState(
    val pendingWorkOrders: Int = 0,
    val workOrders: List<WorkOrderDto> = emptyList(),
    val elderlyList: List<ElderlyDto> = emptyList(),
    val authRequests: List<AuthorizationDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val toastMessage: String? = null,
    // 设备管理
    val boundDevices: List<DeviceDto> = emptyList(),
    val selectedDeviceElderlyId: String = "",
    val maintenanceHistory: List<InspectionDto> = emptyList()
)

@HiltViewModel
class CommunityViewModel @Inject constructor(
    private val api: ElderlyCareApi
) : ViewModel() {

    private val _state = MutableStateFlow(CommunityUiState())
    val state: StateFlow<CommunityUiState> = _state.asStateFlow()

    private var token: String = ""

    fun setToken(t: String) { token = t; loadDashboard() }

    fun loadDashboard() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val auth = "Bearer $token"
                // 仪表盘
                val dashResp = api.getCommunityDashboard(auth)
                if (dashResp.isSuccessful) {
                    _state.value = _state.value.copy(pendingWorkOrders = dashResp.body()?.pendingWorkOrders ?: 0)
                }
                // 老人台账(脱敏)
                val elderlyResp = api.getCommunityElderlyList(auth)
                if (elderlyResp.isSuccessful) {
                    _state.value = _state.value.copy(elderlyList = elderlyResp.body()?.items ?: emptyList())
                }
                // 工单列表
                val ordersResp = api.getMyWorkOrders(auth)
                if (ordersResp.isSuccessful) {
                    _state.value = _state.value.copy(workOrders = ordersResp.body()?.items ?: emptyList())
                }
                _state.value = _state.value.copy(isLoading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun acceptOrder(orderId: String) {
        viewModelScope.launch {
            try {
                api.acceptWorkOrder("Bearer $token", orderId)
                _state.value = _state.value.copy(toastMessage = "已接单")
                loadDashboard()
            } catch (e: Exception) {
                _state.value = _state.value.copy(toastMessage = "接单失败: ${e.message}")
            }
        }
    }

    fun completeOrder(orderId: String, resultJson: String, photos: String?) {
        viewModelScope.launch {
            try {
                api.completeWorkOrder("Bearer $token", orderId, WorkOrderCompleteRequest(resultJson, photos))
                _state.value = _state.value.copy(toastMessage = "工单已完成")
                loadDashboard()
            } catch (e: Exception) {
                _state.value = _state.value.copy(toastMessage = "完成失败: ${e.message}")
            }
        }
    }

    fun loadAuthRequests() {
        viewModelScope.launch {
            try {
                val resp = api.getCommunityAuthorizationRequests("Bearer $token")
                if (resp.isSuccessful && resp.body() != null) {
                    _state.value = _state.value.copy(authRequests = resp.body()!!.items)
                }
            } catch (_: Exception) {}
        }
    }

    fun requestAuthorization(elderlyId: String, permissionType: String, dataScope: String) {
        viewModelScope.launch {
            try {
                val resp = api.requestCommunityAuthorization(
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

    // ==================== 设备绑定与巡检 ====================

    fun bindDeviceByCode(code: String) {
        viewModelScope.launch {
            try {
                val resp = api.bindCommunityDevice("Bearer $token", BindDeviceRequestDto(code))
                if (resp.isSuccessful && resp.body() != null) {
                    val body = resp.body()!!
                    _state.value = _state.value.copy(toastMessage = body.message)
                    loadDashboard()
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
                val resp = api.getCommunityDevices("Bearer $token", elderlyId)
                if (resp.isSuccessful && resp.body() != null) {
                    _state.value = _state.value.copy(boundDevices = resp.body()!!.items)
                }
            } catch (_: Exception) {}
        }
    }

    fun logInspection(deviceId: String, type: String, status: String, findings: String) {
        viewModelScope.launch {
            try {
                val resp = api.logDeviceInspection(
                    "Bearer $token", deviceId,
                    InspectionRequest(type, status, findings, "", "")
                )
                if (resp.isSuccessful) {
                    _state.value = _state.value.copy(toastMessage = "巡检记录已保存")
                    loadMaintenanceHistory(deviceId)
                } else {
                    _state.value = _state.value.copy(toastMessage = "巡检记录失败: ${resp.code()}")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(toastMessage = "巡检记录失败: ${e.message}")
            }
        }
    }

    fun loadMaintenanceHistory(deviceId: String) {
        viewModelScope.launch {
            try {
                val resp = api.getDeviceMaintenanceHistory("Bearer $token", deviceId)
                if (resp.isSuccessful && resp.body() != null) {
                    _state.value = _state.value.copy(maintenanceHistory = resp.body()!!.items)
                }
            } catch (_: Exception) {}
        }
    }
}
