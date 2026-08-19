package com.elderlycare.app

import android.app.Application
import com.elderlycare.app.data.ezviz.RtcSignalingManager
import com.elderlycare.app.data.ezviz.ServiceLocator

class ElderlyCareApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
        // 萤石 EZOpenSDK 初始化（云通话 ERTC + 直播/回放 + 留言模块都依赖它）。
        // 内部幂等，失败（模拟器/缺少 so）自动降级，不影响应用其他功能。
        ServiceLocator.sdkManager.init(this)
        // 已登录则注入 accessToken（供 ERTC 云通话信令/直播取流复用）
        ServiceLocator.tokenManager.getTokenForcefully()?.let {
            ServiceLocator.sdkManager.updateToken(it)
        }
        // 连接后端 WebSocket，接收设备来电
        RtcSignalingManager.connect("family001")
    }
}
