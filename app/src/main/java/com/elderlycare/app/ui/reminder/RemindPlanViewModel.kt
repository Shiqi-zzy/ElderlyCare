package com.elderlycare.app.ui.reminder

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
import com.elderlycare.app.data.ezviz.NetworkResult
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.reminder.PlanDraft
import com.elderlycare.app.data.reminder.PreviewVoices
import com.elderlycare.app.data.reminder.RemindPlanEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 提醒计划 ViewModel（列表页 / 表单页 / 日程 Tab 共用，每个导航栈条目各一个实例）。
 *
 * 职责：
 * - 计划列表展示（Room Flow 实时刷新）+ 进页同步（萤石 clock/list 以设备为准覆盖本地）+ 删除；
 * - 表单保存（萤石 life/remind/clock 下发 → clockId 入库，成功 toast 后回调返回）；
 * - 手机试听（文本+音色 → 后端 edge-tts 合成 mp3 落盘 → ExoPlayer 播放本地文件）。
 */
class RemindPlanViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "RemindPlanViewModel"
    }

    private val repository = ServiceLocator.reminderRepository

    /**
     * 当前可访问设备（响应式，来自绑定授权链路：userId → ACTIVE 绑定 → 档案 → deviceSn）。
     * 与首页/留言页/消息中心同源；禁止回退 deviceBindingStore 旧缓存——
     * 该缓存在登出时被清空、且多端合并后不再可靠，会导致「首页已绑定、提醒页提示未绑定」。
     */
    private val device = MutableStateFlow<BindingRepository.AccessibleDevice?>(null)
    private val deviceSerial: String? get() = device.value?.deviceSn

    // ==================== 列表 ====================

    @OptIn(ExperimentalCoroutinesApi::class)
    val plans: StateFlow<List<RemindPlanEntity>> = device
        .flatMapLatest { d ->
            val serial = d?.deviceSn ?: return@flatMapLatest flowOf(emptyList())
            repository.observePlans(serial)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ==================== 表单：音色 / 试听 / 保存 ====================

    /** 当前选中音色（仅用于手机试听；保存计划不传音色给萤石） */
    private val _selectedVoice = MutableStateFlow(PreviewVoices.DEFAULT_KEY)
    val selectedVoice: StateFlow<String> = _selectedVoice.asStateFlow()

    fun setSelectedVoice(key: String) {
        _selectedVoice.value = key
    }

    private val _previewState = MutableStateFlow<PreviewState>(PreviewState.Idle)
    val previewState: StateFlow<PreviewState> = _previewState.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private var player: ExoPlayer? = null

    // ==================== 一次性提示 ====================

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    fun toast(resId: Int, vararg args: Any) {
        _toastMessage.value = getApplication<Application>().getString(resId, *args)
    }

    fun toastText(text: String) {
        _toastMessage.value = text
    }

    fun consumeToast() {
        _toastMessage.value = null
    }

    // ==================== 初始化 / 删除 ====================

    init {
        // 观察授权设备（响应式）：设备变化（含首次加载）→ 同步一次云端计划
        viewModelScope.launch {
            ServiceLocator.bindingRepository.observeCurrentUserDevice().collect { d ->
                device.value = d
                syncFromDevice()
            }
        }
        startRemindPolling()
    }

    /** 进页同步一次：萤石 clock/list 以设备为准覆盖本地。失败静默——本地数据兜底展示 */
    fun syncFromDevice() {
        val serial = deviceSerial ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.refreshFromDevice(serial)
        }
    }

    /**
     * 播报完成轮询（60s）：schedule/record 识别已播报计划 →
     * 标记 executed + 留言表插系统消息（日程页/留言页实时刷新）。
     * 与 MessageViewModel 的轮询并存：repository 内 Mutex 串行化防重复插消息。
     */
    private fun startRemindPolling() {
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    deviceSerial?.let { repository.pollExecutedAndInsert(it) }
                } catch (e: Exception) {
                    Log.w(TAG, "提醒计划播报轮询失败", e)
                }
                delay(60_000)
            }
        }
    }

    /** 单条计划观察（详情页；删除后发 null） */
    fun observePlan(id: Long): Flow<RemindPlanEntity?> = repository.observePlanById(id)

    /**
     * 设备核对：clock/list 查该 clockId 是否仍存在。
     * null=核对失败（网络/未登录等），调用方保留页面展示 Room 数据。
     */
    suspend fun verifyClockExists(clockId: String): Boolean? {
        val serial = deviceSerial ?: return null
        return withContext(Dispatchers.IO) { repository.verifyClockExists(serial, clockId) }
    }

    /** 仅删本地记录（设备侧 clock 已删除 / 脏数据清理），不调萤石删除接口；suspend 保证关闭页面前落库 */
    suspend fun deleteLocal(plan: RemindPlanEntity) {
        withContext(Dispatchers.IO) { repository.deleteLocalRecord(plan) }
    }

    /** 删除计划：萤石删除成功后才删本地；失败 toast 萤石错误映射文案 */
    fun delete(plan: RemindPlanEntity) {
        val serial = deviceSerial ?: run {
            toast(R.string.message_no_device)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = repository.deletePlan(serial, plan)) {
                is NetworkResult.Success -> Unit
                is NetworkResult.Error -> toastText(result.message)
            }
        }
    }

    /** 家属确认复诊提醒（双重确认）：同意 → 下发设备 clock 回填；拒绝 → 仅 App 本地提醒 */
    fun confirmPlan(plan: RemindPlanEntity, agree: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = repository.confirmPlan(plan.id, agree)) {
                is NetworkResult.Success -> toast(
                    if (agree) R.string.reminder_confirm_agreed_toast
                    else R.string.reminder_confirm_rejected_toast
                )
                is NetworkResult.Error -> toastText(result.message)
            }
        }
    }

    // ==================== 保存 ====================

    /** 保存计划：萤石 clock 接口下发成功 → 本地入库 → toast + onSuccess 返回列表页 */
    fun save(draft: PlanDraft, onSuccess: () -> Unit) {
        val serial = deviceSerial ?: run {
            toast(R.string.message_no_device)
            return
        }
        if (_isSaving.value) return
        _isSaving.value = true
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { repository.addPlan(serial, draft) }
            _isSaving.value = false
            when (result) {
                is NetworkResult.Success -> {
                    toast(R.string.reminder_save_success)
                    onSuccess()
                }
                is NetworkResult.Error -> toastText(result.message)
            }
        }
    }

    // ==================== 手机试听 ====================

    /**
     * 试听：文本+音色 → 后端 edge-tts 合成 mp3 下载到本地 → ExoPlayer 播放。
     * Playing 状态再点 = 停止。音色仅用于试听，不触碰萤石。
     */
    fun togglePreview(text: String, voiceKey: String) {
        when (_previewState.value) {
            PreviewState.Playing -> {
                stopPreview()
                return
            }
            PreviewState.Loading -> return
            PreviewState.Idle -> Unit
        }
        viewModelScope.launch {
            _previewState.value = PreviewState.Loading
            val result = withContext(Dispatchers.IO) { repository.previewTts(text, voiceKey) }
            when (result) {
                is NetworkResult.Success -> playPreview(result.data)
                is NetworkResult.Error -> {
                    _previewState.value = PreviewState.Idle
                    toastText(result.message)
                }
            }
        }
    }

    /** 播放本地试听 mp3（复用留言播放的 ExoPlayer 链路） */
    private fun playPreview(file: File) {
        stopPreview()
        val appContext: android.content.Context = getApplication()
        val exo = ExoPlayer.Builder(appContext)
            .setMediaSourceFactory(DefaultMediaSourceFactory(appContext))
            .build()
        exo.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
        exo.prepare()
        exo.playWhenReady = true
        exo.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    _previewState.value = PreviewState.Idle
                    stopPreview()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "试听播放失败", error)
                _previewState.value = PreviewState.Idle
                toast(R.string.message_play_failed)
                stopPreview()
            }
        })
        player = exo
        _previewState.value = PreviewState.Playing
    }

    fun stopPreview() {
        runCatching { player?.release() }
        player = null
        if (_previewState.value == PreviewState.Playing) {
            _previewState.value = PreviewState.Idle
        }
    }

    override fun onCleared() {
        stopPreview()
        super.onCleared()
    }
}

/** 手机试听状态 */
sealed interface PreviewState {
    data object Idle : PreviewState
    data object Loading : PreviewState
    data object Playing : PreviewState
}
