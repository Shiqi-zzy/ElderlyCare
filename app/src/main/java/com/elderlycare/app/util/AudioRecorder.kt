package com.elderlycare.app.util

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile

/**
 * 按住录音工具（AudioRecord → PCM → WAV）。
 *
 * 输出格式：16kHz / 16bit / 单声道 WAV（约 1.9M/分钟），
 * 直接满足云广播上传格式要求（wav，≤5M、≤60s），无需转码。
 *
 * 注意：
 * - 调用方需已获得 RECORD_AUDIO 权限；
 * - start/stop 应在后台线程调用（内部还有独立录音线程）；
 * - 每次录音新建实例使用，不要复用。
 */
class AudioRecorder {

    companion object {
        private const val TAG = "AudioRecorder"

        const val SAMPLE_RATE = 16000
        const val CHANNEL = AudioFormat.CHANNEL_IN_MONO
        const val ENCODING = AudioFormat.ENCODING_PCM_16BIT

        /** WAV 头长度（44 字节） */
        private const val WAV_HEADER_LEN = 44

        /** 每次读取帧数（1024 帧 = 64ms） */
        private const val FRAMES_PER_READ = 1024
    }

    private var audioRecord: AudioRecord? = null
    private var recordThread: Thread? = null
    private var output: FileOutputStream? = null
    private var recordFile: File? = null

    @Volatile
    private var isRecording = false

    /** 已写入 PCM 数据字节数（不含 WAV 头），stop 后回填头部用 */
    @Volatile
    private var dataBytes = 0L

    /** 最近一次读取块的声音峰值（0~32767），供声波动画采样 */
    @Volatile
    private var lastPeak = 0

    /**
     * 开始录音（后台线程调用）。
     * @return false 表示启动失败（无权限 / 麦克风被占用 / IO 异常），文件不会被保留
     */
    fun start(file: File): Boolean {
        if (isRecording) return true
        try {
            val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL, ENCODING)
                .coerceAtLeast(FRAMES_PER_READ * 2)
            val record = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL,
                ENCODING,
                bufferSize
            )
            if (record.state != AudioRecord.STATE_INITIALIZED) {
                record.release()
                Log.e(TAG, "AudioRecord 初始化失败")
                return false
            }

            // 先写占位 WAV 头，停止时回填真实长度
            val out = FileOutputStream(file)
            writeWavHeader(out, 0)

            recordFile = file
            audioRecord = record
            output = out
            dataBytes = 0
            lastPeak = 0
            isRecording = true

