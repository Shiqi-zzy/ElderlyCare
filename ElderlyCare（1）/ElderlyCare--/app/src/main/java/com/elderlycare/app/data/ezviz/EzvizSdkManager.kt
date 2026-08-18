package com.elderlycare.app.data.ezviz

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.elderlycare.app.config.EzvizConfig
import com.videogo.openapi.EZConstants
import com.videogo.openapi.EZOpenSDK
import com.videogo.openapi.EZOpenSDKListener
import com.videogo.openapi.bean.EZLeaveMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/**
 * EZOpenSDK 封装（留言模块）。
 *
 * 职责：
 * 1. SDK 初始化（initLib / setAccessToken），所有调用带异常保护（模拟器 / 缺少 so 时会失败，不影响 App 其他功能）；
 * 2. 设备留言（微聊）接收侧 API：列表拉取 / 音频数据流下载 / 标记已读 / 删除 / 未读数；
 * 3. App → 设备的实时语音通话由 [VoiceCallSession]（videotalk 模块）负责。
 *
 * 注意：
 * - EZOpenSDK 不支持模拟器，仅真机可用；
 * - 所有 SDK 调用统一切到后台线程执行；
 * - SDK 的「微聊留言」只有接收侧 API（拉取列表/下载音频），没有主动发留言的接口，
 *   App 发留言走 [VoiceCallSession]（实时语音）+ 云广播 REST（文件下发）双通路。
 */
class EzvizSdkManager {

    companion object {
        private const val TAG = "EzvizSdkManager"

        // 留言数据流回调状态码（官方示例语义）
        // TODO(用户需确认): 对照官方文档核实各状态值，若不一致只需改这里
        private const val FLOW_STATE_START = 1   // 开始
        private const val FLOW_STATE_DATA = 2    // 数据
        private const val FLOW_STATE_FINISH = 3  // 完成
        private const val FLOW_STATE_ERROR = 4   // 失败

        /** 留言数据流下载超时（60s，音频 ≤60s + 余量） */
        private const val DOWNLOAD_TIMEOUT_MS = 60_000L
    }

    @Volatile
    private var initialized = false

