package com.elderlycare.app.ui.reminder

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.rk3.Rk3DayData
import com.elderlycare.app.data.rk3.Rk3LanClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

/** 情绪日卡状态：day==null 且无 error 且非 loading = 该日期无采集数据（占位文案） */
data class EmotionDayUiState(
    val isLoading: Boolean = false,
    val day: Rk3DayData? = null,
    val errorMessage: String? = null
)

/**
 * 日程 Tab 情绪日卡 VM：选中日期 → 调 RK3 周报接口（该日期所在周的周一至周日）
 * → days 数组按日期字符串匹配当日。未命中 = 「该日期暂无采集数据」。
 * 提醒计划/复诊确认等原功能与本 VM 完全无关，不受影响。
 */
class Rk3DailyEmotionViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(EmotionDayUiState())
    val uiState: StateFlow<EmotionDayUiState> = _uiState.asStateFlow()

    fun load(date: LocalDate) {
        // 地址未设置 → 统一「未设置」文案，不发请求
        if (ServiceLocator.settingsStore.getRk3ServerAddress().isBlank()) {
            _uiState.value = EmotionDayUiState(errorMessage = Rk3LanClient.MSG_SERVER_NOT_SET)
            return
        }
        _uiState.value = EmotionDayUiState(isLoading = true)
        viewModelScope.launch {
            val monday = date.with(DayOfWeek.MONDAY)
            ServiceLocator.rk3Repository.fetchWeekly(monday.toString(), monday.plusDays(6).toString())
                .onSuccess { weekly ->
                    val target = date.toString()
                    _uiState.value = EmotionDayUiState(day = weekly.days.firstOrNull { it.date == target })
                }
                .onFailure { e ->
                    _uiState.value = EmotionDayUiState(errorMessage = e.message ?: Rk3LanClient.MSG_SERVER_NOT_SET)
                }
        }
    }
}
