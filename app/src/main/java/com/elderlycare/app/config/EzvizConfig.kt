package com.elderlycare.app.config

import com.elderlycare.app.BuildConfig

/**
 * 萤石相关敏感配置集中管理。
 *
 * AppKey / AppSecret 已通过 gradle.properties 注入 BuildConfig（见 app/build.gradle.kts），
 * 此处统一出口，避免散落硬编码。
 */
object EzvizConfig {

    /**
     * 萤石开放平台 AppKey。
     * 用户需在 https://open.ys7.com 创建应用后，把 AppKey 填入项目根目录 gradle.properties 的
     * EZVIZ_APP_KEY 字段；若 BuildConfig 为空则回退到下方占位，用户需填入。
     */
    // TODO(用户需填入): 若 gradle.properties 未配置，请把 AppKey 写在这里
    const val APP_KEY_FALLBACK: String = ""

    /** 实际使用的 AppKey：优先读 BuildConfig（gradle.properties 注入） */
    val APP_KEY: String
        get() = BuildConfig.EZVIZ_APP_KEY.ifBlank { APP_KEY_FALLBACK }

    /** 萤石开放平台 AppSecret（仅 REST 取 token 用，SDK 不需要） */
    val APP_SECRET: String
        get() = BuildConfig.EZVIZ_APP_SECRET

    /**
     * 云广播 REST 接口基地址。
     * 官方文档：https://icnopen.ezviz.com/help/1430
     * TODO(用户需确认): 老版接口为 open.ys7.com；新版接口域名为 icnopen.ezviz.com，
     *  按文档确认你开通的云广播服务用哪个域名。
     */
    const val BROADCAST_BASE_URL: String = "https://open.ys7.com/"

    /**
     * accessToken：复用 TokenManager 动态获取（见 data/ezviz/TokenManager.kt），
     * 此处仅作为兜底占位，正常情况下不直接使用。
     * TODO(用户需填入): 调试时如需固定 token，可临时填在此处（切勿提交到仓库）
     */
    const val ACCESS_TOKEN_PLACEHOLDER: String = ""
}
