package com.ezvizpro.ui.family

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyMainScreen(
    onLogout: () -> Unit,
    viewModel: FamilyViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(state.toastMessage) {
        state.toastMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearToast() }
    }

    Scaffold(
        containerColor = DarkBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("🏠 家属端", color = TextPrimary) },
                actions = {
                    if (state.isSimulating) {
                        CircularProgressIndicator(Modifier.size(20.dp), color = Accent, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    IconButton(onClick = { viewModel.simulateAlarm() }) {
                        Icon(Icons.Default.NotificationsActive, "模拟告警", tint = MediumYellow)
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, "退出", tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            // ── 老人选择器 ──
            if (state.elderlyList.isNotEmpty()) {
                LazyColumn(Modifier.height(64.dp).padding(horizontal = 16.dp)) {
                    items(state.elderlyList) { elderly ->
                        val selected = state.selectedElderly?.id == elderly.id
                        Chip(
                            elderly.name,
                            selected = selected,
                            onClick = { viewModel.selectElderly(elderly) }
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                }
            }
            // ── Tab 栏 ──
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF0F172A),
                contentColor = Accent
            ) {
                Tab(selectedTab == 0, onClick = { selectedTab = 0 }) { Text("📋 告警中心", modifier = Modifier.padding(12.dp)) }
                Tab(selectedTab == 1, onClick = { selectedTab = 1 }) { Text("🔐 授权管理", modifier = Modifier.padding(12.dp)) }
                Tab(selectedTab == 2, onClick = { selectedTab = 2 }) { Text("🛡 隐私控制", modifier = Modifier.padding(12.dp)) }
                Tab(selectedTab == 3, onClick = { selectedTab = 3 }) { Text("📋 健康档案", modifier = Modifier.padding(12.dp)) }
            }

            when (selectedTab) {
                0 -> AlarmTab(state, viewModel)
                1 -> AuthorizationTab(state, viewModel)
                2 -> PrivacyTab(state, viewModel)
                3 -> HealthRecordsTab(state, viewModel)
            }
        }
    }
}

// ═══════════════════ 告警 Tab ═══════════════════
@Composable
private fun AlarmTab(state: FamilyUiState, viewModel: FamilyViewModel) {
    if (state.alarms.isEmpty()) {
        EmptyPlaceholder("暂无告警", "系统运行正常，老人状态良好")
    } else {
        LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(state.alarms) { alarm ->
                AlarmCard(alarm) { viewModel.acknowledgeAlarm(alarm.id) }
            }
        }
    }
}

@Composable
private fun AlarmCard(alarm: com.ezvizpro.data.remote.dto.AlarmDto, onAck: () -> Unit) {
    val (levelColor, levelLabel) = alarmLevelStyle(alarm.alarmLevel)
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(6.dp), color = levelColor.copy(alpha = 0.2f)) {
                        Text(levelLabel, Modifier.padding(horizontal = 8.dp, vertical = 3.dp), color = levelColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(alarm.title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                // AI 分数
                alarm.aiScore?.let { score ->
                    Text("AI ${"%.0f".format(score * 100)}%", color = TextSecondary, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row {
                    Text(alarmTypeLabel(alarm.alarmType), color = TextSecondary, fontSize = 12.sp)
                    Text(" · ", color = TextSecondary, fontSize = 12.sp)
                    Text(alarmStatusLabel(alarm.status), color = TextSecondary, fontSize = 12.sp)
                    Text(" · ", color = TextSecondary, fontSize = 12.sp)
                    Text(alarm.createdAt.take(16).replace("T", " "), color = TextSecondary, fontSize = 11.sp)
                }
                if (alarm.status == "active") {
                    TextButton(onClick = onAck, colors = ButtonDefaults.textButtonColors(contentColor = Accent)) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("确认", fontSize = 13.sp)
                    }
                }
            }
            // AI 核查标识
            if (alarm.aiVerified >= 2) {
                Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VerifiedUser, null, tint = Color(0xFF22C55E), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("多帧复核已通过", color = Color(0xFF22C55E), fontSize = 11.sp)
                }
            }
        }
    }
}