    /**
     * 初始化 EZOpenSDK。必须在 Application.onCreate 中调用。
     * 返回 false 表示初始化失败（AppKey 未配置 / 缺少 so / 模拟器），
     * 之后所有 SDK 调用自动降级为空操作，不影响应用其他功能。
     */
    fun init(app: Context): Boolean {
        if (initialized) return true
        val appKey = EzvizConfig.APP_KEY
        if (appKey.isBlank()) {
            Log.e(TAG, "AppKey 未配置，跳过 EZOpenSDK 初始化（请在 gradle.properties 配置 EZVIZ_APP_KEY）")
            return false
        }
        return try {
            val ok = EZOpenSDK.initLib(app.applicationContext as Application, appKey)
            initialized = ok
            Log.i(TAG, "EZOpenSDK 初始化结果: $ok")
            ok
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "EZOpenSDK 初始化失败（so 加载失败，模拟器不支持）", e)
            false
        } catch (e: NoClassDefFoundError) {
            Log.e(TAG, "EZOpenSDK 初始化失败（类加载失败）", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "EZOpenSDK 初始化失败", e)
            false
        }
    }

    /** 是否初始化成功 */
    fun isInitialized(): Boolean = initialized

    /**
     * 注入 accessToken（复用 EzvizRepository 的登录态）。
     * 每次 SDK 调用前刷新一次；token 过期时由 EzvizRepository 自动重新获取。
     */
    fun updateToken(token: String?) {
        if (!initialized || token.isNullOrBlank()) return
        try {
            EZOpenSDK.getInstance().setAccessToken(token)
        } catch (e: Exception) {
            Log.e(TAG, "setAccessToken 失败", e)
        }
    }

    // ==================== 设备留言（微聊）接收侧 ====================

    /**
     * 拉取设备留言列表（默认近 7 天、最多 50 条）。
     * 失败返回空列表（不抛异常，调用方可提示「同步失败」）。
     */
    suspend fun getLeaveMessageList(
        deviceSerial: String,
        pageStart: Int = 0,
        pageSize: Int = 50,
        startTime: Calendar,
        endTime: Calendar
    ): List<EZLeaveMessage> = withContext(Dispatchers.IO) {
        if (!initialized) return@withContext emptyList()
        try {
            EZOpenSDK.getInstance()
                .getLeaveMessageList(deviceSerial, pageStart, pageSize, startTime, endTime)
                ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "获取设备留言列表失败", e)
            emptyList()
        }
    }

    /** 获取设备留言未读数（微聊） */
    suspend fun getUnreadLeaveMessageCount(deviceSerial: String): Int = withContext(Dispatchers.IO) {
        if (!initialized) return@withContext 0
        try {
            EZOpenSDK.getInstance()
                .getUnreadMessageCount(deviceSerial, EZConstants.EZMessageType.EZMessageTypeLeave)
        } catch (e: Exception) {
            Log.e(TAG, "获取留言未读数失败", e)
            0
        }
    }

    /** 云端标记设备留言已读 */
    suspend fun markLeaveMessageRead(msgIds: List<String>): Boolean = withContext(Dispatchers.IO) {
        if (!initialized || msgIds.isEmpty()) return@withContext false
        try {
            EZOpenSDK.getInstance()
                .setLeaveMessageStatus(msgIds, EZConstants.EZMessageStatus.EZMessageStatusRead)
        } catch (e: Exception) {
            Log.e(TAG, "标记留言已读失败", e)
            false
        }
    }

    /** 云端删除设备留言 */
    suspend fun deleteLeaveMessages(msgIds: List<String>): Boolean = withContext(Dispatchers.IO) {
        if (!initialized || msgIds.isEmpty()) return@withContext false
        try {
            EZOpenSDK.getInstance().deleteLeaveMessages(msgIds)
        } catch (e: Exception) {
            Log.e(TAG, "删除设备留言失败", e)
            false
        }
    }

    /**
     * 通过 SDK 流式接口下载留言音频数据（云广播 URL 下载失败时的兜底通路）。
     *
     * 回调状态：1=开始 2=数据 3=完成 4=失败（见 FLOW_STATE_* 常量）。
     * 回调统一先落到后台单线程队列再写文件，避免阻塞主线程；60s 超时视为失败。
     */
    suspend fun downloadLeaveMessageData(message: EZLeaveMessage, target: File): Boolean {
        if (!initialized) return false
        val result = withTimeoutOrNull(DOWNLOAD_TIMEOUT_MS) {
            suspendCancellableCoroutine<Boolean> { cont ->
                // 文件写入放到后台单线程队列，回调线程只负责投递
                val executor = Executors.newSingleThreadExecutor()
                val resumed = AtomicBoolean(false)
                var output: FileOutputStream? = null

                fun closeQuietly() {
                    try {
                        output?.flush()
                        output?.close()
                    } catch (e: Exception) {
                        Log.w(TAG, "关闭留言音频文件失败", e)
                    }
                    output = null
                }

                fun complete(success: Boolean) {
                    if (resumed.compareAndSet(false, true)) {
                        closeQuietly()
                        executor.shutdown()
                        cont.resume(success)
                    }
                }

                val callback = object : EZOpenSDKListener.EZLeaveMessageFlowCallback {
                    override fun onLeaveMessageFlowCallback(
                        state: Int,
                        data: ByteArray?,
                        type: Int,
                        error: String?
                    ) {
                        when (state) {
                            FLOW_STATE_START -> executor.execute {
                                try {
                                    output = FileOutputStream(target)
                                } catch (e: Exception) {
                                    Log.e(TAG, "创建留言音频文件失败", e)
                                    complete(false)
                                }
                            }
                            FLOW_STATE_DATA -> executor.execute {
                                try {
                                    data?.let { output?.write(it) }
                                } catch (e: Exception) {
                                    Log.e(TAG, "写入留言音频失败", e)
                                    complete(false)
                                }
                            }
                            FLOW_STATE_FINISH -> executor.execute { complete(true) }
                            else -> {
                                Log.e(TAG, "留言音频下载失败: state=$state error=$error")
                                executor.execute { complete(false) }
                            }
                        }
                    }
                }

                // 清理：协程被取消时释放资源
                cont.invokeOnCancellation {
                    complete(false)
                }

                try {
                    // 回调投递到主线程 Handler（官方示例用法），实际文件写入在后台线程
                    EZOpenSDK.getInstance()
                        .getLeaveMessageData(Handler(Looper.getMainLooper()), message, callback)
                } catch (e: Exception) {
                    Log.e(TAG, "启动留言音频下载失败", e)
                    complete(false)
                }
            }
        }
        return result == true
    }
}
