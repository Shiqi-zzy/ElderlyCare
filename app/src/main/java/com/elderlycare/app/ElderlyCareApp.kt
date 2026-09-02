package com.elderlycare.app

import android.app.Application
import com.elderlycare.app.data.ezviz.RtcSignalingManager
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.ui.shared.AuthorizedSnsProvider
import com.elderlycare.app.util.AppForegroundTracker
import com.elderlycare.app.util.LocalRemindScheduler
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

        // 前后台判定（WS 告警前台 toast 用）
        AppForegroundTracker.init(this)

        // 云通话：连接自建信令后端 WebSocket（接收 RK3 来电推送 + 告警实时推送）
        RtcSignalingManager.init(this)
        RtcSignalingManager.connect("family001")
        // 告警 WS 订阅上报：仅家属端上报已授权设备 SN（授权变更实时重发）；
        // 医院/社区员工发空列表清订阅（不实时收告警，消息中心 60s 轮询兜底）
        appScope.launch {
            runCatching {
                AuthorizedSnsProvider.flow().collect { sns ->
                    val family = ServiceLocator.userStore.getCurrentUserId() != null
                    RtcSignalingManager.updateAuthorizedSns(if (family) sns else emptySet())
                }
            }
        }
        // 冷启动刷新 token：SDK 初始化时已注入缓存值，这里异步拉新防止缓存已过期
        appScope.launch {
            runCatching {
                ServiceLocator.repository.obtainValidToken()?.let {
                    ServiceLocator.sdkManager.updateToken(it)
                }
            }
        }

        // 医院端复诊提醒本地通知兜底重调度（进程被杀后 AlarmManager 闹钟丢失的恢复入口；
        // 已播报完成/时间已过的自动取消）
        appScope.launch {
            runCatching {
                LocalRemindScheduler.rescheduleAll(
                    this@ElderlyCareApp,
                    ServiceLocator.reminderRepository.getAllHospitalPlans()
                )
            }
        }
    }
}
