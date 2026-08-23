package com.elderlycare.app.ui.ezviz

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.provider.MediaStore
import android.util.Log
import android.view.SurfaceHolder
import com.videogo.errorlayer.ErrorInfo
import com.videogo.openapi.EZConstants
import com.videogo.openapi.EZOpenSDK
import com.videogo.openapi.EZPlayer
import java.io.File

/**
 * 预览页手机本地录制 + 图片/录像落相册（EZOpenSDK 5.28.4 本地能力）。
 *
 * ⚠️ 边界区分：
 * - 这是手机本地录制，不是设备 SD 卡回放下载（勿与回放搜索/下载接口混淆）；
 * - 手动抓拍已改走「App → 后端 → 萤石 device/capture」云端链路（CaptureRepository），
 *   本文件不再提供本地抓帧（capturePicture/captureCamera 均已下线，禁播放器截屏）；
 * - 与点播/广播FM 无关（那是萤石私有未公开接口，另走占位网络层）。
 *
 * 实现要点（API 签名均已 javap 反编译 5.28.4 aar 确认，勿凭文档猜测）：
 * - 录制：隐藏 EZPlayer 会话（1x1 SurfaceView 提供解码 surface）→ startLocalRecordWithFile
 *   本地落 MP4 → 停止后入库系统相册。隐藏会话与预览页 ExoPlayer/H5 播放互不影响，
 *   页面销毁/预览断开必须 stopRealPlay + stopLocalRecord，防止文件损坏。
 */
object LocalMediaCapture {

    private const val TAG = "LocalMediaCapture"

    // ==================== 相册入库 ====================

