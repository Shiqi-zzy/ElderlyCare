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

    companion object {
        private const val KEY_RETENTION_DAYS = "alarm_retention_days"
        const val DEFAULT_RETENTION_DAYS = 7
    }
}
