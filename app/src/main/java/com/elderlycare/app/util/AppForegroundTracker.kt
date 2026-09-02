package com.elderlycare.app.util

import android.app.Activity
import android.app.Application
import android.os.Bundle

/**
 * 应用前后台判定（零依赖）：registerActivityLifecycleCallbacks 统计已 started 的
 * Activity 数量，>0 视为前台。
 *
 * 用途：WS 告警到达时，前台 toast 提醒「收到新告警」，后台只落库/角标不打扰。
 */
object AppForegroundTracker : Application.ActivityLifecycleCallbacks {

    @Volatile
    private var startedCount = 0

    val isForeground: Boolean
        get() = startedCount > 0

    fun init(app: Application) {
        app.registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityStarted(activity: Activity) {
        startedCount++
    }

    override fun onActivityStopped(activity: Activity) {
        if (startedCount > 0) startedCount--
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
