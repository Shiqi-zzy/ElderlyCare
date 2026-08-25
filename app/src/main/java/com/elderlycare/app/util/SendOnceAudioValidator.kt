package com.elderlycare.app.util

import com.elderlycare.app.R
import java.io.File
import java.io.RandomAccessFile

/**
 * sendonce 临时语音下发的前置音频校验器。
 *
 * 目的：提前拦截不合规音频，规避萤石报错「上传的语音文件长度不正确或文件格式错误」。
 * 校验项：时长 1~60s、文件存在且非空、≤20M、扩展名为 aac、ADTS 同步字首帧校验
 * （转码产物自检；WAV 回退路径不存在，sendonce 仅接受 ADTS 裸 AAC）。
 *
 * 返回 null 表示通过；否则返回对应错误文案的 R.string 资源 id（资源 id 放在 util 层
 * 引用 R 与 MessageRepository 现有做法一致）。
 */
object SendOnceAudioValidator {

    /** sendonce 文件上限：20M（与 EZCloudBroadcastManager.MAX_SENDONCE_BYTES 一致） */
    private const val MAX_FILE_BYTES = 20L * 1024 * 1024

    /** 时长上限（秒）：与 MessageViewModel.MAX_RECORD_SEC 一致 */
    private const val MAX_DURATION_SEC = 60

    /**
     * 校验音频文件与时长。
     *
     * @param file        AAC 文件（WavToAacTranscoder 转码产物）
     * @param durationSec 录音时长（秒，AudioRecorder.stop 返回值）
     * @return null=通过；否则错误文案资源 id
     */
    fun validate(file: File, durationSec: Int): Int? {
        if (durationSec < 1) return R.string.message_record_too_short
        if (durationSec > MAX_DURATION_SEC) return R.string.message_record_too_long
        if (!file.exists() || file.length() == 0L) return R.string.message_audio_invalid
        if (file.length() > MAX_FILE_BYTES) return R.string.message_audio_too_large
        if (file.extension.lowercase() != "aac") return R.string.message_audio_invalid
        if (!isAacAdts(file)) return R.string.message_audio_invalid
        return null
    }

    /**
     * 校验 ADTS 同步字：读文件前 7 字节，首字节必须为 0xFF，次字节高 4 位必须为 0xF
     * （syncword 0xFFF）。非 ADTS 容器（m4a/mp4/裸流）会在这一步被拦下。
     */
    fun isAacAdts(file: File): Boolean = try {
        RandomAccessFile(file, "r").use { raf ->
            if (raf.length() < 2) return@use false
            val b0 = raf.read()
            val b1 = raf.read()
            b0 == 0xFF && (b1 and 0xF0) == 0xF0
        }
    } catch (t: Throwable) {
        false
    }
}
