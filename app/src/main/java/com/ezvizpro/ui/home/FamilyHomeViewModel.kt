package com.ezvizpro.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ezvizpro.core.local.HomeLocalStore
import com.ezvizpro.core.local.TokenManager
import com.ezvizpro.core.util.NetworkResult
import com.ezvizpro.domain.model.AlarmType
import com.ezvizpro.domain.repository.AlarmRepository
import com.ezvizpro.domain.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class FamilyHomeUiState(
    // 隐私遮蔽
    val isPrivacyShieldOn: Boolean = false,
    val deviceSerial: String = "",
    // 快捷功能
    val hasMedicineReminder: Boolean = false,
    // 家庭时光
    val albumUsedMB: Long = 0,
    val albumTotalMB: Long = 1024,
    val faceCapturesToday: List<String> = emptyList(),
    // 播放控制
    val isPlaying: Boolean = false,
    val isMuted: Boolean = false,
    val currentDeviceName: String = "",
    // 通用
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class FamilyHomeViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val alarmRepository: AlarmRepository,
    private val tokenManager: TokenManager,
    private val homeLocalStore: HomeLocalStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(FamilyHomeUiState())
    val uiState: StateFlow<FamilyHomeUiState> = _uiState.asStateFlow()

    init {
        loadAllData()
        observeMedicineReminder()
    }

    private fun observeMedicineReminder() {
        viewModelScope.launch {
            homeLocalStore.hasMedicineReminder.collect { hasReminder ->
                _uiState.update { it.copy(hasMedicineReminder = hasReminder) }
            }
        }
    }

    fun loadAllData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            loadDeviceStatus()
            loadFaceCaptures()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun loadDeviceStatus() {
        when (val result = deviceRepository.getDeviceList()) {
            is NetworkResult.Success -> {
                val devices = result.data
                val firstOnline = devices.firstOrNull { it.status.name == "ONLINE" }
                _uiState.update {
                    it.copy(
                        isPrivacyShieldOn = firstOnline?.defence ?: false,
                        deviceSerial = firstOnline?.deviceSerial ?: "",
                        currentDeviceName = firstOnline?.deviceName ?: "未连接设备"
                    )
                }
            }
            is NetworkResult.Error -> {
                Timber.e("首页加载设备失败: ${result.message}")
                _uiState.update { it.copy(error = result.message) }
            }
        }
    }

    private suspend fun loadFaceCaptures() {
        // 从人形检测告警中提取抓拍图作为"人脸抓拍"
        try {
            when (val result = alarmRepository.getAlarmList(
                pageStart = 0, pageSize = 10, alarmType = AlarmType.HUMAN_DETECT.code
            )) {
                is NetworkResult.Success -> {
                    val captures = result.data
                        .filter { it.alarmPicUrl != null }
                        .mapNotNull { it.alarmPicUrl }
                    _uiState.update { it.copy(faceCapturesToday = captures) }
                }
                is NetworkResult.Error -> {
                    Timber.w("加载人脸抓拍失败: ${result.message}")
                }
            }
        } catch (_: Exception) {}
    }

    fun onPlayPause() {
        _uiState.update { it.copy(isPlaying = !it.isPlaying) }
    }

    fun onMuteToggle() {
        _uiState.update { it.copy(isMuted = !it.isMuted) }
    }
}
