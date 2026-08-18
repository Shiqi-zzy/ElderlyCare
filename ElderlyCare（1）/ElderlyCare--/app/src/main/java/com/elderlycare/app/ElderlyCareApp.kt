package com.elderlycare.app

import android.app.Application
import com.elderlycare.app.data.ezviz.ServiceLocator

class ElderlyCareApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
        // 萤石 EZOpenSDK 初始化（留言模块依赖）。
        // 失败（如模拟器/未配置 AppKey）自动降级，不影响应用其他功能。
        ServiceLocator.sdkManager.init(this)
    }
}
