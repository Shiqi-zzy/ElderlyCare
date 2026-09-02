package com.elderlycare.app.data.rk3

import android.util.Log
import com.elderlycare.app.data.local.SettingsStore
import com.google.gson.JsonObject

/**
 * RK3 局域网报告/建议仓库。
 *
 * 统一规则：服务器地址每次请求前现读设置（改设置立即生效，无需重启）；
 * 地址为空 → Result.failure(Rk3LanException(MSG_SERVER_NOT_SET))，不发请求；
 * 其余失败 exception.message 为可直接展示的用户文案（照 CaptureRepository 范式）；
 * data=null → 各 fetch 转对应空态实例（UI「暂无数据」占位，绝不显示 0% 乱数据）。
 */
class Rk3LanRepository(
    private val settingsStore: SettingsStore,
    private val client: Rk3LanClient
) {

    /** 实时状态 + 最近采集记录；data=null → 全字段 null 实例（UI 显示「--」+「暂无采集记录」） */
    suspend fun fetchHealth(): Result<Rk3HealthData> =
        fetch("/api/health") { data -> Rk3HealthData.fromJson(data) ?: Rk3HealthData(null, null, null, emptyList()) }

    /** 周报（start/end = yyyy-MM-dd，本周周一至周日）；data=null → 空实例（「本周暂无情感采集数据」） */
    suspend fun fetchWeekly(start: String, end: String): Result<Rk3WeeklyData> =
        fetch("/api/reports/weekly", mapOf("start" to start, "end" to end)) { data ->
            Rk3WeeklyData.fromJson(data) ?: Rk3WeeklyData.empty()
        }

    /** 年报（year 当前年，start/end 覆盖全年）；data=null → 空实例（「本年暂无情感采集数据」） */
    suspend fun fetchYearly(year: Int): Result<Rk3YearlyData> =
        fetch(
            "/api/reports/yearly",
            mapOf("year" to year.toString(), "start" to "${year}-01-01", "end" to "${year}-12-31")
        ) { data ->
            Rk3YearlyData.fromJson(data) ?: Rk3YearlyData.empty()
        }

    /** 最新家属建议；data=null → Success(null)（UI「暂无建议，点击上方按钮生成」） */
    suspend fun fetchLatestSuggestion(): Result<Rk3SuggestionData?> =
        fetch("/api/suggestions/latest") { data -> Rk3SuggestionData.fromJson(data) }

    private suspend fun <T> fetch(
        path: String,
        query: Map<String, String> = emptyMap(),
        parse: (JsonObject?) -> T
    ): Result<T> {
        val baseUrl = settingsStore.getRk3ServerAddress()
        if (baseUrl.isBlank()) {
            Log.w(TAG, "RK3 服务器地址未设置（$path）")
            return Result.failure(Rk3LanException(Rk3LanClient.MSG_SERVER_NOT_SET))
        }
        return runCatching {
            val data = client.get(baseUrl, path, query)
            parse(data)
        }.onFailure { e ->
            Log.e(TAG, "$path 请求失败", e)
        }
    }

    private companion object {
        const val TAG = "Rk3LanRepository"
    }
}
