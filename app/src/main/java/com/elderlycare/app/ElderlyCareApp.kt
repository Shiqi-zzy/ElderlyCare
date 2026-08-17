package com.elderlycare.app

import android.app.Application
import com.elderlycare.app.data.ezviz.RtcSignalingManager
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.videogo.openapi.EZOpenSDK

class ElderlyCareApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
        initEzvizOpenSDK()
        // 连接后端 WebSocket，接收设备来电
        RtcSignalingManager.connect("family001")
    }

    /**
     * 初始化萤石 EZOpenSDK（云通话信令 + 直播/回放取流依赖它）。
     * 参考萤石官方 demo 的 SdkInitTool。
     */
    private fun initEzvizOpenSDK() {
        val appKey = BuildConfig.EZVIZ_APP_KEY.takeIf { it.isNotBlank() } ?: return

        // 以下须在 initLib 之前调用
        EZOpenSDK.showSDKLog(BuildConfig.DEBUG)
        EZOpenSDK.setDebugStreamEnable(false) // 生产关闭，避免缓存调试文件
        EZOpenSDK.enableP2P(true)
        EZOpenSDK.enableSDKWithTKToken(false) // accessToken 模式
        EZOpenSDK.initLib(this, appKey)

        // 已登录则设置 accessToken
        ServiceLocator.tokenManager.getTokenForcefully()?.let {
            EZOpenSDK.getInstance().setAccessToken(it)
        }
    }
}
