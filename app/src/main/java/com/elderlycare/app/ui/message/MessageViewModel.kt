package com.elderlycare.app.ui.message

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.elderlycare.app.R
import com.elderlycare.app.data.binding.BindingRepository
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.ezviz.VoiceCallState
import com.elderlycare.app.data.message.MessageEntity
import com.elderlycare.app.data.message.MessageFiles
import com.elderlycare.app.data.reminder.RemindPlanEntity
import com.elderlycare.app.util.AudioRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

/**
 * 留言页 ViewModel。
 *
 * 职责：混合 feed（普通留言 + 系统消息 + 提醒计划）、未读数、
 * 按住录音留言（声波采样/计时/60s 上限，松开后仓库级联发送：
 * 双通道优先，失败自动降级 sendonce 一次性下发）、失败消息重发、
 * 文字留言发送（HTTP 提交文本给后端，云端 TTS + 萤石云广播）、
 * 设备留言接收（EZOpenSDK 微聊公开接口：拉取视频留言/下载/标记已读/删除）、
 * 音频播放（ExoPlayer）、提醒计划播报完成轮询（每 60s，页面销毁自动取消）、
 * 设备留言静默轮询（每 60s 与提醒计划轮询合并，失败只打日志）、
 * 语音通话状态透传（通路①）。
 *
 * 设备源：BindingRepository 授权链路（响应式）——登出/切号后自动置空，
 * 重登后跟随当前档案 deviceSn，不再读 deviceBindingStore 缓存。
 *
 * 所有耗时操作（录音、发送、拉取）均切到后台协程执行（SDK 留言接口禁止主线程）。
 */
class MessageViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "MessageViewModel"

        /** 录音时长上限（云广播要求 ≤60s） */
        private const val MAX_RECORD_SEC = 60

        /** 声波采样间隔 */
        private const val AMPLITUDE_SAMPLE_MS = 100L
    }

    private val repository = ServiceLocator.messageRepository

    /** 当前可访问设备（响应式，来自绑定授权链路） */
    private val device = MutableStateFlow<BindingRepository.AccessibleDevice?>(null)
    private val deviceSerial: String? get() = device.value?.deviceSn

    /** 设备名称（兜底读绑定缓存，无则空 → 入库用默认「设备」文案） */
    private val deviceName: String
        get() = ServiceLocator.deviceBindingStore.load()?.deviceName ?: ""

    // ==================== 混合 feed / 未读 ====================

    /**
     * 留言页混合 feed：留言表（普通留言 + 系统消息）+ remind_plan 表（提醒计划），
     * 按时间倒序。合并位置在本 ViewModel——不改 MessageRepository 构造，零涟漪。
     * 设备源为授权链路（响应式）：设备变化（含登出置空）时自动切流。
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val feed: StateFlow<List<MessageFeedItem>> = device
        .flatMapLatest { d ->
            val serial = d?.deviceSn ?: return@flatMapLatest flowOf(emptyList())
            combine(
                repository.observeMessages(serial),
                ServiceLocator.reminderRepository.observePlans(serial)
            ) { msgs, plans ->
                buildList {
                    msgs.forEach { add(MessageFeedItem.Msg(it)) }
                    plans.forEach { add(MessageFeedItem.Plan(it)) }
                }.sortedByDescending { it.sortTime }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val unreadCount: StateFlow<Int> = device
        .flatMapLatest { d ->
            val serial = d?.deviceSn ?: return@flatMapLatest flowOf(0)
            repository.observeUnreadCount(serial)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // ==================== 录音 ====================

    private val recorder = AudioRecorder()
    private var currentRecordFile: File? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    /** 录音中上滑取消状态（UI 显示红色取消样式） */
    private val _recordCancelled = MutableStateFlow(false)
    val recordCancelled: StateFlow<Boolean> = _recordCancelled.asStateFlow()

    /** 已录音秒数（UI 计时显示） */
    private val _recordElapsed = MutableStateFlow(0)
    val recordElapsed: StateFlow<Int> = _recordElapsed.asStateFlow()

    /** 当前音量峰值（0~32767，声波动画用） */
    private val _recordAmplitude = MutableStateFlow(0)
    val recordAmplitude: StateFlow<Int> = _recordAmplitude.asStateFlow()

    private var recordJob: Job? = null

    // ==================== 播放 ====================

    private var player: ExoPlayer? = null
    private var progressJob: Job? = null

    private val _playingId = MutableStateFlow<Long?>(null)
    val playingId: StateFlow<Long?> = _playingId.asStateFlow()

    /** 当前播放进度（0f~1f），播放中定时刷新 */
    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

    // ==================== 语音通话 ====================

    val voiceCallState: StateFlow<VoiceCallState> = repository.voiceCall.state

    // ==================== 一次性提示 ====================

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    fun toast(resId: Int, vararg args: Any) {
        _toastMessage.value = getApplication<Application>().getString(resId, *args)
    }

    fun consumeToast() {
        _toastMessage.value = null
    }

    // ==================== 初始化 ====================

    init {
        // 观察授权设备（响应式）：设备变化（含首次加载）→ 同步一次云端留言
        viewModelScope.launch {
            ServiceLocator.bindingRepository.observeCurrentUserDevice().collect { d ->
                device.value = d
                d?.deviceSn?.let { refresh() }
            }
        }
        // 提醒计划播报完成轮询
        startRemindPolling()
    }

    /** 同步设备留言（EZOpenSDK 网络操作，禁止主线程——显式切 IO 调度器） */
    fun refresh() {
        val serial = deviceSerial ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val added = repository.refreshDeviceMessages(serial, deviceName)
            if (added < 0) {
                toast(R.string.message_refresh_failed)
            }
        }
    }

    /**
     * 提醒计划播报完成轮询：每 60s 查萤石 schedule/record（今天+昨天），
     * 识别已播报 → markExecuted + 插【系统】消息；同一循环内静默拉取设备视频留言
     * （refreshDeviceMessages，失败只打日志不阻断，与消息中心并发拉取靠
     * remoteId 唯一索引 + insertIgnore 幂等）。页面销毁随 viewModelScope
     * 自动取消；全程静默（轮询不能崩，失败打日志下一轮重试）。
     */
    private fun startRemindPolling() {
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    deviceSerial?.let { ServiceLocator.reminderRepository.pollExecutedAndInsert(it) }
                } catch (e: Exception) {
                    Log.w(TAG, "提醒计划播报轮询失败", e)
                }
                try {
                    deviceSerial?.let { repository.refreshDeviceMessages(it, deviceName) }
                } catch (e: Exception) {
                    Log.w(TAG, "设备视频留言轮询失败", e)
                }
                delay(60_000)
            }
        }
    }

    // ==================== 录音控制 ====================

    /** 开始录音（调用方需先确认 RECORD_AUDIO 权限） */
    fun startRecording() {
        if (_isRecording.value) return
        val file = MessageFiles.newRecordFile(getApplication())
        currentRecordFile = file
        viewModelScope.launch(Dispatchers.IO) {
            val ok = recorder.start(file)
            if (!ok) {
                currentRecordFile = null
                _isRecording.value = false
                toast(R.string.message_record_failed)
                return@launch
            }
            _isRecording.value = true
            _recordCancelled.value = false
            _recordElapsed.value = 0
            _recordAmplitude.value = 0

            // 声波采样 + 计时循环（60s 上限自动结束）
            recordJob = viewModelScope.launch(Dispatchers.Default) {
                var elapsed = 0
                var tick = 0
                while (isActive) {
                    _recordAmplitude.value = recorder.getAmplitude()
                    delay(AMPLITUDE_SAMPLE_MS)
                    tick++
                    if (tick >= 10) {
                        tick = 0
                        elapsed++
                        _recordElapsed.value = elapsed
                        if (elapsed >= MAX_RECORD_SEC) {
                            toast(R.string.message_record_max_reached)
                            finishRecording()
                            break
                        }
                    }
                }
            }
        }
    }

    /** 松开按钮：结束录音并发送（仓库内部级联：双通道优先，失败自动降级 sendonce） */
    fun finishRecording() {
        if (!_isRecording.value) return
        recordJob?.cancel()
        recordJob = null
        viewModelScope.launch(Dispatchers.IO) {
            val duration = recorder.stop()
            _isRecording.value = false
            _recordCancelled.value = false
            val file = currentRecordFile ?: return@launch
            currentRecordFile = null

            when {
                duration <= 0 -> {
                    MessageFiles.deleteQuietly(file)
                    toast(R.string.message_record_failed)
                }
                duration < 1 -> {
                    MessageFiles.deleteQuietly(file)
                    toast(R.string.message_record_too_short)
                }
                else -> {
                    // 录音文件 → 级联发送（双通道 → 失败自动降级 sendonce，用户无感知）
                    val serial = deviceSerial
                    if (serial == null) {
                        MessageFiles.deleteQuietly(file)
                        toast(R.string.message_no_device)
                    } else {
                        repository.sendRecordMessage(serial, file, duration)
                    }
                }
            }
        }
    }

    /** 上滑取消：停止录音并删除文件 */
    fun cancelRecording() {
        if (!_isRecording.value) return
        recordJob?.cancel()
        recordJob = null
        _recordCancelled.value = true
        viewModelScope.launch(Dispatchers.IO) {
            recorder.cancel()
            currentRecordFile = null
            _isRecording.value = false
        }
        toast(R.string.message_record_cancelled)
    }

    // ==================== 发送 ====================

    /** 文字留言（萤石 v3 闹铃接口：RK3 本地 TTS 即时播报，播报后自动清理临时闹铃） */
    fun sendText(text: String) {
        val serial = deviceSerial ?: run {
            toast(R.string.message_no_device)
            return
        }
        viewModelScope.launch {
            repository.sendTextMessage(serial, text)
        }
    }

    // ==================== 播放 ====================

    /** 健康建议气泡点击：仅标记已读（无音频可播，不走设备播报） */
    fun markRead(message: MessageEntity) {
        if (!message.isRead) {
            viewModelScope.launch { repository.markMessageRead(message) }
        }
    }

    /** 点击留言：播放/暂停音频，同时标记已读 */
    fun togglePlay(message: MessageEntity) {
        if (!message.isRead) {
            viewModelScope.launch { repository.markMessageRead(message) }
        }
        // 同一条留言：切换播放/暂停
        if (_playingId.value == message.id) {
            player?.let { it.playWhenReady = !it.playWhenReady }
            return
        }
        if (message.localAudioPath.isBlank()) {
            toast(R.string.message_play_failed)
            return
        }
        stopPlayback()
        val appContext: android.content.Context = getApplication()
        val exo = ExoPlayer.Builder(appContext)
            .setMediaSourceFactory(DefaultMediaSourceFactory(appContext))
            .build()
        exo.setMediaItem(MediaItem.fromUri(Uri.fromFile(File(message.localAudioPath))))
        exo.prepare()
        exo.playWhenReady = true
        exo.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) stopPlayback()
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "音频播放失败", error)
                toast(R.string.message_play_failed)
                stopPlayback()
            }
        })
        player = exo
        _playingId.value = message.id
        _playbackProgress.value = 0f
        // ExoPlayer 在主线程创建，访问 duration/currentPosition 必须在主线程（否则抛 Player is accessed on the wrong thread）
        progressJob = viewModelScope.launch(Dispatchers.Main) {
            while (isActive) {
                val duration = exo.duration.takeIf { it > 0 } ?: 0L
                _playbackProgress.value =
                    if (duration > 0) exo.currentPosition.toFloat() / duration else 0f
                delay(200)
            }
        }
    }

    fun stopPlayback() {
        progressJob?.cancel()
        progressJob = null
        runCatching { player?.release() }
        player = null
        _playingId.value = null
        _playbackProgress.value = 0f
    }

    // ==================== 语音通话 / 删除 ====================

    /** 结束语音通话（通路①的主动挂断入口） */
    fun endVoiceCall() {
        repository.voiceCall.stop()
    }

    fun delete(message: MessageEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMessage(message)
        }
    }

    /**
     * 重发失败的录音留言：完整复现「双通道 → 失败自动降级 sendonce」整套级联流程。
     * 仅失败状态的录音留言可重发（守卫在仓库层），失败 toast 提示。
     */
    fun resend(message: MessageEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            if (repository.resendMessage(message) < 0) {
                toast(R.string.message_resend_failed)
            }
        }
    }

    override fun onCleared() {
        recordJob?.cancel()
        runCatching { recorder.cancel() }
        stopPlayback()
        repository.voiceCall.stop()
        super.onCleared()
    }
}

/**
 * 留言页混合 feed 条目：留言消息（含系统消息）或提醒计划。
 * 两表 Long 自增 id 会撞，LazyColumn key 需加类型前缀（"msg_"/"plan_"）。
 */
sealed interface MessageFeedItem {
    val sortTime: Long

    data class Msg(val message: MessageEntity) : MessageFeedItem {
        override val sortTime: Long get() = message.createTime
    }

    data class Plan(val plan: RemindPlanEntity) : MessageFeedItem {
        override val sortTime: Long get() = plan.createTime
    }
}
