package com.elderlycare.app.ui.family

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.ui.components.RiskLevel
import com.elderlycare.app.ui.components.StatusBadge
import com.elderlycare.app.ui.theme.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertCenterScreen(onNavigateBack: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var retentionDays by remember { mutableIntStateOf(ServiceLocator.settingsStore.getAlarmRetentionDays()) }
    var showSettings by remember { mutableStateOf(false) }
    val tabs = listOf("全部", "跌倒", "久坐不动", "设备离线", "SOS求救")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("告警中心", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                actions = {
                    IconButton(onClick = { showSettings = true }) { Icon(Icons.Filled.Settings, "告警保留时限设置") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            TabRow(selectedTabIndex = selectedTab, containerColor = Surface, contentColor = Primary) {
                tabs.forEachIndexed { i, t -> Tab(selected = selectedTab == i, onClick = { selectedTab = i }, text = { Text(t, fontWeight = if (selectedTab == i) FontWeight.SemiBold else FontWeight.Normal) }) }
            }
            LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(mockAlerts.filter { tabs[selectedTab] == "全部" || it.type == tabs[selectedTab] }) { alert ->
                    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                        Row(modifier = Modifier.padding(14.dp)) {
                            if (alert.level == RiskLevel.RISK) { Box(modifier = Modifier.width(4.dp).height(60.dp).clip(RoundedCornerShape(2.dp)).background(alert.level.color)); Spacer(modifier = Modifier.width(10.dp)) }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(alert.title, fontWeight = FontWeight.SemiBold); Text(alert.time, style = MaterialTheme.typography.labelSmall, color = TextHint) }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(alert.desc, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, maxLines = 2)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row { StatusBadge(text = alert.level.label, color = alert.level.color); Spacer(modifier = Modifier.width(8.dp)); Text(alert.type, style = MaterialTheme.typography.labelSmall, color = Primary) }
                            }
                            TextButton(onClick = {}, modifier = Modifier.align(Alignment.CenterVertically)) { Text("查看回放", color = Primary) }
                        }
                    }
                }
            }
        }
    }

    if (showSettings) {
        var sliderValue by remember { mutableFloatStateOf(retentionDays.toFloat()) }
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("告警回放过期时限") },
            text = {
                Column {
                    Text("保留 ${sliderValue.roundToInt()} 天", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        valueRange = 1f..30f,
                        steps = 28
                    )
                    Text("超期告警片段将标记为已过期、不可回放", style = MaterialTheme.typography.labelSmall, color = TextHint)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val days = sliderValue.roundToInt()
                    retentionDays = days
                    ServiceLocator.settingsStore.setAlarmRetentionDays(days)
                    showSettings = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showSettings = false }) { Text("取消") }
            }
        )
    }
}

private data class MockAlert(val title: String, val desc: String, val time: String, val type: String, val level: RiskLevel)

private val mockAlerts = listOf(
    MockAlert("客厅区域跌倒检测", "AI检测到用户在客厅区域疑似跌倒，可信度87%", "今天 15:30", "跌倒", RiskLevel.RISK),
    MockAlert("厨房区域跌倒预警", "用户进入厨房后久未移动，请关注", "昨天 18:45", "跌倒", RiskLevel.ATTENTION),
    MockAlert("连续久坐提醒", "用户在沙发区域连续久坐超过4小时", "今天 12:00", "久坐不动", RiskLevel.ATTENTION),
    MockAlert("RK3设备离线", "RK3机器人（SN: RK3-2024-A1B2C3）离线5分钟", "今天 08:00", "设备离线", RiskLevel.NORMAL),
    MockAlert("SOS紧急呼叫", "用户按下RK3紧急呼救按钮", "昨天 21:15", "SOS求救", RiskLevel.RISK)
)
