package com.elderlycare.app.data.message

import android.content.Context
import android.media.MediaMetadataRetriever
import android.util.Log
import java.io.File

/**
 * 留言音频文件管理。
 * 全部音频统一存放在应用私有目录 getFilesDir()/messages/ 下：
 * - dev_*  设备留言下载的音频（容器格式由云端内容决定，不带扩展名，靠 ExoPlayer 自动识别）
 * - rec_*  按住录音留言文件（16kHz/16bit 单声道 WAV，云广播上传格式）
 * 注：文字留言 TTS 在云端完成，App 不落盘 TTS 文件。
 */
object MessageFiles {

    private const val TAG = "MessageFiles"

    /** 留言音频目录（不存在时自动创建） */
    fun messageDir(context: Context): File =
        File(context.filesDir, "messages").apply {
            if (!exists() && !mkdirs()) {
                Log.w(TAG, "创建留言音频目录失败: $absolutePath")
            }
        }

    /** 生成录音留言文件路径（16kHz/16bit 单声道 WAV） */
    fun newRecordFile(context: Context): File =
        File(messageDir(context), "rec_${System.currentTimeMillis()}.wav")

    /** 生成设备留言下载文件路径（不带扩展名，内容可能是 mp3/aac） */
    fun newDeviceFile(context: Context, msgId: String): File =
        File(messageDir(context), "dev_$msgId")

    /** 静默删除文件 */
    fun deleteQuietly(file: File) {
        try {
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            Log.w(TAG, "删除文件失败: ${file.absolutePath}", e)
        }
    }

    /**
     * 读取本地音频时长（秒）。
     * 优先用 MediaMetadataRetriever（适配 wav/mp3/aac/m4a 等格式）；
     * 失败时按 WAV 16kHz 16bit 单声道字节数兜底估算。
     */
    fun audioDurationSec(file: File): Int {
        if (!file.exists()) return 0
        try {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(file.absolutePath)
                val ms = retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L
                if (ms > 0) return (ms / 1000L).toInt()
            } finally {
                runCatching { retriever.release() }
            }
        } catch (e: Exception) {
            Log.w(TAG, "读取音频时长失败，改用文件大小估算", e)
        }
        // 兜底：WAV 头 44 字节 + PCM 32000 字节/秒
        val dataBytes = file.length() - 44
        return if (dataBytes > 0) ((dataBytes.toDouble() / 32000.0) + 0.5).toInt() else 0
    }
}
