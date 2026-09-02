package com.elderlycare.app.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.elderlycare.app.R
import com.elderlycare.app.data.reminder.RemindPlanEntity
import java.util.Calendar

/**
 * 医院端复诊提醒的 App 本地通知调度（AlarmManager 精确闹钟）。
 *
 * 无论是否下发 RK3 设备播报，医院端复诊提醒都会落 remind_plan 表（source=1/2），
 * 到点由 [RemindAlarmReceiver] 弹本地通知 + 标记计划已播报完成 + 插一条系统消息
 * （家属端留言页/消息中心可见，替代本阶段未接入的后端推送）。
 *
 * 注意：进程被杀后闹钟会丢失——App 启动时（ElderlyCareApp.onCreate）调
 * [rescheduleAll] 兜底重调度。MIUI 等国产 ROM 需用户在系统设置放行自启动/省电限制。
 */
object LocalRemindScheduler {

    private const val TAG = "LocalRemindScheduler"

    const val CHANNEL_ID = "hospital_remind_channel"
    const val EXTRA_PLAN_ID = "extra_plan_id"
    const val EXTRA_PLAN_TAG = "extra_plan_tag"
    const val EXTRA_PLAN_CONTENT = "extra_plan_content"
    const val EXTRA_DEVICE_SERIAL = "extra_device_serial"

    /** 通知渠道（API 26+；幂等） */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        context.getString(R.string.hospital_remind_notification_channel),
                        NotificationManager.IMPORTANCE_HIGH
                    )
                )
            }
        }
    }

    /** 计划 → 触发时间戳（毫秒）；仅单次计划（复诊提醒）支持，时间已过返回 null */
    fun planTimeMillis(plan: RemindPlanEntity): Long? {
        if (plan.repeatType != RemindPlanEntity.REPEAT_ONCE || plan.year <= 0) return null
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, plan.year)
            set(Calendar.MONTH, plan.month - 1)
            set(Calendar.DAY_OF_MONTH, plan.day)
            set(Calendar.HOUR_OF_DAY, plan.timeHour)
            set(Calendar.MINUTE, plan.timeMin)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    /** 调度单条计划（requestCode = 本地计划 id；重复调度覆盖旧闹钟） */
    fun schedule(context: Context, plan: RemindPlanEntity) {
        if (plan.executed == RemindPlanEntity.EXECUTED_YES) return
        val timeMillis = planTimeMillis(plan)
            ?: run { Log.w(TAG, "无法调度：非单次计划 id=${plan.id}"); return }
        if (timeMillis <= System.currentTimeMillis()) {
            Log.w(TAG, "无法调度：提醒时间已过 id=${plan.id}")
            return
        }
        ensureChannel(context)
        val intent = Intent(context, RemindAlarmReceiver::class.java).apply {
            putExtra(EXTRA_PLAN_ID, plan.id)
            putExtra(EXTRA_PLAN_TAG, plan.tag)
            putExtra(EXTRA_PLAN_CONTENT, plan.content)
            putExtra(EXTRA_DEVICE_SERIAL, plan.deviceSerial)
        }
        val requestCode = (plan.id % 1_000_000).toInt()
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        runCatching {
            // 优先精确闹钟（省电模式照常触发）
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMillis, pending)
        }.onFailure { e ->
            // 无精确闹钟权限（SCHEDULE_EXACT_ALARM）→ 降级非精确（可能延迟数分钟）
            Log.w(TAG, "精确闹钟不可用，降级非精确调度: id=${plan.id}", e)
            runCatching {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMillis, pending)
            }
        }
        Log.i(TAG, "复诊提醒通知已调度: id=${plan.id} tag=${plan.tag} at=$timeMillis")
    }

    /** 取消单条计划的闹钟 */
    fun cancel(context: Context, planId: Long) {
        val intent = Intent(context, RemindAlarmReceiver::class.java)
        val requestCode = (planId % 1_000_000).toInt()
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        alarmManager.cancel(pending)
        pending.cancel()
    }

    /**
     * App 启动兜底：重调度全部医院端计划（进程被杀后闹钟丢失的恢复入口）。
     * 已播报完成/时间已过的取消闹钟；未到点且待播报的（重新）调度。
     */
    fun rescheduleAll(context: Context, plans: List<RemindPlanEntity>) {
        val now = System.currentTimeMillis()
        var scheduled = 0
        plans.forEach { plan ->
            val time = planTimeMillis(plan)
            val shouldFire = plan.executed != RemindPlanEntity.EXECUTED_YES &&
                time != null && time > now
            if (shouldFire) {
                schedule(context, plan)
                scheduled++
            } else {
                cancel(context, plan.id)
            }
        }
        Log.i(TAG, "复诊提醒通知重调度完成：共 ${plans.size} 条，待触发 $scheduled 条")
    }
}
