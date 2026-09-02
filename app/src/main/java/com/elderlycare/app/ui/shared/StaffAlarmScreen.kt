package com.elderlycare.app.ui.shared

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.elderlycare.app.data.binding.AlertStatus
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.ezviz.model.AlarmMessage
import com.elderlycare.app.data.ezviz.model.AlarmType
import com.elderlycare.app.data.model.ElderlyProfile
import com.elderlycare.app.ui.components.InfoRow
import com.elderlycare.app.ui.ezviz.AlarmListScreen
import com.elderlycare.app.ui.theme.Primary
import kotlinx.coroutines.launch

/**
 * 社区/医院端「告警消息」。
 *
 * 复用 [AlarmListScreen]（点击直达详情，不做回放入口）：
 * - 列表数据由 AlarmListViewModel 按「当前 staff 的 ACTIVE 绑定照护对象 deviceSn」过滤，
 *   云端全量返回也不会看到其他设备告警；REVOKED 解绑后对应告警实时消失。
 * - 详情按 alarm.deviceSerial 匹配当前 staff 可访问照护对象档案，定位到照护对象与设备。
 * - 「标记已处理」：更新本地告警表（LocalAlertEntity）状态 + 档案 alertActive=false，
 *   社区花名册红色「异常」角标随之恢复（正常→异常→已处理闭环）。
 */
@Composable
fun StaffAlarmScreen() {
    val scope = rememberCoroutineScope()
    var selectedAlarm by remember { mutableStateOf<AlarmMessage?>(null) }
    var elderlyName by remember { mutableStateOf<String?>(null) }
    var deviceName by remember { mutableStateOf<String?>(null) }
    var matchProfile by remember { mutableStateOf<ElderlyProfile?>(null) }

    // 详情打开时按 deviceSerial 匹配当前 staff 可访问照护对象（权限过滤定位到照护对象）
    LaunchedEffect(selectedAlarm?.alarmId) {
        val alarm = selectedAlarm ?: return@LaunchedEffect
        val staff = ServiceLocator.staffUserStore.getCurrentStaffUser() ?: return@LaunchedEffect
        val match = ServiceLocator.bindingRepository.getAccessibleElderly(staff)
            .firstOrNull { it.profile.deviceSn == alarm.deviceSerial }
        matchProfile = match?.profile
        elderlyName = match?.profile?.name ?: "未知照护对象"
        deviceName = alarm.deviceName ?: alarm.deviceSerial
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AlarmListScreen(
            onAlarmClick = { selectedAlarm = it }
        )
    }

    selectedAlarm?.let { alarm ->
        AlertDialog(
            onDismissRequest = { selectedAlarm = null },
            title = { Text(alarm.alarmName) },
            text = {
                Column {
                    InfoRow("照护对象", elderlyName ?: "加载中…")
                    InfoRow("设备", deviceName ?: alarm.deviceSerial)
                    InfoRow("时间", alarm.alarmTime)
                    InfoRow("类型", AlarmType.fromCode(alarm.alarmType).label)
                }
            },
            confirmButton = {
                Row {
                    TextButton(
                        onClick = {
                            scope.launch {
                                // 本地告警表：alarmId 精确匹配 + 按设备兜底（非 WS 通道到达的告警）
                                ServiceLocator.bindingDao.updateAlertStatus(alarm.alarmId, AlertStatus.HANDLED.name)
                                ServiceLocator.bindingDao.updateAlertsByDevice(alarm.deviceSerial, AlertStatus.HANDLED.name)
                                // 档案异常状态复位 → 花名册「异常」角标消失
                                matchProfile?.let {
                                    ServiceLocator.profileStore.saveProfile(it.copy(alertActive = false))
                                }
                                selectedAlarm = null
                            }
                        }
                    ) { Text("标记已处理", color = Primary) }
                    TextButton(onClick = { selectedAlarm = null }) { Text("关闭") }
                }
            }
        )
    }
}
