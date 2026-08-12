package com.ezvizpro

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class EzVizApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // 初始化 Timber 日志
        if (com.ezvizpro.BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // 验证萤石 AppKey 配置
        if (BuildConfig.EZVIZ_APP_KEY.isBlank() || BuildConfig.EZVIZ_APP_SECRET.isBlank()) {
            Timber.w("萤石 AppKey/AppSecret 未配置，请在 gradle.properties 中设置 EZVIZ_APP_KEY 和 EZVIZ_APP_SECRET")
        }
    }
}