            recordThread = Thread({
                recordLoop(record)
            }, "message-audio-record").also { it.start() }
            return true
        } catch (e: SecurityException) {
            Log.e(TAG, "缺少录音权限", e)
            cleanup(file)
            return false
        } catch (e: Exception) {
            Log.e(TAG, "启动录音失败", e)
            cleanup(file)
            return false
        }
    }

    /** 录音线程主体：循环读 PCM 并写文件 */
    private fun recordLoop(record: AudioRecord) {
        val buffer = ShortArray(FRAMES_PER_READ)
        try {
            record.startRecording()
            while (isRecording) {
                val read = record.read(buffer, 0, buffer.size)
                if (read <= 0) {
                    // 读异常（如被其他应用抢占麦克风），直接结束
                    Log.w(TAG, "录音读取异常: $read")
                    break
                }
                var peak = 0
                for (i in 0 until read) {
                    val v = buffer[i].toInt()
                    val abs = if (v < 0) -v else v
                    if (abs > peak) peak = abs
                }
                lastPeak = peak
                val bytes = ByteArray(read * 2)
                for (i in 0 until read) {
                    val v = buffer[i].toInt()
                    bytes[i * 2] = (v and 0xFF).toByte()
                    bytes[i * 2 + 1] = ((v shr 8) and 0xFF).toByte()
                }
                output?.write(bytes)
                dataBytes += bytes.size
            }
        } catch (e: IOException) {
            Log.e(TAG, "录音写文件异常", e)
        } catch (e: Exception) {
            Log.e(TAG, "录音线程异常", e)
        } finally {
            runCatching { record.stop() }
            runCatching { record.release() }
        }
    }

    /**
     * 停止录音并回填 WAV 头（后台线程调用）。
     * @return 录音时长（秒）；失败返回 -1
     */
    fun stop(): Int {
        if (!isRecording) return -1
        isRecording = false
        try {
            recordThread?.join(3000)
            recordThread = null
            output?.flush()
            output?.close()
            output = null

            val file = recordFile ?: return -1
            // 回填 WAV 头中的真实数据长度
            RandomAccessFile(file, "rw").use { raf ->
                raf.seek(40)
                writeLittleEndianInt(raf, dataBytes.toInt())
                raf.seek(4)
                writeLittleEndianInt(raf, (dataBytes + WAV_HEADER_LEN - 8).toInt())
            }
            val duration = (dataBytes.toDouble() / (SAMPLE_RATE * 2).toDouble() + 0.5).toInt()
            audioRecord = null
            return duration
        } catch (e: Exception) {
            Log.e(TAG, "停止录音失败", e)
            cleanup(recordFile ?: File(""))
            return -1
        } finally {
            lastPeak = 0
        }
    }

    /** 取消录音并删除已生成的文件 */
    fun cancel() {
        val file = recordFile
        stop()
        file?.let { f ->
            try {
                // 文件不存在时 delete 返回 false，不会抛异常，无需先判断
                f.delete()
            } catch (e: Exception) {
                Log.w(TAG, "删除取消的录音文件失败", e)
            }
        }
    }

    /** 当前振幅峰值（0~32767），供声波动画使用（无录音时返回 0） */
    fun getAmplitude(): Int = if (isRecording) lastPeak else 0

    /** 启动失败时清理现场 */
    private fun cleanup(file: File) {
        isRecording = false
        runCatching { output?.close() }
        output = null
        runCatching { audioRecord?.release() }
        audioRecord = null
        if (file.exists()) {
            runCatching { file.delete() }
        }
        recordFile = null
    }

    private fun writeWavHeader(out: FileOutputStream, dataLen: Int) {
        val header = ByteArray(WAV_HEADER_LEN)
        fun putAscii(offset: Int, s: String) {
            for (i in s.indices) header[offset + i] = s[i].code.toByte()
        }
        putAscii(0, "RIFF")
        writeIntLE(header, 4, dataLen + WAV_HEADER_LEN - 8)
        putAscii(8, "WAVE")
        putAscii(12, "fmt ")
        writeIntLE(header, 16, 16)                       // fmt 块大小
        writeShortLE(header, 20, 1)                      // PCM
        writeShortLE(header, 22, 1)                      // 单声道
        writeIntLE(header, 24, SAMPLE_RATE)
        writeIntLE(header, 28, SAMPLE_RATE * 2)          // 字节率
        writeShortLE(header, 32, 2)                      // 块对齐
        writeShortLE(header, 34, 16)                     // 位深
        putAscii(40, "data")
        writeIntLE(header, 40, dataLen)
        out.write(header)
    }

    private fun writeIntLE(bytes: ByteArray, offset: Int, value: Int) {
        bytes[offset] = (value and 0xFF).toByte()
        bytes[offset + 1] = ((value shr 8) and 0xFF).toByte()
        bytes[offset + 2] = ((value shr 16) and 0xFF).toByte()
        bytes[offset + 3] = ((value shr 24) and 0xFF).toByte()
    }

    private fun writeShortLE(bytes: ByteArray, offset: Int, value: Int) {
        bytes[offset] = (value and 0xFF).toByte()
        bytes[offset + 1] = ((value shr 8) and 0xFF).toByte()
    }

    private fun writeLittleEndianInt(raf: RandomAccessFile, value: Int) {
        raf.write(value and 0xFF)
        raf.write((value shr 8) and 0xFF)
        raf.write((value shr 16) and 0xFF)
        raf.write((value shr 24) and 0xFF)
    }
}
