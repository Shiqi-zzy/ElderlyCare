package com.elderlycare.app.data.ezviz

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * Token 管理器（SharedPreferences 持久化）
 *
 * 职责：
 * 1. 存储 accessToken 和过期时间
 * 2. 判断 Token 是否有效（未真正过期即返回）
 */
class TokenManager(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("ezviz_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "TokenManager"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_EXPIRE_TIME = "expire_time"
    }

    /**
     * 获取有效 Token；已过期返回 null
     */
    fun getValidToken(): String? {
        val token = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val expireTime = prefs.getLong(KEY_EXPIRE_TIME, 0L)
        if (expireTime <= 0L) return token // 无过期时间，直接返回
        return if (System.currentTimeMillis() < expireTime) token else null
    }

    /**
     * 获取当前 Token（即使过期也返回，用于退出登录等）
     */
    fun getTokenForcefully(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    fun saveToken(accessToken: String, expireTime: Long) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putLong(KEY_EXPIRE_TIME, expireTime)
            .apply()
        Log.d(TAG, "Token 已保存，过期时间: $expireTime")
    }

    fun clearToken() {
        prefs.edit().clear().apply()
        Log.d(TAG, "Token 已清除")
    }
}
