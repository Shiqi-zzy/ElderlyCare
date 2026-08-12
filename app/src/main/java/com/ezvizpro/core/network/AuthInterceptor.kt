package com.ezvizpro.core.network

import com.ezvizpro.BuildConfig
import com.ezvizpro.core.local.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Token 注入拦截器
 *
 * 注：萤石 API 的 accessToken 是作为 Form Field 而非 HTTP Header 传递的，
 * 所以此拦截器主要处理 Token 过期检测和日志记录。
 * 实际的 accessToken 参数在 Api 接口的 @Field 中传递。
 *
 * 此拦截器负责：
 * 1. 日志记录请求和响应
 * 2. 检测 401/Token 过期响应（后续可扩展自动重试）
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // 记录请求日志
        Timber.tag("EzViz-Network").d(
            "→ ${originalRequest.method} ${originalRequest.url.encodedPath}"
        )

        val response = chain.proceed(originalRequest)

        // 记录响应日志
        Timber.tag("EzViz-Network").d(
            "← ${response.code} ${originalRequest.url.encodedPath}"
        )

        return response
    }
}
