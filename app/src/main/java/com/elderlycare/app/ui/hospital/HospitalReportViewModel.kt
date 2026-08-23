package com.elderlycare.app.ui.hospital

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.message.MessageEntity
import com.elderlycare.app.data.model.ElderlyProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * 医院端「本地版健康报告」ViewModel。
 *
 * 进入先校验 ACTIVE 医院授权（getAccessibleElderlyById 权限闸门）：
 * - 未授权 → authorized=false，页面占位「未获得家属授权，无法查看健康报告」；
 * - 已授权 → 订阅本地 Room 数据流：档案体征（observeProfiles 按 elderlyId）、
 *   告警消息（message 表 category=2，按档案设备 SN）、随访/建议计数。
 * 聚合展示在 Screen 复用家属端图表组件（Charts.kt/RiskLevelIndicator/StatusBadge），
 * 统计代码（healthCategory 等）沿用 ui/shared/ElderlyHealthStatus，本阶段不做跨端后端版。
 */
class HospitalReportViewModel(application: Application) : AndroidViewModel(application) {

    /** 授权状态：null=校验中；true=有 ACTIVE 授权；false=未授权 */
    private val _authorized = MutableStateFlow<Boolean?>(null)
    val authorized: StateFlow<Boolean?> = _authorized.asStateFlow()

    /** 老人档案（授权通过后订阅） */
    private val _profile = MutableStateFlow<ElderlyProfile?>(null)
    val profile: StateFlow<ElderlyProfile?> = _profile.asStateFlow()

    /** 老人设备的告警消息（本地 Room，category=2；未绑定设备为空列表） */
    private val _alerts = MutableStateFlow<List<MessageEntity>>(emptyList())
    val alerts: StateFlow<List<MessageEntity>> = _alerts.asStateFlow()

    /** 医疗随访记录数（医院端录入） */
    private val _followUpCount = MutableStateFlow(0)
    val followUpCount: StateFlow<Int> = _followUpCount.asStateFlow()

    /** 健康建议条数（医院端录入） */
    private val _adviceCount = MutableStateFlow(0)
    val adviceCount: StateFlow<Int> = _adviceCount.asStateFlow()

    private var started = false

    /** 进入即校验 ACTIVE 医院授权；通过后订阅本地聚合数据流（按 elderlyId） */
    fun start(elderlyId: String) {
        if (started || elderlyId.isBlank()) return
        started = true
        viewModelScope.launch {
            val staff = ServiceLocator.staffUserStore.getCurrentStaffUser()
            val ok = staff != null &&
                ServiceLocator.bindingRepository.getAccessibleElderlyById(staff, elderlyId) != null
            _authorized.value = ok
            if (!ok) return@launch

            val db = ServiceLocator.appDatabase
            val profileFlow = ServiceLocator.profileStore.observeProfiles()
                .map { list -> list.firstOrNull { it.userId == elderlyId } }

            launch { profileFlow.collect { _profile.value = it } }

            // 告警流跟随档案的设备 SN（解绑/换设备自动切换；未绑定 → 空）
            launch {
                profileFlow.flatMapLatest { p ->
                    val sn = p?.deviceSn.orEmpty()
                    if (sn.isBlank()) flowOf(emptyList())
                    else db.messageDao().observeByDeviceSerialAndCategory(
                        sn, MessageEntity.MESSAGE_CATEGORY_ALERT
                    )
                }.collect { _alerts.value = it }
            }

            launch {
                db.medicalFollowUpDao().observeCountByElderlyId(elderlyId)
                    .collect { _followUpCount.value = it }
            }
            launch {
                db.healthAdviceDao().observeCountByElderlyId(elderlyId)
                    .collect { _adviceCount.value = it }
            }
        }
    }
}
