package com.elderlycare.app.ui.shared

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.ezviz.model.AlarmMessage
import com.elderlycare.app.data.ezviz.model.AlarmType
import com.elderlycare.app.ui.components.InfoRow
import com.elderlycare.app.ui.ezviz.AlarmListScreen

/**
 * 社区/医院端「告警消息」。
 *
 * 复用 [AlarmListScreen]（点击直达详情，不做回放入口）：
 * - 列表数据由 AlarmListViewModel 按「当前 staff 的 ACTIVE 绑定老人 deviceSn」过滤，
 *   云端全量返回也不会看到其他设备告警；REVOKED 解绑后对应告警实时消失。
 * - 详情按 alarm.deviceSerial 匹配当前 staff 可访问老人档案，定位到老人与设备。
 */
@Composable
fun StaffAlarmScreen() {
    var selectedAlarm by remember { mutableStateOf<AlarmMessage?>(null) }
    var elderlyName by remember { mutableStateOf<String?>(null) }
    var deviceName by remember { mutableStateOf<String?>(null) }

    // 详情打开时按 deviceSerial 匹配当前 staff 可访问老人（权限过滤定位到老人）
    LaunchedEffect(selectedAlarm?.alarmId) {
        val alarm = selectedAlarm ?: return@LaunchedEffect
        val staff = ServiceLocator.staffUserStore.getCurrentStaffUser() ?: return@LaunchedEffect
        val match = ServiceLocator.bindingRepository.getAccessibleElderly(staff)
            .firstOrNull { it.profile.deviceSn == alarm.deviceSerial }
        elderlyName = match?.profile?.name ?: "未知老人"
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
                    InfoRow("老人", elderlyName ?: "加载中…")
                    InfoRow("设备", deviceName ?: alarm.deviceSerial)
                    InfoRow("时间", alarm.alarmTime)
                    InfoRow("类型", AlarmType.fromCode(alarm.alarmType).label)
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedAlarm = null }) { Text("关闭") }
            }
        )
    }
}
