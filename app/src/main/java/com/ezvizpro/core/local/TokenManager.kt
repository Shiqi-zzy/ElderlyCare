package com.ezvizpro.core.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ezvizpro.core.network.model.TokenInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ezviz_prefs")

/**
 * Token 管理器
 *
 * 职责:
 * 1. 持久化存储 accessToken 和过期时间
 * 2. 判断 Token 是否有效（距离过期 > 1 天视为有效）
 * 3. 线程安全地刷新 Token（使用 Mutex 防止并发刷新）
 */
@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val KEY_EXPIRE_TIME = longPreferencesKey("expire_time")
        private val KEY_FETCH_TIME = longPreferencesKey("fetch_time")
        private val KEY_APP_KEY = stringPreferencesKey("app_key")
        private val KEY_APP_SECRET = stringPreferencesKey("app_secret")

        // Token 提前刷新阈值: 距离过期 < 1 天就开始刷新（仅用于主动刷新，不阻断使用）
        private const val REFRESH_THRESHOLD_MS = 24 * 60 * 60 * 1000L
    }

    private val refreshMutex = Mutex()

    /**
     * 获取有效的 Token 信息
     * 如果 Token 即将过期或已过期，会自动返回需要刷新的标记
     */
    fun tokenFlow() = context.dataStore.data.map { prefs ->
        val token = prefs[KEY_ACCESS_TOKEN]
        val expireTime = prefs[KEY_EXPIRE_TIME] ?: 0L
        val fetchTime = prefs[KEY_FETCH_TIME] ?: 0L
        if (token != null) {
            TokenInfo(token, expireTime, fetchTime)
        } else {
            null
        }
    }

    /**
     * 同步获取当前 Token（用于 Repository/UseCase）
     * 只要 token 未真正过期就返回，不因"临近过期"而拒绝
     */
    suspend fun getValidToken(): String? {
        val prefs = context.dataStore.data.first()
        val token = prefs[KEY_ACCESS_TOKEN] ?: return null
        val expireTime = prefs[KEY_EXPIRE_TIME] ?: return token // 无过期时间，直接返回
        val now = System.currentTimeMillis()

        if (now < expireTime) {
            // 临近过期时打日志提醒，但仍返回 token 保证可用
            if (now >= expireTime - REFRESH_THRESHOLD_MS) {
                Timber.d("Token 临近过期 (expires at $expireTime)，建议主动刷新")
            }
            return token
        }
        Timber.d("Token 已过期 (expires at $expireTime, now $now)")
        return null
    }

    /**
     * 获取存储的 AppKey（用于自动重新认证）
     */
    suspend fun getStoredAppKey(): String? {
        val prefs = context.dataStore.data.first()
        return prefs[KEY_APP_KEY]
    }

    /**
     * 获取存储的 AppSecret（用于自动重新认证）
     */
    suspend fun getStoredAppSecret(): String? {
        val prefs = context.dataStore.data.first()
        return prefs[KEY_APP_SECRET]
    }

    /**
     * 判断 token 是否临近过期，需要主动刷新
     */
    suspend fun needsRefresh(): Boolean {
        val prefs = context.dataStore.data.first()
        val expireTime = prefs[KEY_EXPIRE_TIME] ?: return false
        val now = System.currentTimeMillis()
        return now >= expireTime - REFRESH_THRESHOLD_MS
    }

    /**
     * 线程安全地保存 Token 及凭证
     */
    suspend fun saveToken(accessToken: String, expireTime: Long, appKey: String = "", appSecret: String = "") {
        refreshMutex.withLock {
            context.dataStore.edit { prefs ->
                prefs[KEY_ACCESS_TOKEN] = accessToken
                prefs[KEY_EXPIRE_TIME] = expireTime
                prefs[KEY_FETCH_TIME] = System.currentTimeMillis()
                if (appKey.isNotBlank()) prefs[KEY_APP_KEY] = appKey
                if (appSecret.isNotBlank()) prefs[KEY_APP_SECRET] = appSecret
            }
            Timber.d("Token 已保存，过期时间: $expireTime")
        }
    }

    /**
     * 检查当前 Token 是否有效
     */
    suspend fun isTokenValid(): Boolean {
        val token = getValidToken()
        return token != null
    }

    /**
     * 清除 Token（退出登录时调用）
     */
    suspend fun clearToken() {
        refreshMutex.withLock {
            context.dataStore.edit { it.clear() }
            Timber.d("Token 已清除")
        }
    }

    /**
     * 获取当前 Token（即使即将过期也返回，仅用于退出登录等操作）
     */
    suspend fun getTokenForcefully(): String? {
        val prefs = context.dataStore.data.first()
        return prefs[KEY_ACCESS_TOKEN]
    }
}
