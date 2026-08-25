package com.elderlycare.app.util

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Log
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * WAV（16kHz/16bit/单声道 PCM）→ ADTS 裸 AAC 转码器。
 *
 * 用途：sendonce 临时语音下发（/api/lapp/voice/sendonce）仅接受带 ADTS 头的裸 AAC 流，
 * 而 AudioRecorder 录音输出 16kHz 单声道 WAV（双通道通路继续用 WAV）——
 * 离线留言分支发送前用本工具把 WAV 转码为 *.aac。
 *
 * 实现：MediaCodec 软件编码器 audio/mp4a-lc（OMX.google.aac.encoder，全真机可用；
 * 模拟器可能缺失，转码失败返回 false，由调用方置 failReason，不回退 WAV 直传）。
 * 输出为 ADTS 帧序列（每帧 7 字节 ADTS 头 + AAC-LC 原始帧），采样率 16000、单声道、
 * 码率 24kbps，与萤石语音文件要求一致。
 */
object WavToAacTranscoder {

    private const val TAG = "WavToAacTranscoder"

    /** 编码器参数（与 AudioRecorder 输出严格对齐：16kHz/单声道） */
    private const val SAMPLE_RATE = 16000
    private const val CHANNEL_COUNT = 1
    private const val BIT_RATE = 24_000

    /** ADTS 采样率索引：16kHz = 8 */
    private const val ADTS_SAMPLE_RATE_INDEX = 8

    /** MediaCodec 输入/输出超时 */
    private const val CODEC_TIMEOUT_US = 10_000L

    /** 输入缓冲区字节对齐（AAC 编码器输入为整帧 PCM，对齐保证不丢尾帧） */
    private const val PCM_FRAME_ALIGN = 1024

    /**
     * 把 16kHz/16bit/单声道 WAV 转码为 ADTS 裸 AAC，写入 [outFile]。
     *
     * 前置校验：文件存在、RIFF/WAVE 标记、采样率 16000、单声道、16bit；
     * 不符合直接返回 false（不产生半成品文件）。转码/写文件全程 try/catch，
     * 失败返回 false（outFile 可能残留，由调用方删除）。
     */
    fun transcode(wavFile: File, outFile: File): Boolean {
        if (!wavFile.exists() || wavFile.length() == 0L) {
            Log.w(TAG, "WAV 文件不存在或为空: ${wavFile.absolutePath}")
            return false
        }
        return try {
            val wav = wavFile.readBytes()
            val pcmOffset = parseWavHeader(wav) ?: run {
                Log.w(TAG, "WAV 头不符合要求（需 16kHz/16bit/单声道）: ${wavFile.name}")
                return false
            }
            if (wav.size - pcmOffset <= 0) {
                Log.w(TAG, "WAV 无 PCM 数据: ${wavFile.name}")
                return false
            }
            encodeToAdts(wav, pcmOffset, outFile)
        } catch (t: Throwable) {
            Log.e(TAG, "WAV→AAC 转码异常", t)
            false
        }
    }

    /**
     * 校验 WAV 头（RIFF/WAVE 标记 + 16kHz/16bit/单声道），返回 PCM 数据起始偏移；
     * 头不合法返回 null。
     */
    private fun parseWavHeader(wav: ByteArray): Int? {
        if (wav.size < 44) return null
        if (wav[0] != 'R'.code.toByte() || wav[1] != 'I'.code.toByte() ||
            wav[2] != 'F'.code.toByte() || wav[3] != 'F'.code.toByte()
        ) return null
        if (wav[8] != 'W'.code.toByte() || wav[9] != 'A'.code.toByte() ||
            wav[10] != 'V'.code.toByte() || wav[11] != 'E'.code.toByte()
        ) return null
        val channels = littleEndianShort(wav, 22)
        val sampleRate = littleEndianInt(wav, 24)
        val bitsPerSample = littleEndianShort(wav, 34)
        if (channels != CHANNEL_COUNT || sampleRate != SAMPLE_RATE || bitsPerSample != 16) return null
        // 标准 44 字节头后直接是 "data" chunk；扫描确认（AudioRecorder 写标准头，这里兜底）
        var offset = 36
        while (offset + 8 <= wav.size) {
            if (wav[offset] == 'd'.code.toByte() && wav[offset + 1] == 'a'.code.toByte() &&
                wav[offset + 2] == 't'.code.toByte() && wav[offset + 3] == 'a'.code.toByte()
            ) {
                return offset + 8
            }
            offset += 2
        }
        // 兜底：标准 44 字节头
        return 44
    }

