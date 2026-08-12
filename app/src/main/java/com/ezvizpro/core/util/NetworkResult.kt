package com.ezvizpro.core.util

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

/**
 * 将萤石 API 返回的 ApiResponse 转换为 NetworkResult
 * 包含详细的错误诊断日志，便于排查 JSON 解析问题
 */
suspend fun <T> apiCall(call: suspend () -> retrofit2.Response<com.ezvizpro.core.network.model.ApiResponse<T>>): NetworkResult<T> {
    return try {
        val response = call()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null && body.code == "200") {
                NetworkResult.Success(body.data!!)
            } else {
                NetworkResult.Error(
                    code = body?.code ?: "-1",
                    message = body?.msg ?: "未知错误"
                )
            }
        } else {
            // 尝试读取错误响应体用于诊断
            val errorBody = try {
                response.errorBody()?.string()
            } catch (_: Exception) {
                null
            }
            timber.log.Timber.e("API 请求失败: HTTP ${response.code()}, 响应体: ${errorBody ?: "无"}")
            NetworkResult.Error(
                code = response.code().toString(),
                message = "HTTP ${response.code()}: ${response.message()}"
            )
        }
    } catch (e: java.net.UnknownHostException) {
        NetworkResult.Error(message = "网络连接失败，请检查网络", throwable = e)
    } catch (e: java.net.SocketTimeoutException) {
        NetworkResult.Error(message = "请求超时，请稍后重试", throwable = e)
    } catch (e: kotlinx.serialization.SerializationException) {
        // JSON 解析失败：记录详细信息用于排查
        val errorMsg = e.message?.substringBefore('\n') ?: "未知格式错误"
        timber.log.Timber.tag("EzViz-Network").e("JSON 解析失败: $errorMsg")
        NetworkResult.Error(
            message = "数据解析失败: $errorMsg",
            throwable = e
        )
    } catch (e: Exception) {
        val errorMsg = e.message ?: "未知错误"
        timber.log.Timber.tag("EzViz-Network").e(e, "API 调用异常: $errorMsg")
        NetworkResult.Error(message = errorMsg, throwable = e)
    }
}
