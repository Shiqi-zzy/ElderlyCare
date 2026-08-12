package com.ezvizpro.ui.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ezvizpro.data.remote.dto.*
import com.ezvizpro.data.repository.ElderlyCareRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FamilyUiState(
    val elderlyList: List<ElderlyDto> = emptyList(),
    val selectedElderly: ElderlyDto? = null,
    val alarms: List<AlarmDto> = emptyList(),
    val authorizations: List<AuthorizationDto> = emptyList(),
    val devices: List<DeviceDto> = emptyList(),
    val privacyPaused: Boolean = false,
    val isLoading: Boolean = false,
    val isSimulating: Boolean = false,
    val error: String? = null,
    val toastMessage: String? = null,
    // 设备验证码
    val generatedCode: GenerateCodeResponse? = null,
    val showCodeDialog: Boolean = false,
    val activeCodes: List<VerificationCodeItem> = emptyList(),
    // 健康档案（Phase 3）
    val healthRecords: List<HealthRecordDto> = emptyList()
)

@HiltViewModel
class FamilyViewModel @Inject constructor(
    private val repository: ElderlyCareRepository
) : ViewModel() {

    private val _state = MutableStateFlow(FamilyUiState())
    val state: StateFlow<FamilyUiState> = _state.asStateFlow()

    private val _pendingRequests = MutableStateFlow<List<AuthorizationDto>>(emptyList())
    val pendingRequests: StateFlow<List<AuthorizationDto>> = _pendingRequests.asStateFlow()

    init { loadElderlyList() }

    fun loadElderlyList() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            repository.getMyElderly().onSuccess { list ->
                _state.value = _state.value.copy(elderlyList = list, isLoading = false)
                if (list.isNotEmpty() && _state.value.selectedElderly == null) {
                    selectElderly(list.first())
                }
            }.onFailure { e ->
                _state.value = _state.value.copy(
                    error = e.message,
                    isLoading = false,
                    toastMessage = "加载老人列表失败: ${e.message}"
                )
            }
        }
    }

    fun selectElderly(elderly: ElderlyDto) {
        _state.value = _state.value.copy(selectedElderly = elderly)
        loadAlarms(elderly.id)
        loadAuthorizations(elderly.id)
        loadDevices(elderly.id)
        loadPrivacyStatus(elderly.id)
    }

    fun loadAlarms(elderlyId: String) {
        viewModelScope.launch {
            repository.getAlarms(elderlyId).onSuccess { list ->
                _state.value = _state.value.copy(alarms = list)
            }
        }
    }

    fun acknowledgeAlarm(alarmId: String) {
        viewModelScope.launch {
            repository.acknowledgeAlarm(alarmId).onSuccess {
                _state.value = _state.value.copy(toastMessage = "告警已确认")
                _state.value.selectedElderly?.let { loadAlarms(it.id) }
            }.onFailure { e ->
                _state.value = _state.value.copy(toastMessage = "确认失败: ${e.message}")
            }
        }
    }

    fun simulateAlarm() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSimulating = true)
            repository.simulateAlarm().onSuccess {
                _state.value = _state.value.copy(
                    isSimulating = false,
                    toastMessage = "模拟告警: ${it.alarmLevel} - ${it.title}"
                )
                _state.value.selectedElderly?.let { e -> loadAlarms(e.id) }
            }.onFailure { e ->
                _state.value = _state.value.copy(isSimulating = false, toastMessage = "模拟失败: ${e.message}")
            }
        }
    }

    fun loadAuthorizations(elderlyId: String) {
        viewModelScope.launch {
            repository.getAuthorizations(elderlyId)
                .onSuccess { list -> _state.value = _state.value.copy(authorizations = list) }
                .onFailure { e -> _state.value = _state.value.copy(toastMessage = "加载授权列表失败: ${e.message}") }
        }
    }

    fun grantAuthorization(
        granteeUserId: String, permissionType: String, dataScope: String, effectiveUntil: String
    ) {
        viewModelScope.launch {
            val elderlyId = _state.value.selectedElderly?.id
            if (elderlyId == null) {
                _state.value = _state.value.copy(toastMessage = "请先在告警中心选择一位老人")
                return@launch
            }
            if (granteeUserId.isBlank()) {
                _state.value = _state.value.copy(toastMessage = "请输入被授权人UUID")
                return@launch
            }
            // 未填写有效期则默认 30 天
            val effective = effectiveUntil.ifBlank {
                val cal = java.util.Calendar.getInstance()
                cal.add(java.util.Calendar.DAY_OF_YEAR, 30)
                java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(cal.time) + "T23:59:59"
            }
            repository.grantAuthorization(
                GrantAuthorizationRequest(elderlyId, granteeUserId, permissionType, dataScope, effective)
            ).onSuccess {
                _state.value = _state.value.copy(toastMessage = "授权成功")
                loadAuthorizations(elderlyId)
            }.onFailure { e ->
                _state.value = _state.value.copy(toastMessage = "授权失败: ${e.message}")
            }
        }
    }

    fun revokeAuthorization(authId: String, reason: String? = null) {
        viewModelScope.launch {
            repository.revokeAuthorization(authId, reason).onSuccess {
                _state.value = _state.value.copy(toastMessage = "授权已撤销")
                _state.value.selectedElderly?.let { loadAuthorizations(it.id) }
            }.onFailure { e ->
                _state.value = _state.value.copy(toastMessage = "撤销失败: ${e.message}")
            }
        }
    }

    fun togglePrivacy() {
        viewModelScope.launch {
            val elderlyId = _state.value.selectedElderly?.id ?: return@launch
            if (_state.value.privacyPaused) {
                repository.resumeMonitoring(elderlyId).onSuccess {
                    _state.value = _state.value.copy(privacyPaused = false, toastMessage = "监控已恢复")
                }
            } else {
                repository.pauseMonitoring(elderlyId).onSuccess {
                    _state.value = _state.value.copy(privacyPaused = true, toastMessage = "监控已暂停")
                }
            }
        }
    }

    private fun loadDevices(elderlyId: String) {
        viewModelScope.launch {
            repository.getDevices(elderlyId).onSuccess { list ->
                _state.value = _state.value.copy(devices = list)
            }
        }
    }

    private fun loadPrivacyStatus(elderlyId: String) {
        viewModelScope.launch {
            repository.getPrivacyStatus(elderlyId).onSuccess { paused ->
                _state.value = _state.value.copy(privacyPaused = paused)
            }
        }
    }

    fun loadPendingRequests() {
        viewModelScope.launch {
            repository.getPendingAuthorizationRequests()
                .onSuccess { _pendingRequests.value = it }
        }
    }

    fun approveRequest(requestId: String) {
        viewModelScope.launch {
            repository.approveAuthorizationRequest(requestId)
                .onSuccess {
                    _state.value = _state.value.copy(toastMessage = "授权申请已通过")
                    loadPendingRequests()
                    _state.value.selectedElderly?.let { loadAuthorizations(it.id) }
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(toastMessage = "审批失败: ${e.message}")
                }
        }
    }

    fun rejectRequest(requestId: String) {
        viewModelScope.launch {
            repository.rejectAuthorizationRequest(requestId)
                .onSuccess {
                    _state.value = _state.value.copy(toastMessage = "授权申请已拒绝")
                    loadPendingRequests()
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(toastMessage = "操作失败: ${e.message}")
                }
        }
    }

    fun clearToast() { _state.value = _state.value.copy(toastMessage = null) }

    // ==================== 健康档案（Phase 3） ====================

    fun loadHealthRecords(elderlyId: String) {
        viewModelScope.launch {
            repository.getFamilyHealthRecords(elderlyId)
                .onSuccess { list -> _state.value = _state.value.copy(healthRecords = list) }
                .onFailure { e -> _state.value = _state.value.copy(toastMessage = "加载健康档案失败: ${e.message}") }
        }
    }

    // ==================== 设备验证码 ====================

    fun generateDeviceCode(deviceId: String) {
        viewModelScope.launch {
            repository.generateDeviceCode(deviceId)
                .onSuccess { result ->
                    _state.value = _state.value.copy(generatedCode = result, showCodeDialog = true)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(toastMessage = "生成失败: ${e.message}")
                }
        }
    }

    fun dismissCodeDialog() {
        _state.value = _state.value.copy(showCodeDialog = false, generatedCode = null)
    }

    fun loadActiveCodes() {
        viewModelScope.launch {
            repository.getActiveDeviceCodes()
                .onSuccess { list -> _state.value = _state.value.copy(activeCodes = list) }
                .onFailure { e -> _state.value = _state.value.copy(toastMessage = "加载验证码列表失败: ${e.message}") }
        }
    }

    fun revokeDeviceCode(codeId: String) {
        viewModelScope.launch {
            repository.revokeDeviceCode(codeId)
                .onSuccess { msg ->
                    _state.value = _state.value.copy(toastMessage = msg)
                    loadActiveCodes()
                }
                .onFailure { e -> _state.value = _state.value.copy(toastMessage = "撤销失败: ${e.message}") }
        }
    }
}