// ═══════════════════ 授权 Tab ═══════════════════
@Composable
private fun AuthorizationTab(state: FamilyUiState, viewModel: FamilyViewModel) {
    var showGrantDialog by remember { mutableStateOf(false) }
    var grantUserId by remember { mutableStateOf("") }
    var grantType by remember { mutableStateOf("monitoring") }
    var grantUntil by remember { mutableStateOf("") }

    Column(Modifier.padding(16.dp)) {
        Button(
            onClick = { showGrantDialog = true },
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Accent)
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp)); Text("新建授权")
        }
        Spacer(Modifier.height(12.dp))

        if (state.authorizations.isEmpty()) {
            EmptyPlaceholder("暂无授权记录", "点击上方按钮为社区/医院人员授权")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.authorizations) { auth ->
                    AuthCard(auth) { viewModel.revokeAuthorization(auth.id) }
                }
            }
        }
    }

    if (showGrantDialog) {
        AlertDialog(
            onDismissRequest = { showGrantDialog = false },
            title = { Text("新建授权") },
            text = {
                Column {
                    OutlinedTextField(grantUserId, { grantUserId = it }, label = { Text("被授权用户UUID") }, singleLine = true, isError = grantUserId.isBlank())
                    Spacer(Modifier.height(8.dp))
                    Text("授权类型", color = TextSecondary, fontSize = 13.sp)
                    Row {
                        listOf("monitoring" to "监控", "health_records" to "健康", "all" to "全部").forEach { (k, v) ->
                            FilterChip(selected = grantType == k, onClick = { grantType = k }, label = { Text(v) }, modifier = Modifier.padding(2.dp))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(grantUntil, { grantUntil = it }, label = { Text("有效期至 (YYYY-MM-DD，留空默认30天)") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.grantAuthorization(grantUserId, grantType, grantType, grantUntil)
                        showGrantDialog = false
                    },
                    enabled = grantUserId.isNotBlank()
                ) { Text("确认") }
            },
            dismissButton = { TextButton(onClick = { showGrantDialog = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun AuthCard(auth: com.ezvizpro.data.remote.dto.AuthorizationDto, onRevoke: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = CardBg)) {
        Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("👤 ${auth.granteeName.ifEmpty { "用户" }}", color = TextPrimary, fontWeight = FontWeight.Medium)
                Text(auth.granteePhone.ifEmpty { "无电话" }, color = TextSecondary, fontSize = 12.sp)
                Row {
                    Surface(shape = RoundedCornerShape(4.dp), color = Accent.copy(alpha = 0.15f)) {
                        Text(permLabel(auth.permissionType), Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Accent, fontSize = 11.sp)
                    }
                    Spacer(Modifier.width(6.dp))
                    Surface(shape = RoundedCornerShape(4.dp), color = statusColor(auth.status).copy(alpha = 0.15f)) {
                        Text(authStatusLabel(auth.status), Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = statusColor(auth.status), fontSize = 11.sp)
                    }
                }
                Text("至 ${auth.effectiveUntil.take(10)}", color = TextSecondary, fontSize = 11.sp)
            }
            if (auth.status == "active") {
                IconButton(onClick = onRevoke) { Icon(Icons.Default.Cancel, "撤销", tint = Color(0xFFEF4444)) }
            }
        }
    }
}

// ═══════════════════ 隐私 Tab ═══════════════════
@Composable
private fun PrivacyTab(state: FamilyUiState, viewModel: FamilyViewModel) {
    val elderly = state.selectedElderly ?: return
    // Load active codes when tab is shown
    LaunchedEffect(Unit) { viewModel.loadActiveCodes() }
    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(32.dp))
        Icon(
            if (state.privacyPaused) Icons.Default.VisibilityOff else Icons.Default.Visibility,
            null, modifier = Modifier.size(72.dp), tint = if (state.privacyPaused) Color(0xFFF59E0B) else Color(0xFF22C55E)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            if (state.privacyPaused) "监控已暂停" else "监控运行中",
            color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold
        )
        Text(
            if (state.privacyPaused) "摄像头与传感器数据暂不上报，家属可随时恢复" else "设备在线，数据正常采集上报",
            color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = { viewModel.togglePrivacy() },
            Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (state.privacyPaused) Color(0xFF22C55E) else Color(0xFFF59E0B)
            )
        ) {
            Icon(
                if (state.privacyPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                null, modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(if (state.privacyPaused) "恢复监控" else "暂停监控", fontSize = 16.sp)
        }
        // 设备列表
        if (state.devices.isNotEmpty()) {
            Spacer(Modifier.height(28.dp))
            Text("绑定设备 (${state.devices.size})", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.align(Alignment.Start))
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.devices) { device ->
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = CardBg)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(deviceIcon(device.deviceType), null, tint = if (device.status == "online") Color(0xFF22C55E) else TextSecondary)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(device.deviceName, color = TextPrimary, fontWeight = FontWeight.Medium)
                                Text("${deviceTypeLabel(device.deviceType)} · ${if (device.status == "online") "在线" else "离线"}", color = TextSecondary, fontSize = 12.sp)
                            }
                            IconButton(onClick = { viewModel.generateDeviceCode(device.id) }) {
                                Icon(Icons.Default.Share, "生成验证码", tint = Accent)
                            }
                        }
                    }
                }
            }
        }

        // 活跃验证码
        if (state.activeCodes.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Text("活跃验证码 (${state.activeCodes.size})", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.align(Alignment.Start))
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(state.activeCodes) { vc ->
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = CardBg)) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("📱 ${vc.deviceName} · 👴 ${vc.elderlyName}", color = TextPrimary, fontSize = 13.sp)
                                Text("验证码: ${vc.code} · 有效期至 ${vc.expiresAt.take(16)}", color = TextSecondary, fontSize = 11.sp)
                            }
                            IconButton(onClick = { viewModel.revokeDeviceCode(vc.id) }) {
                                Icon(Icons.Default.Cancel, "撤销", tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // ── 验证码弹窗 ──
    if (state.showCodeDialog && state.generatedCode != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissCodeDialog() },
            containerColor = CardBg,
            title = { Text("📱 设备验证码", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("请将以下验证码告知社区/医院工作人员", color = TextSecondary, fontSize = 14.sp)
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Accent.copy(alpha = 0.1f),
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Text(
                            state.generatedCode!!.code,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            color = Accent,
                            letterSpacing = 10.sp,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("有效期至: ${state.generatedCode!!.expiresAt.take(16).replace("T", " ")}", color = TextSecondary, fontSize = 12.sp)
                    Text("设备: ${state.generatedCode!!.deviceName} · 老人: ${state.generatedCode!!.elderlyName}", color = TextSecondary, fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.dismissCodeDialog() }, colors = ButtonDefaults.buttonColors(containerColor = Accent)) {
                    Text("关闭")
                }
            }
        )
    }
}

