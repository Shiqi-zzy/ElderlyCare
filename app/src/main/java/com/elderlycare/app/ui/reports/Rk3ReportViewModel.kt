package com.elderlycare.app.ui.reports

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.rk3.Rk3HealthData
import com.elderlycare.app.data.rk3.Rk3LanClient
import com.elderlycare.app.data.rk3.Rk3SuggestionData
import com.elderlycare.app.data.rk3.Rk3WeeklyData
import com.elderlycare.app.data.rk3.Rk3YearlyData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

/** 单个 Tab 的加载状态：Idle=尚未加载（进入 Tab 时惰性加载一次） */
sealed interface Rk3TabState<out T> {
    object Idle : Rk3TabState<Nothing>
    object Loading : Rk3TabState<Nothing>
    data class Success<T>(val data: T) : Rk3TabState<T>
    data class Failed(val message: String) : Rk3TabState<Nothing>
}

data class Rk3ReportUiState(
    val realtime: Rk3TabState<Rk3HealthData> = Rk3TabState.Idle,
    val weekly: Rk3TabState<Rk3WeeklyData> = Rk3TabState.Idle,
    val yearly: Rk3TabState<Rk3YearlyData> = Rk3TabState.Idle,
    /** Success(null) = 设备还没有生成任何建议（「暂无建议，点击上方按钮生成」） */
    val suggestion: Rk3TabState<Rk3SuggestionData?> = Rk3TabState.Idle,
)

/**
 * 报告页（实时/周度/年度/建议四 Tab）VM。
 *
 * 统一规则：服务器地址为空 → 直接 Failed(「请前往设置…」) 不发请求；
 * 全部请求走 viewModelScope 协程 + RK3 独立 12s 超时客户端（见 Rk3LanRepository）；
 * 失败 message 为可直接展示的用户文案；建议 Tab「生成家属建议」= 仅重调接口刷新，
 * 不做本地大模型请求。
 */
class Rk3ReportViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(Rk3ReportUiState())
    val uiState: StateFlow<Rk3ReportUiState> = _uiState.asStateFlow()

    private val repository get() = ServiceLocator.rk3Repository
    private val settings get() = ServiceLocator.settingsStore

    /** 周度查询区间：本周周一至周日（yyyy-MM-dd） */
    private fun currentWeekRange(): Pair<String, String> {
        val monday = LocalDate.now().with(DayOfWeek.MONDAY)
        return monday.toString() to monday.plusDays(6).toString()
    }

    private fun addressBlank(): Boolean = settings.getRk3ServerAddress().isBlank()

    private fun <T> fail(update: (Rk3ReportUiState, Rk3TabState<T>) -> Rk3ReportUiState, e: Throwable) {
        _uiState.update { update(it, Rk3TabState.Failed(e.message ?: Rk3LanClient.MSG_SERVER_NOT_SET)) }
    }

    // ==================== 实时 Tab ====================

    /** 进入 Tab 惰性加载（仅 Idle 时请求） */
    fun ensureRealtime() {
        if (_uiState.value.realtime is Rk3TabState.Idle) refreshRealtime()
    }

    fun refreshRealtime() {
        if (addressBlank()) {
            _uiState.update { it.copy(realtime = Rk3TabState.Failed(Rk3LanClient.MSG_SERVER_NOT_SET)) }
            return
        }
        _uiState.update { it.copy(realtime = Rk3TabState.Loading) }
        viewModelScope.launch {
            repository.fetchHealth()
                .onSuccess { data ->
                    _uiState.update { it.copy(realtime = Rk3TabState.Success(data)) }
                }
                .onFailure { e -> fail({ s, t -> s.copy(realtime = t) }, e) }
        }
    }

    // ==================== 周度 Tab ====================

    fun ensureWeekly() {
        if (_uiState.value.weekly is Rk3TabState.Idle) refreshWeekly()
    }

    fun refreshWeekly() {
        if (addressBlank()) {
            _uiState.update { it.copy(weekly = Rk3TabState.Failed(Rk3LanClient.MSG_SERVER_NOT_SET)) }
            return
        }
        _uiState.update { it.copy(weekly = Rk3TabState.Loading) }
        viewModelScope.launch {
            val (start, end) = currentWeekRange()
            repository.fetchWeekly(start, end)
                .onSuccess { data ->
                    _uiState.update { it.copy(weekly = Rk3TabState.Success(data)) }
                }
                .onFailure { e -> fail({ s, t -> s.copy(weekly = t) }, e) }
        }
    }

    // ==================== 年度 Tab ====================

    fun ensureYearly() {
        if (_uiState.value.yearly is Rk3TabState.Idle) refreshYearly()
    }

    fun refreshYearly() {
        if (addressBlank()) {
            _uiState.update { it.copy(yearly = Rk3TabState.Failed(Rk3LanClient.MSG_SERVER_NOT_SET)) }
            return
        }
        _uiState.update { it.copy(yearly = Rk3TabState.Loading) }
        viewModelScope.launch {
            repository.fetchYearly(LocalDate.now().year)
                .onSuccess { data ->
                    _uiState.update { it.copy(yearly = Rk3TabState.Success(data)) }
                }
                .onFailure { e -> fail({ s, t -> s.copy(yearly = t) }, e) }
        }
    }

    // ==================== 建议 Tab ====================

    fun ensureSuggestion() {
        if (_uiState.value.suggestion is Rk3TabState.Idle) refreshSuggestion()
    }

    /** 「生成家属建议」按钮 = 仅刷新重调 /api/suggestions/latest，不做本地大模型请求 */
    fun refreshSuggestion() {
        if (addressBlank()) {
            _uiState.update { it.copy(suggestion = Rk3TabState.Failed(Rk3LanClient.MSG_SERVER_NOT_SET)) }
            return
        }
        _uiState.update { it.copy(suggestion = Rk3TabState.Loading) }
        viewModelScope.launch {
            repository.fetchLatestSuggestion()
                .onSuccess { data ->
                    _uiState.update { it.copy(suggestion = Rk3TabState.Success(data)) }
                }
                .onFailure { e -> fail({ s, t -> s.copy(suggestion = t) }, e) }
        }
    }
}