    /** 图片写入系统相册（API≥29 走 MediaStore 免权限；API≤28 写公共 Pictures 目录，需调用方先申请存储权限） */
    fun saveImageToGallery(context: Context, file: File, prefix: String): String {
        val appContext = context.applicationContext
        val fileName = "${prefix}_${System.currentTimeMillis()}.jpg"
        val bytes = file.readBytes()
        return if (Build.VERSION.SDK_INT >= 29) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ElderlyCare")
            }
            val resolver = appContext.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw Exception("截图保存失败，请检查存储空间")
            resolver.openOutputStream(uri)?.use { it.write(bytes) }
                ?: throw Exception("截图保存失败，请检查存储空间")
            uri.toString()
        } else {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            if (!dir.exists() && !dir.mkdirs()) throw Exception("截图保存失败，请检查存储空间")
            val target = File(dir, fileName)
            target.writeBytes(bytes)
            target.absolutePath
        }
    }

    // ==================== 录制会话 ====================

    /**
     * 隐藏 EZPlayer 录制会话。
     *
     * 生命周期：
     * start（createPlayer + setPlayVerifyCode + setSurfaceHold + setHandler + startRealPlay）
     *  → 播放成功回调（MSG_REALPLAY_PLAY_SUCCESS=102）→ startLocalRecordWithFile 本地落 MP4
     *  → stop：stopLocalRecord + stopRealPlay + releasePlayer → 录像入库系统相册。
     * 任何一步失败/超时都会回收会话并回调 [Listener.onRecordFailed]。
     *
     * ⚠️ 消息码取自 EZConstants.EZRealPlayConstants（官方 demo 同款消息协议），
     * 播放成功回调必须收到后才开始录像，避免生成空文件。
     */
    class RecordSession(private val context: Context) {

        interface Listener {
            fun onRecordStarted()
            fun onRecordFailed(friendlyMessage: String)
        }

        private val mainHandler = Handler(Looper.getMainLooper())
        private var player: EZPlayer? = null
        private var listener: Listener? = null
        private var recordFile: File? = null
        private var recordStarted = false
        private var released = false

        /** 播放成功 20s 未到则判定失败，避免会话悬挂 */
        private val timeoutRunnable = Runnable { fail("设备连接超时，录制失败，请重试") }

        private val playHandler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                if (released) return
                when (msg.what) {
                    EZConstants.EZRealPlayConstants.MSG_REALPLAY_PLAY_SUCCESS -> {
                        mainHandler.removeCallbacks(timeoutRunnable)
                        beginLocalRecord()
                    }
                    EZConstants.EZRealPlayConstants.MSG_REALPLAY_PLAY_FAIL,
                    EZConstants.EZRealPlayConstants.MSG_START_RECORD_FAIL -> {
                        val code = (msg.obj as? ErrorInfo)?.errorCode ?: -1
                        fail(friendlyPlayError(code))
                    }
                    // 加密/密码错误：验证码相关
                    EZConstants.EZRealPlayConstants.MSG_REALPLAY_PASSWORD_ERROR,
                    EZConstants.EZRealPlayConstants.MSG_REALPLAY_ENCRYPT_PASSWORD_ERROR -> {
                        fail("设备已开启视频加密，请确认验证码正确（设备标签上的6位大写字母）")
                    }
                    EZConstants.EZRealPlayConstants.MSG_START_RECORD_SUCCESS -> {
                        // 双保险：startLocalRecordWithFile 已置状态，此处幂等
                        recordStarted = true
                    }
                }
            }
        }

        /**
         * 启动录制会话（主线程调用）。
         * 返回 true 表示会话已发起（真实结果异步经 Listener 回调）。
         */
        fun start(
            deviceSerial: String,
            channelNo: Int,
            verifyCode: String,
            holder: SurfaceHolder,
            listener: Listener
        ): Boolean {
            if (released) return false
            this.listener = listener
            return try {
                val p = EZOpenSDK.getInstance().createPlayer(deviceSerial, channelNo)
                if (p == null) {
                    Log.e(TAG, "createPlayer 返回 null")
                    return false
                }
                player = p
                if (verifyCode.length == 6) p.setPlayVerifyCode(verifyCode)
                p.setSurfaceHold(holder)
                p.setHandler(playHandler)
                val ok = p.startRealPlay()
                if (!ok) {
                    fail("录制启动失败，请重试")
                    return false
                }
                mainHandler.postDelayed(timeoutRunnable, PLAY_TIMEOUT_MS)
                true
            } catch (e: Exception) {
                Log.e(TAG, "启动录制会话异常", e)
                fail("录制启动失败，请重试")
                false
            }
        }

        private fun beginLocalRecord() {
            val p = player ?: return
            try {
                // 隐藏会话只录流不重复外放声音（预览页已在播放设备声音）
                p.closeSound()
                val file = File(
                    context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir,
                    "rec_tmp_${System.currentTimeMillis()}.mp4"
                ).apply { parentFile?.mkdirs() }
                recordFile = file
                val ok = p.startLocalRecordWithFile(file.absolutePath)
                if (!ok) {
                    fail("录制启动失败，请重试")
                    return
                }
                recordStarted = true
                listener?.onRecordStarted()
            } catch (e: Exception) {
                Log.e(TAG, "启动本地录制异常", e)
                fail("录制启动失败，请重试")
            }
        }

        /** 会话是否已回收（停止/失败后为 true，供调用方区分「创建失败」与「被停止打断」） */
        fun isReleased(): Boolean = released

        /**
         * 停止录制并落库到系统相册（可在 IO 线程调用）。
         * 返回已保存的录像位置；会话未真正录像（无数据）时返回 null。
         */
        fun stop(): String? {
            if (released) return null
            released = true
            mainHandler.removeCallbacks(timeoutRunnable)
            val p = player
            val file = recordFile
            val hasData = recordStarted && file != null && file.exists() && file.length() > 0
            try {
                if (p != null) {
                    if (recordStarted) p.stopLocalRecord()
                    p.stopRealPlay()
                }
            } catch (e: Exception) {
                Log.e(TAG, "停止录制会话异常", e)
            }
            try {
                if (p != null) EZOpenSDK.getInstance().releasePlayer(p)
            } catch (e: Exception) {
                Log.e(TAG, "释放录制播放器异常", e)
            }
            player = null
            if (!hasData) {
                file?.delete()
                return null
            }
            val temp = file ?: return null
            return try {
                saveVideoToGallery(context, temp)
            } catch (e: Exception) {
                Log.e(TAG, "录像入库相册失败", e)
                temp.absolutePath // 相册入库失败时保留应用目录文件，避免用户数据丢失
            }
        }

        /** 会话失败回收（不落库） */
        private fun fail(msg: String) {
            if (released) return
            Log.e(TAG, "录制会话失败: $msg")
            released = true
            mainHandler.removeCallbacks(timeoutRunnable)
            val p = player
            player = null
            try {
                if (p != null) {
                    if (recordStarted) p.stopLocalRecord()
                    p.stopRealPlay()
                }
            } catch (e: Exception) {
                Log.e(TAG, "失败清理异常", e)
            }
            try {
                if (p != null) EZOpenSDK.getInstance().releasePlayer(p)
            } catch (e: Exception) {
                Log.e(TAG, "失败释放播放器异常", e)
            }
            recordFile?.delete()
            recordFile = null
            listener?.onRecordFailed(msg)
        }

        private fun friendlyPlayError(code: Int): String = when (code) {
            60019 -> "设备已开启视频加密，请确认验证码正确（设备标签上的6位大写字母）"
            60020 -> "设备不在线，请确认设备已开机并连接网络后重试"
            10002 -> "登录凭证已过期，请重新登录后再试"
            -1 -> "录制启动失败，请重试"
            else -> "录制失败（错误码：$code）"
        }

        private companion object {
            const val PLAY_TIMEOUT_MS = 20_000L
        }
    }

    /**
     * 录像写入系统相册（API≥29 走 MediaStore Movies；API≤28 移动到公共 Movies 目录）。
     * 成功返回保存位置并删除临时文件。
     */
    fun saveVideoToGallery(context: Context, temp: File): String {
        val appContext = context.applicationContext
        val fileName = "record_${System.currentTimeMillis()}.mp4"
        return if (Build.VERSION.SDK_INT >= 29) {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/ElderlyCare")
            }
            val resolver = appContext.contentResolver
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw Exception("录像保存失败，请检查存储空间")
            resolver.openOutputStream(uri)?.use { out ->
                temp.inputStream().use { it.copyTo(out) }
            } ?: throw Exception("录像保存失败，请检查存储空间")
            temp.delete()
            uri.toString()
        } else {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            if (!dir.exists() && !dir.mkdirs()) throw Exception("录像保存失败，请检查存储空间")
            val target = File(dir, fileName)
            if (!temp.renameTo(target)) {
                temp.copyTo(target, overwrite = true)
                temp.delete()
            }
            target.absolutePath
        }
    }
}