// ═══════════════════ 健康档案 Tab（Phase 3 — 只读） ═══════════════════
@Composable
private fun HealthRecordsTab(state: FamilyUiState, viewModel: FamilyViewModel) {
    val elderly = state.selectedElderly
    LaunchedEffect(elderly) {
        elderly?.let { viewModel.loadHealthRecords(it.id) }
    }

    if (elderly == null) {
        EmptyPlaceholder("请先选择老人", "在顶部选择老人后查看健康档案")
        return
    }

    if (state.healthRecords.isEmpty()) {
        EmptyPlaceholder("暂无健康档案", "医院尚未为此老人录入健康档案")
        return
    }

    LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(
                "共 ${state.healthRecords.size} 条记录",
                color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium
            )
        }
        items(state.healthRecords) { record ->
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val rtColor = when (record.recordType) {
                                "诊断" -> EmergencyRed; "处方" -> Accent; "检查报告" -> MediumYellow
                                "用药记录" -> Color(0xFF22C55E); "疫苗接种" -> Color(0xFF8B5CF6); else -> TextSecondary
                            }
                            Surface(shape = RoundedCornerShape(6.dp), color = rtColor.copy(alpha = 0.2f)) {
                                Text(
                                    record.recordType,
                                    Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    color = rtColor, fontSize = 11.sp, fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(record.recordDate.take(10), color = TextSecondary, fontSize = 12.sp)
                        }
                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF22C55E).copy(alpha = 0.1f)) {
                            Text(
                                if (record.visibility == "both") "家属可见" else record.visibility,
                                Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = Color(0xFF22C55E), fontSize = 10.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(record.contentJson, color = TextPrimary, fontSize = 14.sp, maxLines = 5, overflow = TextOverflow.Ellipsis)
                    if (record.doctorName.isNotBlank() || record.hospitalName.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Row {
                            if (record.doctorName.isNotBlank()) {
                                Text("👨‍⚕ ${record.doctorName}", color = TextSecondary, fontSize = 11.sp)
                            }
                            if (record.hospitalName.isNotBlank()) {
                                Spacer(Modifier.width(12.dp))
                                Text("🏥 ${record.hospitalName}", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════ 复用组件 ═══════════════════
@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) Accent else CardBg,
        contentColor = if (selected) Color.White else TextSecondary
    ) {
        Text(label, Modifier.padding(horizontal = 14.dp, vertical = 6.dp), fontSize = 13.sp)
    }
}

@Composable
private fun EmptyPlaceholder(title: String, subtitle: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📭", fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text(title, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = TextSecondary, fontSize = 13.sp)
        }
    }
}

// ========== 标签映射 ==========
private fun alarmLevelStyle(level: String): Pair<Color, String> = when (level) {
    "EMERGENCY" -> EmergencyRed to "紧急"
    "HIGH" -> HighOrange to "高"
    "MEDIUM" -> MediumYellow to "中"
    "LOW" -> LowBlue to "低"
    else -> TextSecondary to level
}
private fun alarmTypeLabel(t: String) = when (t) {
    "fall" -> "跌倒检测"; "stillness" -> "静止异常"; "smoke" -> "烟感"; "gas" -> "燃气"; "vital_signs" -> "体征异常"; "absence" -> "离床"; else -> t
}
private fun alarmStatusLabel(s: String) = when (s) {
    "active" -> "待确认"; "acknowledged" -> "已确认"; "resolved" -> "已解决"; "archived" -> "已归档"; else -> s
}
private fun permLabel(p: String) = when (p) {
    "monitoring" -> "监控"; "health_records" -> "健康"; "alarm_video" -> "告警视频"; "all" -> "全部数据"; else -> p
}
private fun authStatusLabel(s: String) = when (s) {
    "active" -> "有效"; "revoked" -> "已撤销"; "expired" -> "已过期"; "rejected" -> "已拒绝"; else -> s
}
private fun statusColor(s: String) = when (s) {
    "active" -> Color(0xFF22C55E); "revoked" -> Color(0xFFEF4444); "expired" -> TextSecondary; else -> TextSecondary
}
private fun deviceIcon(t: String): ImageVector = when (t) {
    "camera" -> Icons.Default.Videocam; "wearable" -> Icons.Default.Watch; "bed_sensor" -> Icons.Default.Bed; "smoke_detector" -> Icons.Default.LocalFireDepartment; else -> Icons.Default.DevicesOther
}
private fun deviceTypeLabel(t: String) = when (t) {
    "camera" -> "摄像头"; "wearable" -> "穿戴设备"; "bed_sensor" -> "床垫传感器"; "smoke_detector" -> "烟感"; "gas_detector" -> "燃气"; else -> t
}
