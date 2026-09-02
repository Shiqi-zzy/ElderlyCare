package com.elderlycare.app.data.ezviz

import android.util.Log
import retrofit2.Response

/**
 * 统一结果封装
 */
sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error(
        val code: String? = null,
        val message: String,
        val throwable: Throwable? = null
    ) : NetworkResult<Nothing>()

    val isSuccess get() = this is Success
    val isError get() = this is Error

    fun <R> map(transform: (T) -> R): NetworkResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
    }

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }
}

private const val TAG = "EzViz-Network"

/**
 * 将萤石 API 返回的 ApiResponse 转换为 NetworkResult。
 * 兼容无 data 字段的接口（device/add、alarm/read 等），此时 data==null 返回 Unit。
 */
@Suppress("UNCHECKED_CAST")
suspend fun <T> apiCall(call: suspend () -> Response<ApiResponse<T>>): NetworkResult<T> {
    return try {
        val response = call()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null && body.code == "200") {
                NetworkResult.Success((body.data ?: Unit) as T)
            } else {
                NetworkResult.Error(
                    code = body?.code ?: "-1",
                    message = body?.msg?.takeIf { it.isNotBlank() } ?: "未知错误"
                )
            }
        } else {
            val errorBody = try {
                response.errorBody()?.string()
            } catch (_: Exception) {
                null
            }
            Log.e(TAG, "API 请求失败: HTTP ${response.code()}, 响应体: ${errorBody ?: "无"}")
            NetworkResult.Error(
                code = response.code().toString(),
                message = "HTTP ${response.code()}: ${response.message()}"
            )
        }
    } catch (e: java.net.UnknownHostException) {
        NetworkResult.Error(message = "网络连接失败，请检查网络", throwable = e)
    } catch (e: java.net.SocketTimeoutException) {
        NetworkResult.Error(message = "请求超时，请稍后重试", throwable = e)
    } catch (e: Exception) {
        Log.e(TAG, "API 调用异常: ${e.message}", e)
        NetworkResult.Error(message = e.message ?: "未知错误", throwable = e)
    }
}
