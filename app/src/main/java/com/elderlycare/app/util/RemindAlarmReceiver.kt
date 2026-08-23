package com.elderlycare.app.util

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.elderlycare.app.R
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.message.AppDatabase
import com.elderlycare.app.data.message.MessageEntity
import kotlinx.coroutines.runBlocking

/**
 * 医院端复诊提醒到点广播接收器（AlarmManager 触发）。
 *
 * 到点动作：①弹 App 本地通知；②标记 remind_plan 该条已播报完成；
 * ③插一条系统消息（MSG_TYPE_SYSTEM，家属端留言页/消息中心可见——
 * 本阶段后端推送未接入，本地消息作为替代）；④顺手清理已播报完成的
 * RK3 残留闹铃（复用 RemindPlanRepository.cleanExecutedDeviceClocks）。
 *
 * Room 写入走后台线程（Receiver.onReceive 主线程限制 ~10s，禁止主线程跑 suspend）。
 */
class RemindAlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "RemindAlarmReceiver"
        private const val NOTIFICATION_ID_BASE = 4000
    }

    override fun onReceive(context: Context, intent: Intent) {
        val planId = intent.getLongExtra(LocalRemindScheduler.EXTRA_PLAN_ID, -1L)
        if (planId <= 0) return
        val tag = intent.getStringExtra(LocalRemindScheduler.EXTRA_PLAN_TAG).orEmpty()
        val content = intent.getStringExtra(LocalRemindScheduler.EXTRA_PLAN_CONTENT).orEmpty()
        val deviceSerial = intent.getStringExtra(LocalRemindScheduler.EXTRA_DEVICE_SERIAL).orEmpty()

        Log.i(TAG, "复诊提醒到点: id=$planId tag=$tag content=$content")

        // ①本地通知（Android 13+ 需 POST_NOTIFICATIONS 运行时授权，未授权静默跳过）
        postNotification(context, planId, tag.ifBlank { context.getString(R.string.hospital_remind_title) }, content)

        // ②③④ Room 标记已播报 + 系统消息 + RK3 残留闹铃清理（后台线程）
        Thread {
            runBlocking {
                runCatching {
                    val db = AppDatabase.getInstance(context)
                    db.remindPlanDao().markExecuted(planId)
                    if (deviceSerial.isNotBlank()) {
                        val messageDao = db.messageDao()
                        val dedupeKey = "hospital_remind_$planId"
                        if (messageDao.getByRemoteId(dedupeKey) == null) {
                            messageDao.insert(
                                MessageEntity(
                                    msgType = MessageEntity.MSG_TYPE_SYSTEM,
                                    senderName = context.getString(R.string.hospital_remind_system_sender),
                                    content = context.getString(
                                        R.string.hospital_remind_system_msg_format,
                                        content.ifBlank { tag }
                                    ),
                                    createTime = System.currentTimeMillis(),
                                    isRead = false,
                                    deviceSerial = deviceSerial,
                                    remoteId = dedupeKey,
                                    sendStatus = MessageEntity.SEND_STATUS_SUCCESS,
                                    sendChannel = MessageEntity.CHANNEL_NONE
                                )
                            )
                        }
                    }
                    // RK3 残留闹铃清理（已播报完成的设备播报计划，复用 v3 deleteClocks）
                    ServiceLocator.reminderRepository.cleanExecutedDeviceClocks()
                }.onFailure { e ->
                    Log.w(TAG, "复诊提醒到点落库/清理失败: id=$planId", e)
                }
            }
        }.start()
    }

    /** 弹本地通知；点击通知打开应用（MainActivity） */
    private fun postNotification(context: Context, planId: Long, title: String, content: String) {
        runCatching {
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
            if (android.os.Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            LocalRemindScheduler.ensureChannel(context)
            val launchIntent = context.packageManager
                .getLaunchIntentForPackage(context.packageName) ?: return
            val contentIntent = PendingIntent.getActivity(
                context,
                (planId % 1_000_000).toInt(),
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(context, LocalRemindScheduler.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(
                    context.getString(
                        R.string.hospital_remind_notify_content,
                        content.ifBlank { title }
                    )
                )
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
            NotificationManagerCompat.from(context).notify(
                (NOTIFICATION_ID_BASE + (planId % 1_000_000)).toInt(),
                notification
            )
        }.onFailure { e -> Log.w(TAG, "复诊提醒通知发送失败: id=$planId", e) }
    }
}
