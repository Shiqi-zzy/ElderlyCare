package com.elderlycare.app

import android.app.Application
import com.elderlycare.app.data.ezviz.RtcSignalingManager
import com.elderlycare.app.data.ezviz.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ElderlyCareApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
        // 萤石 EZOpenSDK 初始化（留言模块依赖）。
        // 失败（如模拟器/未配置 AppKey）自动降级，不影响应用其他功能。
        ServiceLocator.sdkManager.init(this)

        // 云通话：连接自建信令后端 WebSocket（接收 RK3 来电推送）
        RtcSignalingManager.connect("family001")
        // 冷启动刷新 token：SDK 初始化时已注入缓存值，这里异步拉新防止缓存已过期
        appScope.launch {
            runCatching {
                ServiceLocator.repository.obtainValidToken()?.let {
                    ServiceLocator.sdkManager.updateToken(it)
                }
            }
        }
    }
}
