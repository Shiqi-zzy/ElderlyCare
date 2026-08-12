package com.ezvizpro.domain.repository

import com.ezvizpro.core.util.NetworkResult

/**
 * 认证仓库接口
 */
interface AuthRepository {

    /**
     * 使用萤石账号密码登录
     * 通过萤石开放平台 API 获取 accessToken
     */
    suspend fun login(appKey: String, appSecret: String): NetworkResult<String>

    /**
     * 检查当前 Token 是否有效
     */
    suspend fun isLoggedIn(): Boolean

    /**
     * 退出登录，清除本地 Token
     */
    suspend fun logout()
}
