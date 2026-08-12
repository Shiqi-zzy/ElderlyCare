package com.ezvizpro.ui.family

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// ──── 告警等级色 ────
private val EmergencyRed = Color(0xFFDC2626)
private val HighOrange = Color(0xFFEA580C)
private val MediumYellow = Color(0xFFCA8A04)
private val LowBlue = Color(0xFF2563EB)
private val DarkBg = Color(0xFF0F172A)
private val CardBg = Color(0xFF1E293B)
private val TextPrimary = Color(0xFFE2E8F0)
private val TextSecondary = Color(0xFF94A3B8)
private val Accent = Color(0xFF3B82F6)

/**
 * 养老告警中心（独立子页面，从家属端 MainScreen 导航进入）
 *
 * 复用 FamilyViewModel 获取告警数据
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElderlyAlarmsScreen(
    onBack: () -> Unit,
    viewModel: FamilyViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.toastMessage) {
        state.toastMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearToast() }
    }

    Scaffold(
        containerColor = DarkBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("📋 告警中心", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = TextPrimary)
                    }
                },
                actions = {
                    if (state.isSimulating) {
                        CircularProgressIndicator(Modifier.size(20.dp), color = Accent, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    IconButton(onClick = { viewModel.simulateAlarm() }) {
                        Icon(Icons.Default.NotificationsActive, "模拟告警", tint = MediumYellow)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            // 老人选择器
            if (state.elderlyList.isNotEmpty()) {
                LazyColumn(
                    Modifier
                        .height(64.dp)
                        .padding(horizontal = 16.dp)
                ) {
                    items(state.elderlyList) { elderly ->
                        val selected = state.selectedElderly?.id == elderly.id
                        Surface(
                            onClick = { viewModel.selectElderly(elderly) },
                            shape = RoundedCornerShape(20.dp),
                            color = if (selected) Accent else CardBg,
                            contentColor = if (selected) Color.White else TextSecondary
                        ) {
                            Text(
                                elderly.name,
                                Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                fontSize = 13.sp
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                }
            }

            if (state.alarms.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📭", fontSize = 48.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("暂无告警", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                        Text("系统运行正常，老人状态良好", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.alarms) { alarm ->
                        AlarmCard(alarm) { viewModel.acknowledgeAlarm(alarm.id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlarmCard(alarm: com.ezvizpro.data.remote.dto.AlarmDto, onAck: () -> Unit) {
    val (levelColor, levelLabel) = when (alarm.alarmLevel) {
        "EMERGENCY" -> EmergencyRed to "紧急"
        "HIGH" -> HighOrange to "高"
        "MEDIUM" -> MediumYellow to "中"
        "LOW" -> LowBlue to "低"
        else -> TextSecondary to alarm.alarmLevel
    }
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = levelColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            levelLabel,
                            Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            color = levelColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        alarm.title,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                alarm.aiScore?.let { score ->
                    Text("AI ${"%.0f".format(score * 100)}%", color = TextSecondary, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    Text(alarmTypeLabel(alarm.alarmType), color = TextSecondary, fontSize = 12.sp)
                    Text(" · ", color = TextSecondary, fontSize = 12.sp)
                    Text(alarmStatusLabel(alarm.status), color = TextSecondary, fontSize = 12.sp)
                    Text(" · ", color = TextSecondary, fontSize = 12.sp)
                    Text(
                        alarm.createdAt.take(16).replace("T", " "),
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                if (alarm.status == "active") {
                    TextButton(
                        onClick = onAck,
                        colors = ButtonDefaults.textButtonColors(contentColor = Accent)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("确认", fontSize = 13.sp)
                    }
                }
            }
            if (alarm.aiVerified >= 2) {
                Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.VerifiedUser,
                        null,
                        tint = Color(0xFF22C55E),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("多帧复核已通过", color = Color(0xFF22C55E), fontSize = 11.sp)
                }
            }
        }
    }
}

private fun alarmTypeLabel(t: String) = when (t) {
    "fall" -> "跌倒检测"
    "stillness" -> "静止异常"
    "smoke" -> "烟感"
    "gas" -> "燃气"
    "vital_signs" -> "体征异常"
    "absence" -> "离床"
    else -> t
}

private fun alarmStatusLabel(s: String) = when (s) {
    "active" -> "待确认"
    "acknowledged" -> "已确认"
    "resolved" -> "已解决"
    "archived" -> "已归档"
    else -> s
}
