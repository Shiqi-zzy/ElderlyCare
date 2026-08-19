package com.elderlycare.app.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/**
 * 文字转语音工具（留言模块：文字留言 → WAV 音频）。
 *
 * 注意：
 * - TTS 初始化必须在主线程（部分 ROM 要求），合成文件可在后台等待回调；
 * - 合成超时按最长 60s 兜底（80 字正常语速约 40s）；
 * - 若 TTS 引擎不可用，调用方会记录发送失败，不影响其他通路。
 */
class TtsHelper(private val context: Context) {

    companion object {
        private const val TAG = "TtsHelper"

        /** 单次合成最长等待（80 字 ≈ 40s 语音，超时视为失败） */
        private const val SYNTH_TIMEOUT_MS = 60_000L
    }

    private var tts: TextToSpeech? = null
    private var ready = false
    private val initLock = Mutex()

    /** 初始化（幂等，主线程调用）。返回是否可用 */
    private suspend fun ensureInit(): Boolean = initLock.withLock {
        if (ready) return true
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { cont ->
                if (tts == null) {
                    tts = TextToSpeech(context.applicationContext) { status ->
                        val ok = status == TextToSpeech.SUCCESS
                        if (ok) {
                            val result = tts?.setLanguage(Locale.CHINA)
                                ?: TextToSpeech.LANG_MISSING_DATA
                            ready = result != TextToSpeech.LANG_MISSING_DATA &&
                                result != TextToSpeech.LANG_NOT_SUPPORTED
                        }
                        cont.resume(ready)
                    }
                } else {
                    cont.resume(ready)
                }
            }
        }
    }

    /**
     * 把文字合成为 WAV 文件。
     * @param text   留言文本（UI 已限制 80 字）
     * @param target 输出文件（.wav 后缀，引擎将输出 WAV 格式）
     * @return 成功返回音频时长（秒）；失败返回 null
     */
    suspend fun synthesizeToFile(text: String, target: File): Int? {
        if (!ensureInit()) {
            Log.e(TAG, "TTS 引擎不可用")
            return null
        }
        return withTimeoutOrNull(SYNTH_TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                val resumed = AtomicBoolean(false)

                fun complete(duration: Int?) {
                    if (resumed.compareAndSet(false, true)) {
                        cont.resume(duration)
                    }
                }

                val listener = object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = Unit

                    override fun onDone(utteranceId: String?) {
                        complete(durationOf(target))
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        Log.e(TAG, "TTS 合成失败: $utteranceId")
                        complete(null)
                    }
                }

                cont.invokeOnCancellation {
                    runCatching { tts?.stop() }
                }

                try {
                    // 先注册监听再合成，避免快速完成时丢回调
                    tts?.setOnUtteranceProgressListener(listener)
                    val result = tts?.synthesizeToFile(
                        text,
                        null,
                        target,
                        "msg_tts_${System.currentTimeMillis()}"
                    ) ?: TextToSpeech.ERROR
                    if (result != TextToSpeech.SUCCESS) {
                        complete(null)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "TTS 合成异常", e)
                    complete(null)
                }
            }
        }
    }

    /** 释放引擎（Application 级单例可不调用） */
    suspend fun shutdown() {
        withContext(Dispatchers.Main) {
            runCatching { tts?.shutdown() }
            tts = null
            ready = false
        }
    }

    /** 读取合成文件时长（秒），失败返回 0 */
    private fun durationOf(file: File): Int {
        if (!file.exists()) return 0
        return try {
            val retriever = android.media.MediaMetadataRetriever()
            try {
                retriever.setDataSource(file.absolutePath)
                val ms = retriever
                    .extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L
                if (ms > 0) (ms / 1000L).toInt() else 0
            } finally {
                runCatching { retriever.release() }
            }
        } catch (e: Exception) {
            Log.w(TAG, "读取 TTS 音频时长失败", e)
            0
        }
    }
}
