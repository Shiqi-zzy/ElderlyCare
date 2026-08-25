package com.elderlycare.app.ui.hospital

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.elderlycare.app.R
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.message.MessageEntity
import com.elderlycare.app.data.reminder.PlanDraft
import com.elderlycare.app.data.reminder.RemindPlanEntity
import com.elderlycare.app.util.LocalRemindScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * 医院端复诊提醒 ViewModel。
 *
 * 数据链路（复用现有提醒计划整套能力，不重写 clock 逻辑）：
 * - 照护对象选择：observeAccessibleElderly(当前医院工作人员) —— 列表本身即
 *   ACTIVE 授权集合，选中照护对象必在授权内（无授权根本不会出现在列表）；
 * - 下发设备播报：复用 RemindPlanRepository.addPendingConfirmPlan/confirmPlan
 *   （复诊双重确认，家属同意后才下萤石 v3 clock）；
 * - 仅 App 提醒：RemindPlanRepository.addLocalPlan（不入萤石，clockId 空）；
 * - 两条路径都落 remind_plan 表（source=1/2），家属端日程/留言 feed 同步展示；
 * - 到点本地通知：LocalRemindScheduler（AlarmManager + RemindAlarmReceiver）；
 * - 清理：轮询复用 pollExecutedAndInsert（播报完成识别）+ cleanExecutedDeviceClocks
 *   （已播报的 RK3 残留闹铃 deleteClocks，防设备残留）。
 */
class HospitalMedicalRemindViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "HospitalMedicalRemindVM"

        /** 复诊提醒计划的固定 tag（列表过滤与清理识别用） */
        const val REMIND_TAG = "复诊提醒"
    }

    private val repository = ServiceLocator.reminderRepository

    /** 照护对象选项（全部档案 + 授权标记），供筛选选择 */
    data class ElderlyOption(
        val elderlyId: String,
        val name: String,
        val deviceSn: String,
        /** 是否存在 ACTIVE 医院授权（observeAccessibleElderly 成员）→ 设备播报复选框可用性 */
        val authorized: Boolean
    )

    private val _elderlyOptions = MutableStateFlow<List<ElderlyOption>>(emptyList())
    val elderlyOptions: StateFlow<List<ElderlyOption>> = _elderlyOptions.asStateFlow()

    private val _selectedElderlyId = MutableStateFlow<String?>(null)
    val selectedElderlyId: StateFlow<String?> = _selectedElderlyId.asStateFlow()

    /** 当前选中照护对象（null = 未选择/已解绑） */
    val selectedElderly: StateFlow<ElderlyOption?> = combine(
        _elderlyOptions, _selectedElderlyId
    ) { list, id ->
        list.firstOrNull { it.elderlyId == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** 医院端创建的复诊提醒列表（全部设备，source != 0，时间倒序） */
    val hospitalPlans: StateFlow<List<RemindPlanEntity>> =
        repository.observeHospitalPlans()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

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

    fun selectElderly(elderlyId: String) {
        _selectedElderlyId.value = elderlyId
    }

    // ==================== 初始化 / 轮询 ====================

    init {
        viewModelScope.launch {
            val staff = ServiceLocator.staffUserStore.getCurrentStaffUser() ?: return@launch
            // 全部档案 × ACTIVE 授权集合 → 选项带授权标记。
            // 未授权照护对象仍可选择（仅 App 提醒），设备播报靠 authorized 强制校验。
            combine(
                ServiceLocator.profileStore.observeProfiles(),
                ServiceLocator.bindingRepository.observeAccessibleElderly(staff)
            ) { profiles, accessible ->
                val authorizedIds = accessible.map { it.elderlyId }.toSet()
                profiles.map { p ->
                    ElderlyOption(p.userId, p.name, p.deviceSn, p.userId in authorizedIds)
                }
            }.collect { options ->
                _elderlyOptions.value = options
                // 首次默认选中第一位；选中照护对象被解绑后自动切换到下一位
                val current = _selectedElderlyId.value
                if (current == null || options.none { it.elderlyId == current }) {
                    _selectedElderlyId.value = options.firstOrNull()?.elderlyId
                }
            }
        }
        startPolling()
    }

    /**
     * 轮询（60s，页面销毁随 viewModelScope 取消）：
     * ①按可访问设备跑 pollExecutedAndInsert 识别已播报（复用家属端同一链路）；
     * ②cleanExecutedDeviceClocks 清理已播报完成的 RK3 残留闹铃（复用 v3 deleteClocks）。
     */
    private fun startPolling() {
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    _elderlyOptions.value.filter { it.authorized }.map { it.deviceSn }
                        .filter { it.isNotBlank() }
                        .distinct()
                        .forEach { serial ->
                            runCatching { repository.pollExecutedAndInsert(serial) }
                        }
                    repository.cleanExecutedDeviceClocks()
                } catch (e: Exception) {
                    Log.w(TAG, "复诊提醒轮询失败", e)
                }
                delay(60_000)
            }
        }
    }

    // ==================== 保存 ====================

    /**
     * 保存复诊提醒（两条路径统一落 remind_plan 表）：
     * - sendToDevice = true → 复诊双重确认：addPendingConfirmPlan 插待确认行
     *   （confirmStatus=PENDING，不下发 clock）+ 调度 App 本地通知兜底 + 插家属
     *   SYSTEM 消息；家属同意后才建 v3 clock 下发 RK3 播报。前置校验：
     *   照护对象 ACTIVE 授权（列表内）且已绑定设备；
     * - sendToDevice = false → addLocalPlan（仅 App 本地提醒，不调萤石）。
     * 保存成功后均调度 App 本地通知（到点弹通知 + 标记已播报 + 插系统消息）。
     */
    fun save(
        dateMillis: Long,
        timeHour: Int,
        timeMin: Int,
        content: String,
        sendToDevice: Boolean,
        onSuccess: () -> Unit
    ) {
        val selected = selectedElderly.value ?: run {
            toast(R.string.hospital_remind_elderly_required)
            return
        }
        val trimmed = content.trim()
        if (trimmed.isEmpty()) {
            toast(R.string.hospital_remind_content_required)
            return
        }
        if (_isSaving.value) return

        // 强制授权校验：无 ACTIVE 医院授权 → 拦截设备播报（仅 App 提醒不受此限）
        if (sendToDevice) {
            if (!selected.authorized) {
                toast(R.string.hospital_remind_no_auth_toast)
                return
            }
            if (selected.deviceSn.isBlank()) {
                toast(R.string.hospital_remind_no_device_toast)
                return
            }
        }

        val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
        val draft = PlanDraft(
            tag = REMIND_TAG,
            content = trimmed,
            timeHour = timeHour,
            timeMin = timeMin,
            repeatType = RemindPlanEntity.REPEAT_ONCE,
            weekdays = listOf((cal.get(Calendar.DAY_OF_WEEK) + 6) % 7),
            year = cal.get(Calendar.YEAR),
            month = cal.get(Calendar.MONTH) + 1,
            day = cal.get(Calendar.DAY_OF_MONTH),
            source = if (sendToDevice)
                RemindPlanEntity.SOURCE_HOSPITAL_DEVICE
            else RemindPlanEntity.SOURCE_HOSPITAL_LOCAL
        )

        _isSaving.value = true
        viewModelScope.launch {
            try {
                if (sendToDevice) {
                    // 复诊双重确认：先插待确认行（不下发 clock），家属同意后才建 v3 闹铃
                    val deviceSn = selected.deviceSn
                    val planId = withContext(Dispatchers.IO) {
                        repository.addPendingConfirmPlan(deviceSn, draft)
                    }
                    if (planId > 0) {
                        // 待确认行（clockId 空）按草稿还原实体用于通知调度：
                        // 家属未确认/拒绝到点也弹 App 通知（闹钟兜底）
                        LocalRemindScheduler.schedule(
                            getApplication(),
                            RemindPlanEntity(
                                id = planId,
                                tag = REMIND_TAG,
                                content = trimmed,
                                timeHour = timeHour,
                                timeMin = timeMin,
                                repeatType = RemindPlanEntity.REPEAT_ONCE,
                                weekdays = draft.weekdays.joinToString(","),
                                year = draft.year,
                                month = draft.month,
                                day = draft.day,
                                deviceSerial = deviceSn,
                                createTime = System.currentTimeMillis(),
                                source = RemindPlanEntity.SOURCE_HOSPITAL_DEVICE,
                                confirmStatus = RemindPlanEntity.CONFIRM_PENDING
                            )
                        )
                        // 插家属端 SYSTEM 消息（remoteId 去重）：申请家属确认下发设备
                        withContext(Dispatchers.IO) {
                            val messageDao = ServiceLocator.appDatabase.messageDao()
                            val remoteId = "confirm_$planId"
                            if (messageDao.getByRemoteId(remoteId) == null) {
                                val app = getApplication<Application>()
                                messageDao.insert(
                                    MessageEntity(
                                        msgType = MessageEntity.MSG_TYPE_SYSTEM,
                                        senderName = app.getString(R.string.hospital_remind_system_sender),
                                        content = app.getString(
                                            R.string.hospital_remind_confirm_msg, trimmed
                                        ),
                                        createTime = System.currentTimeMillis(),
                                        isRead = false,
                                        deviceSerial = deviceSn,
                                        remoteId = remoteId,
                                        sendStatus = MessageEntity.SEND_STATUS_SUCCESS,
                                        sendChannel = MessageEntity.CHANNEL_NONE
                                    )
                                )
                            }
                        }
                        toast(R.string.hospital_remind_pending_saved)
                        onSuccess()
                    } else {
                        toastText("保存失败，请稍后重试")
                    }
                } else {
                    val planId = withContext(Dispatchers.IO) {
                        repository.addLocalPlan(selected.deviceSn, draft)
                    }
                    if (planId > 0) {
                        // 本地提醒行（clockId 空）按草稿还原实体用于通知调度
                        LocalRemindScheduler.schedule(
                            getApplication(),
                            RemindPlanEntity(
                                id = planId,
                                tag = REMIND_TAG,
                                content = trimmed,
                                timeHour = timeHour,
                                timeMin = timeMin,
                                repeatType = RemindPlanEntity.REPEAT_ONCE,
                                weekdays = draft.weekdays.joinToString(","),
                                year = draft.year,
                                month = draft.month,
                                day = draft.day,
                                deviceSerial = selected.deviceSn,
                                createTime = System.currentTimeMillis(),
                                source = RemindPlanEntity.SOURCE_HOSPITAL_LOCAL
                            )
                        )
                        toast(R.string.hospital_remind_saved)
                        onSuccess()
                    } else {
                        toastText("保存失败，请稍后重试")
                    }
                }
            } finally {
                _isSaving.value = false
            }
        }
    }
}
