package com.ezvizpro.ui.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ezvizpro.core.util.NetworkResult
import com.ezvizpro.data.repository.ElderlyCareRepository
import com.ezvizpro.domain.usecase.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * 萤石设备绑定登录 ViewModel
 *
 * 流程：
 * 1. App 启动 → 自动用萤石 AppKey/AppSecret 获取 accessToken（设备绑定）
 * 2. 后端 sync（client_id + accessToken）→ 查找/创建用户
 * 3. 显示门户选择 → 用户选家属/社区/医院
 * 4. 调用 select-role（如角色变化）→ 返回 JWT → 导航进端
 *
 * 验证码方案已移除，登录与萤石设备体系绑定。
 */
data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    /** 萤石登录+后端sync完成 */
    val ezvizReady: Boolean = false,
    /** 用户已选门户，导航就绪 */
    val portalSelected: Boolean = false,
    val clientId: String = "",
    val ezvizToken: String = "",
    val backendToken: String = "",
    val currentRole: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val loginUseCase: LoginUseCase,
    private val repository: ElderlyCareRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    init {
        loginToEzviz()
    }

    fun retry() {
        _state.value = _state.value.copy(error = null)
        loginToEzviz()
    }

    /** 萤石自动登录 → 后端 sync */
    private fun loginToEzviz() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            val appKey = com.ezvizpro.BuildConfig.EZVIZ_APP_KEY.takeIf { it.isNotBlank() }
            val appSecret = com.ezvizpro.BuildConfig.EZVIZ_APP_SECRET.takeIf { it.isNotBlank() }

            if (appKey == null || appSecret == null) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "请配置萤石 AppKey/AppSecret"
                )
                return@launch
            }

            when (val result = loginUseCase(appKey, appSecret)) {
                is NetworkResult.Success -> syncWithBackend(result.data)
                is NetworkResult.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
            }
        }
    }

    /** 后端 sync：client_id + accessToken → 用户信息 + JWT */
    private suspend fun syncWithBackend(ezvizToken: String) {
        val clientId = getOrCreateClientId()

        repository.sync(clientId, ezvizToken)
            .onSuccess { resp ->
                if (resp.user != null && resp.accessToken.isNotEmpty()) {
                    repository.setToken(resp.accessToken)
                    _state.value = _state.value.copy(
                        isLoading = false,
                        ezvizReady = true,
                        clientId = clientId,
                        ezvizToken = ezvizToken,
                        backendToken = resp.accessToken,
                        currentRole = resp.user.role
                    )
                } else if (resp.needSelectRole) {
                    // 新设备，尚无角色
                    _state.value = _state.value.copy(
                        isLoading = false,
                        ezvizReady = true,
                        clientId = clientId,
                        ezvizToken = ezvizToken,
                        backendToken = "",
                        currentRole = null
                    )
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "服务端返回数据异常"
                    )
                }
            }
            .onFailure { e ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "同步失败: ${e.localizedMessage}"
                )
            }
    }

    /** 用户在门户选择页选了角色 */
    fun selectPortal(role: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            val clientId = _state.value.clientId

            // 同角色已有 token → 直接走
            if (_state.value.currentRole == role && _state.value.backendToken.isNotEmpty()) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    portalSelected = true,
                    currentRole = role
                )
                return@launch
            }

            // 切换角色 / 新用户 → 调 select-role
            repository.selectRole(clientId, role, "", "")
                .onSuccess { resp ->
                    if (resp.user != null && resp.accessToken.isNotEmpty()) {
                        repository.setToken(resp.accessToken)
                        _state.value = _state.value.copy(
                            isLoading = false,
                            portalSelected = true,
                            currentRole = resp.user.role,
                            backendToken = resp.accessToken
                        )
                    } else {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = "角色选择失败"
                        )
                    }
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = e.localizedMessage ?: "角色选择失败"
                    )
                }
        }
    }

    /** 导航完成后重置 */
    fun resetForNavigation() {
        _state.value = _state.value.copy(portalSelected = false)
    }

    private fun getOrCreateClientId(): String {
        val prefs = context.getSharedPreferences("ezvizpro_prefs", Context.MODE_PRIVATE)
        var id = prefs.getString("client_id", null)
        if (id == null) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString("client_id", id).apply()
        }
        return id
    }
}
