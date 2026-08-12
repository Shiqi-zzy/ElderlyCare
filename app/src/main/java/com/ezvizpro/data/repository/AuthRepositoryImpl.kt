package com.ezvizpro.data.repository

import com.ezvizpro.core.local.TokenManager
import com.ezvizpro.core.network.EzvizApi
import com.ezvizpro.core.util.NetworkResult
import com.ezvizpro.domain.repository.AuthRepository
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val api: EzvizApi,
    private val tokenManager: TokenManager
) : AuthRepository {

    override suspend fun login(appKey: String, appSecret: String): NetworkResult<String> {
        Timber.d("开始登录，appKey=$appKey")

        return try {
            val response = api.getAccessTokenRaw(appKey, appSecret)

            if (!response.isSuccessful) {
                val errorBody = try { response.errorBody()?.string() } catch (_: Exception) { null }
                Timber.e("登录 HTTP 失败: ${response.code()}, body=$errorBody")
                return NetworkResult.Error(
                    code = response.code().toString(),
                    message = "HTTP ${response.code()}: ${response.message()}"
                )
            }

            val body = response.body()
            if (body == null) {
                Timber.e("登录返回空响应体")
                return NetworkResult.Error(message = "服务器返回空响应")
            }

            Timber.d("登录原始响应: $body")

            val code = body["code"]?.let { (it as? JsonPrimitive)?.content } ?: ""
            val msg = body["msg"]?.let { (it as? JsonPrimitive)?.content } ?: ""

            if (code != "200") {
                Timber.e("登录失败: code=$code, msg=$msg")
                return NetworkResult.Error(code = code, message = msg.ifBlank { "未知错误" })
            }

            // data 字段可能是对象也可能是字符串，兼容处理
            val dataElement = body["data"]
            if (dataElement == null) {
                Timber.e("登录响应缺少 data 字段")
                return NetworkResult.Error(message = "响应中缺少 data 字段")
            }

            val (accessToken, expireTime) = when (dataElement) {
                is JsonObject -> {
                    // 标准格式: data = { "accessToken": "...", "expireTime": 1234567890000 }
                    val token = dataElement["accessToken"]?.let {
                        (it as? JsonPrimitive)?.content
                    } ?: ""
                    val expire = dataElement["expireTime"]?.let {
                        when (it) {
                            is JsonPrimitive -> it.content.toLongOrNull()
                            else -> null
                        }
                    } ?: (System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L) // 默认7天
                    Pair(token, expire)
                }
                is JsonPrimitive -> {
                    // 简化格式: data = "at.xxxxx"（只有 token，没有过期时间）
                    val token = dataElement.content
                    val expire = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L
                    Pair(token, expire)
                }
                else -> {
                    Timber.e("登录 data 字段格式未知: ${dataElement::class.simpleName}")
                    return NetworkResult.Error(message = "响应 data 字段格式异常")
                }
            }

            if (accessToken.isBlank()) {
                Timber.e("登录返回的 accessToken 为空")
                return NetworkResult.Error(message = "获取到的 accessToken 为空")
            }

            tokenManager.saveToken(accessToken, expireTime, appKey, appSecret)
            Timber.d("登录成功，token 有效期至 $expireTime")
            NetworkResult.Success(accessToken)

        } catch (e: java.net.UnknownHostException) {
            Timber.e(e, "登录网络错误")
            NetworkResult.Error(message = "网络连接失败，请检查网络", throwable = e)
        } catch (e: java.net.SocketTimeoutException) {
            Timber.e(e, "登录超时")
            NetworkResult.Error(message = "请求超时，请稍后重试", throwable = e)
        } catch (e: Exception) {
            Timber.e(e, "登录异常")
            NetworkResult.Error(message = e.message ?: "登录失败", throwable = e)
        }
    }

    override suspend fun isLoggedIn(): Boolean {
        return tokenManager.isTokenValid()
    }

    override suspend fun logout() {
        tokenManager.clearToken()
        Timber.d("已退出登录")
    }
}