    /** MediaCodec 编码：PCM16 → AAC-LC 帧 → 手写 ADTS 头 → 顺序写文件 */
    private fun encodeToAdts(wav: ByteArray, pcmOffset: Int, outFile: File): Boolean {
        val format = MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_AAC, SAMPLE_RATE, CHANNEL_COUNT
        ).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 4096)
        }

        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        try {
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()
            val info = MediaCodec.BufferInfo()
            val inputBuffers = codec.inputBuffers
            val outputBuffers = codec.outputBuffers
            var inputDone = false
            var outputDone = false
            var pcmPos = pcmOffset

            FileOutputStream(outFile).use { fos ->
                BufferedOutputStream(fos).use { bos ->
                    while (!outputDone) {
                        // 喂输入：整帧对齐（编码器输入尽量按 1024 字节对齐，避免尾帧被丢弃）
                        if (!inputDone) {
                            val inIndex = codec.dequeueInputBuffer(CODEC_TIMEOUT_US)
                            if (inIndex >= 0) {
                                val buffer = inputBuffers[inIndex]
                                buffer.clear()
                                val remaining = wav.size - pcmPos
                                if (remaining <= 0) {
                                    codec.queueInputBuffer(
                                        inIndex, 0, 0, 0,
                                        MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                    )
                                    inputDone = true
                                } else {
                                    val chunk = minOf(remaining, buffer.capacity(), PCM_FRAME_ALIGN)
                                    buffer.put(wav, pcmPos, chunk)
                                    codec.queueInputBuffer(inIndex, 0, chunk, 0, 0)
                                    pcmPos += chunk
                                }
                            }
                        }
                        // 收输出：写 ADTS 帧
                        val outIndex = codec.dequeueOutputBuffer(info, CODEC_TIMEOUT_US)
                        when {
                            outIndex >= 0 -> {
                                val buffer = outputBuffers[outIndex]
                                if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                                    // AAC 编码器无 csd 输出，理论上不走这里
                                    info.size = 0
                                }
                                if (info.size > 0) {
                                    // 输出 buffer 为 ByteBuffer，先拷到字节数组再写流
                                    val chunk = ByteArray(info.size)
                                    buffer.position(info.offset)
                                    buffer.get(chunk, 0, info.size)
                                    bos.write(buildAdtsHeader(info.size))
                                    bos.write(chunk)
                                }
                                codec.releaseOutputBuffer(outIndex, false)
                                if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                                    outputDone = true
                                }
                            }
                            outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                                // 格式变化忽略（输出即 ADTS 原始帧，无需按 MediaFormat 处理）
                            }
                            outIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                                // 无输出可用，继续喂输入
                            }
                        }
                    }
                }
            }
            return true
        } finally {
            runCatching { codec.stop() }
            runCatching { codec.release() }
        }
    }

    /**
     * 构造 7 字节 ADTS 帧头（AAC-LC / 16kHz / 单声道）。
     * 帧长 = 7 + 原始帧字节数；buffer fullness 按可变帧率约定置满（0x7FF）。
     */
    private fun buildAdtsHeader(frameSize: Int): ByteArray {
        val frameLen = frameSize + 7
        return byteArrayOf(
            0xFF.toByte(),
            0xF1.toByte(),
            // profile(2bit)=0(AAC-LC) | samplingFrequencyIndex(4bit)=8 | private=0 | channel 高位(2bit)=0
            0x20.toByte(),
            // channel 低位(2bit)=0 | original=0 | home=0 | copyright=0 | frameLen 高 2 位
            (((frameLen shr 11) and 0x03) or 0x00).toByte(),
            ((frameLen shr 3) and 0xFF).toByte(),
            // frameLen 低 3 位 | buffer fullness 高 5 位（0x7FF 全 1 前 5 位）
            ((((frameLen and 0x07) shl 5) or 0x1F) and 0xFF).toByte(),
            // buffer fullness 低 6 位全 1 | number_of_raw_data_blocks=0
            0xFC.toByte()
        )
    }

    private fun littleEndianInt(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 3].toInt() and 0xFF) shl 24)

    private fun littleEndianShort(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)
}
