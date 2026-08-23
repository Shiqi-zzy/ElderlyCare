package com.elderlycare.app.data.local

import android.content.Context
import android.content.SharedPreferences

/**
 * 应用设置持久化（SharedPreferences）。
 */
class SettingsStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    /** 告警片段保留天数（默认 7 天） */
    fun getAlarmRetentionDays(): Int =
        prefs.getInt(KEY_RETENTION_DAYS, DEFAULT_RETENTION_DAYS)

    fun setAlarmRetentionDays(days: Int) {
        prefs.edit().putInt(KEY_RETENTION_DAYS, days.coerceIn(1, 30)).apply()
    }

    /**
     * RK3 点播/广播FM Mock 开关（默认开：网络层为占位实现，
     * Mock 是当前唯一可用路径；真实接口开通后默认改 false）。
     */
    fun isRk3MediaMockEnabled(): Boolean =
        prefs.getBoolean(KEY_RK3_MEDIA_MOCK, true)

    fun setRk3MediaMockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_RK3_MEDIA_MOCK, enabled).apply()
    }

    companion object {
        private const val KEY_RETENTION_DAYS = "alarm_retention_days"
        private const val KEY_RK3_MEDIA_MOCK = "rk3_media_mock"
        const val DEFAULT_RETENTION_DAYS = 7
    }
}
