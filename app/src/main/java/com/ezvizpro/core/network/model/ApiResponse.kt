package com.ezvizpro.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 萤石开放平台 API 统一返回格式
 * {
 *   "code": "200",
 *   "msg": "操作成功",
 *   "data": { ... },
 *   "page": { "total": 100, "page": 1, "size": 10 }
 * }
 */
@Serializable
data class ApiResponse<T>(
    val code: String,
    val msg: String,
    val data: T? = null,
    val page: PageInfo? = null
)

@Serializable
data class PageInfo(
    val total: Int = 0,
    val page: Int = 0,
    val size: Int = 0
)

/**
 * 判断 API 返回是否成功
 */
fun <T> ApiResponse<T>.isSuccess(): Boolean = code == "200"

/**
 * 萤石 API 常见错误码
 */
object EzvizErrorCode {
    const val SUCCESS = "200"
    const val TOKEN_EXPIRED = "10002"
    const val TOKEN_INVALID = "10012"
    const val DEVICE_OFFLINE = "20010"
    const val DEVICE_NOT_EXIST = "20002"
    const val PERMISSION_DENIED = "20004"
    const val RATE_LIMIT = "10005"
    const val PARAM_ERROR = "10001"
}
