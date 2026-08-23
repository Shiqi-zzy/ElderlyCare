package com.elderlycare.app.ui.message

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.elderlycare.app.R
import com.elderlycare.app.data.binding.BindingRepository
import com.elderlycare.app.data.ezviz.NetworkResult
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.message.MessageEntity
import com.elderlycare.app.ui.shared.AuthorizedSnsProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 消息中心 ViewModel（会话列表模式，对标萤石对话会话列表）。
 *
 * - conversations = 当前设备全部消息（排除系统消息）在内存按发送方聚合的会话条目，
 *   最新消息时间倒序；同发送方归一个会话，不新增任何数据表/字段；
 * - 会话分组键（内存归一）：设备产生消息（报警 msgType=5 / 设备视频留言 msgType=3）
 *   统一归到「RK3(设备序列号)」会话（历史脏数据 senderName 为空/旧值同样归一）；
 *   其余按 senderName 分组（「我」=App 发送，其他人员=对应名称）；
 * - 顶栏「全部已读」：全局清零当前设备全部未读（Room 流自动刷新会话红点）；
 * - 数据同步：设备视频留言（refreshDeviceMessages）+ 云端告警落库（getAlarmList
 *   按授权 SN 过滤 → saveAlertMessages，alarmId 幂等）；进页/切设备首刷 +
 *   60s 静默轮询；与留言页并发拉取靠 remoteId 唯一索引 + insertIgnore 兜底。
 *
 * 设备源：BindingRepository 授权链路（响应式）——登出/切号后自动置空。
 */
class MessageCenterViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "MessageCenterViewModel"

        /** 同步轮询间隔：与留言页 60s 对齐 */
        private const val POLL_INTERVAL_MS = 60_000L

        /**
         * 会话分组键（内存归一，与落库展示名对齐）：
         * 设备产生消息（报警/设备视频留言）恒归「RK3(SN)」——历史脏数据 senderName
         * 为空或旧值（如「设备」）也在分组层归一到设备会话；其余按 senderName 分组。
         */
        fun conversationKey(message: MessageEntity): String = when (message.msgType) {
            MessageEntity.MSG_TYPE_ALERT, MessageEntity.MSG_TYPE_DEVICE ->
                MessageEntity.deviceSenderName(message.deviceSerial)
            else -> message.senderName.ifBlank { MessageEntity.deviceSenderName(message.deviceSerial) }
        }
    }

    /** 会话条目（纯内存聚合产物，不落库） */
    data class Conversation(
        val key: String,            // 分组键（设备会话 = RK3(SN)，人员会话 = senderName）
        val title: String,          // 会话标题展示文本（恒非空，绝不渲染 null）
        val isDevice: Boolean,      // 设备会话（列表左侧用设备图标）
        val latest: MessageEntity,  // 该会话最新一条消息（预览摘要 + 时间）
        val unread: Int             // 该会话内部未读数量（>0 显示红点角标）
    )

    private val repository = ServiceLocator.messageRepository

    /** 当前可访问设备（响应式，来自绑定授权链路） */
    private val device = MutableStateFlow<BindingRepository.AccessibleDevice?>(null)
    private val deviceSerial: String? get() = device.value?.deviceSn

    /** 设备名称（设备视频留言拉取用，落库 senderName 已统一 RK3(SN)，此处仅保留接口兼容） */
    private val deviceName: String
        get() = ServiceLocator.deviceBindingStore.load()?.deviceName ?: ""

    /** 告警落库过滤用的授权 SN 集合（角色感知） */
    private val authorizedSns = MutableStateFlow<Set<String>>(emptySet())

    /**
     * 会话列表：全部消息（时间倒序，DAO 保证）在内存按发送方聚合；
     * 系统消息（提醒播报完成）只在留言页显示，不进消息中心（决策 7）。
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val conversations: StateFlow<List<Conversation>> = device
        .flatMapLatest { d ->
            val serial = d?.deviceSn ?: return@flatMapLatest flowOf(emptyList())
            repository.observeAllMessages(serial)
                .map { list -> groupConversations(list.filter { it.msgType != MessageEntity.MSG_TYPE_SYSTEM }) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
        // 观察授权设备（响应式）：设备变化（含首次加载）→ 立即同步一次
        viewModelScope.launch {
            ServiceLocator.bindingRepository.observeCurrentUserDevice().collect { d ->
                device.value = d
                if (d != null) syncNow()
            }
        }
        // 授权 SN 集合（告警落库过滤用）
        viewModelScope.launch {
            AuthorizedSnsProvider.flow().collect { authorizedSns.value = it }
        }
        startSyncPolling()
    }

    // ==================== 会话分组（纯内存，不新增数据表） ====================

    /** 按发送方聚合会话，最新消息时间倒序 */
    private fun groupConversations(messages: List<MessageEntity>): List<Conversation> =
        messages.groupBy { conversationKey(it) }
            .map { (key, list) ->
                val latest = list.maxByOrNull { it.createTime } ?: return@map null
                Conversation(
                    key = key,
                    title = key,
                    isDevice = latest.msgType == MessageEntity.MSG_TYPE_ALERT ||
                        latest.msgType == MessageEntity.MSG_TYPE_DEVICE,
                    latest = latest,
                    unread = list.count { !it.isRead }
                )
            }
            .filterNotNull()
            .sortedByDescending { it.latest.createTime }

    // ==================== 已读 ====================

    /** 顶栏「全部已读」：清零当前设备全部未读（全部会话统一标记） */
    fun markAllRead() {
        val serial = deviceSerial ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.markAllRead(serial)
            toast(R.string.message_center_mark_all_read_done)
        }
    }

    // ==================== 数据同步 ====================

    /** 立即同步：设备视频留言 + 云端告警落库（进页/切设备触发，静默不打断 UI） */
    fun syncNow() {
        viewModelScope.launch(Dispatchers.IO) {
            val serial = deviceSerial ?: return@launch
            try {
                repository.refreshDeviceMessages(serial, deviceName)
            } catch (e: Exception) {
                Log.w(TAG, "设备视频留言拉取失败", e)
            }
            syncAlarms()
        }
    }

    /** 云端告警 → 消息中心落库（按授权 SN 过滤，alarmId 幂等，静默） */
    private suspend fun syncAlarms() {
        try {
            when (val result = ServiceLocator.repository.getAlarmList(pageStart = 0, pageSize = 50)) {
                is NetworkResult.Success -> {
                    val filtered = result.data.filter { it.deviceSerial in authorizedSns.value }
                    repository.saveAlertMessages(filtered)
                }
                is NetworkResult.Error -> Log.w(TAG, "云端告警拉取失败: ${result.message}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "告警落库失败", e)
        }
    }

    /**
     * 60s 静默轮询（与留言页轮询并发拉取设备留言，靠 remoteId 唯一索引 +
     * insertIgnore 幂等；失败只打日志，下一轮重试）。
     */
    private fun startSyncPolling() {
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                deviceSerial?.let { serial ->
                    try {
                        repository.refreshDeviceMessages(serial, deviceName)
                    } catch (e: Exception) {
                        Log.w(TAG, "设备视频留言轮询失败", e)
                    }
                    syncAlarms()
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }
}
