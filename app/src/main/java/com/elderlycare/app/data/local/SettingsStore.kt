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

    /**
     * RK3 局域网 HTTP 服务地址（报告/建议接口 baseUrl，形如 http://192.168.110.43:8080）。
     * 空串 = 未设置；RK3 无公网 IP，仅在手机与设备同一 WiFi 下可访问。
     */
    fun getRk3ServerAddress(): String =
        prefs.getString(KEY_RK3_SERVER_ADDRESS, "").orEmpty()

    fun setRk3ServerAddress(address: String) {
        prefs.edit().putString(KEY_RK3_SERVER_ADDRESS, address.trim()).apply()
    }

    /** 建议 Tab 的 AI 智能体配置（仅本地保存，暂不上传设备/不发起请求） */
    data class AiAgentConfig(
        val modelName: String = "",
        val temperature: String = "",
        val maxTokens: String = "",
        val systemPrompt: String = ""
    )

    fun getAiAgentConfig(): AiAgentConfig = AiAgentConfig(
        modelName = prefs.getString(KEY_AI_MODEL_NAME, "").orEmpty(),
        temperature = prefs.getString(KEY_AI_TEMPERATURE, "").orEmpty(),
        maxTokens = prefs.getString(KEY_AI_MAX_TOKENS, "").orEmpty(),
        systemPrompt = prefs.getString(KEY_AI_SYSTEM_PROMPT, "").orEmpty()
    )

    fun setAiAgentConfig(config: AiAgentConfig) {
        prefs.edit()
            .putString(KEY_AI_MODEL_NAME, config.modelName)
            .putString(KEY_AI_TEMPERATURE, config.temperature)
            .putString(KEY_AI_MAX_TOKENS, config.maxTokens)
            .putString(KEY_AI_SYSTEM_PROMPT, config.systemPrompt)
            .apply()
    }

    companion object {
        private const val KEY_RETENTION_DAYS = "alarm_retention_days"
        private const val KEY_RK3_MEDIA_MOCK = "rk3_media_mock"
        private const val KEY_RK3_SERVER_ADDRESS = "rk3_server_address"
        private const val KEY_AI_MODEL_NAME = "ai_model_name"
        private const val KEY_AI_TEMPERATURE = "ai_temperature"
        private const val KEY_AI_MAX_TOKENS = "ai_max_tokens"
        private const val KEY_AI_SYSTEM_PROMPT = "ai_system_prompt"
        const val DEFAULT_RETENTION_DAYS = 7
    }
}
