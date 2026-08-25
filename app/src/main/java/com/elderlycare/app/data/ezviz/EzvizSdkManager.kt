package com.elderlycare.app.data.ezviz

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.elderlycare.app.BuildConfig
import com.elderlycare.app.config.EzvizConfig
import com.videogo.exception.BaseException
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
 * 2. 设备留言（微聊）接收侧 API：列表拉取 / 音频数据流下载 / 标记已读 / 删除 / 未读数。
 *
 * 注意：
 * - EZOpenSDK 不支持模拟器，仅真机可用；
 * - 所有 SDK 调用统一切到后台线程执行；
 * - SDK 的「微聊留言」只有接收侧 API（拉取列表/下载音频），没有主动发留言的接口，
 *   App 发留言只走云广播 REST（文件下发）单通路；语音通话/对讲通路已废弃
 *   （RK3 不支持对讲，EZOpenSDK 4168：isSupportTalk=0）。
 */
class EzvizSdkManager {

    companion object {
        private const val TAG = "EzvizSdkManager"

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
            // 云通话（ERTC）/ 播放器 P2P 所需的 SDK 预配置（与官方云通话 Demo 的初始化顺序一致）
            EZOpenSDK.showSDKLog(BuildConfig.DEBUG)
            EZOpenSDK.setDebugStreamEnable(false)
            EZOpenSDK.enableP2P(true)
            EZOpenSDK.enableSDKWithTKToken(false) // accessToken 认证模式
            val ok = EZOpenSDK.initLib(app.applicationContext as Application, appKey)
            initialized = ok
            if (ok) {
                // 注入缓存的 accessToken（SDK 留言/对讲/通话接口依赖；过期由仓库层自动刷新）
                ServiceLocator.tokenManager.getTokenForcefully()?.let {
                    EZOpenSDK.getInstance().setAccessToken(it)
                }
            }
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

    // ==================== 云台控制 ====================

    /**
     * 云台控制（EZOpenSDK 5.28.4，javap 实证签名 controlPTZ(String,int,EZPTZCommand,EZPTZAction,int)）。
     * START=开始转动（按住期间设备持续动作），STOP=停止（松开必须成对调用）。
     * 返回 false = SDK 未初始化 / 设备不支持云台 / 调用异常（调用方 toast「云台操作失败」）。
     */
    suspend fun controlPtz(
        deviceSerial: String,
        channelNo: Int,
        command: EZConstants.EZPTZCommand,
        action: EZConstants.EZPTZAction,
        speed: Int = EZConstants.PTZ_SPEED_DEFAULT
    ): Boolean = withContext(Dispatchers.IO) {
        if (!initialized) return@withContext false
        try {
            EZOpenSDK.getInstance().controlPTZ(deviceSerial, channelNo, command, action, speed)
        } catch (e: BaseException) {
            Log.e(TAG, "云台控制失败 code=${e.errorCode}", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "云台控制异常", e)
            false
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
     * 通过 SDK 流式接口下载留言音频数据（cloudServerUrl 直连下载失败时的兜底通路）。
     *
     * ⚠️ 5.28.4 实测回调语义（javap 反编译 EZStreamDownload.onDataCallBack 确认，
     * 与「1=开始/2=数据/3=完成/4=失败」的旧文档示例不同，勿按状态机处理）：
     * - SDK 内部用 ByteArrayOutputStream 累积数据块；
     * - 流结束（STREAM_TYPE_END）时**一次性**回调完整音频：
     *   onLeaveMessageFlowCallback(state=contentType, data=完整byte[], type=data长度, error=msgId)；
     * - 即：state 传的是 contentType（1=语音/2=视频），不是进度状态；
     *   回调只来一次，data 就是整个音频文件字节。
     *
     * 处理：收到一次回调 → 校验 data 非空 → 后台线程一次性写入 target 文件 → 完成。
     * 60s 超时或 data 为空视为失败。
     */
    suspend fun downloadLeaveMessageData(message: EZLeaveMessage, target: File): Boolean {
        if (!initialized) return false
        val result = withTimeoutOrNull(DOWNLOAD_TIMEOUT_MS) {
            suspendCancellableCoroutine<Boolean> { cont ->
                // 文件写入放到后台单线程队列，回调线程（主线程 Handler）只负责投递
                val executor = Executors.newSingleThreadExecutor()
                val resumed = AtomicBoolean(false)

                fun complete(success: Boolean) {
                    if (resumed.compareAndSet(false, true)) {
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
                        // 实测语义：state=contentType，data=完整音频字节，一次回调
                        if (data == null || data.isEmpty()) {
                            Log.e(
                                TAG,
                                "留言音频数据为空: state=$state len=$type error=$error msgId=${message.msgId}"
                            )
                            complete(false)
                            return
                        }
                        executor.execute {
                            try {
                                FileOutputStream(target).use { it.write(data) }
                                Log.i(TAG, "留言音频下载成功: ${data.size} 字节 -> ${target.name}")
                                complete(true)
                            } catch (e: Exception) {
                                Log.e(TAG, "写入留言音频文件失败", e)
                                complete(false)
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
