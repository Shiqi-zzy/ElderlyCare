package com.ezvizpro.ui.hospital

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.ezvizpro.ui.theme.Blue600
import com.ezvizpro.ui.theme.Green500
import com.ezvizpro.ui.theme.Orange500
import com.ezvizpro.ui.theme.Red500
import com.ezvizpro.ui.theme.Gray600

private val AccentColor = Blue600
private val SuccessColor = Green500
private val WarningColor = Orange500
private val ErrorColor = Red500
private val SubtleText = Gray600
private val HospitalAccent = Red500

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HospitalMainScreen(
    token: String,
    onLogout: () -> Unit,
    onBack: () -> Unit = {},
    viewModel: HospitalViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(token) { viewModel.setToken(token) }
    LaunchedEffect(state.toastMessage) {
        state.toastMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearToast() }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("🏥 医院端", color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) { Icon(Icons.Default.Logout, "退出", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            // ── 概览卡片 ──
            Card(
                Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    StatItem("👴", "绑定老人", "${state.boundElderlyCount}", AccentColor)
                    StatItem("📹", "已绑设备", "${state.boundDevices.size}", SuccessColor)
                    StatItem("🔖", "授权申请", "${state.authRequests.size}", HospitalAccent)
                }
            }
            // ── Tab ──
            TabRow(selectedTabIndex = selectedTab, containerColor = MaterialTheme.colorScheme.background, contentColor = HospitalAccent) {
                Tab(selectedTab == 0, onClick = { selectedTab = 0 }) { Text("👴 绑定老人", Modifier.padding(10.dp)) }
                Tab(selectedTab == 1, onClick = { selectedTab = 1 }) { Text("📋 健康档案", Modifier.padding(10.dp)) }
                Tab(selectedTab == 2, onClick = { selectedTab = 2 }) { Text("🚨 急救权限", Modifier.padding(10.dp)) }
                Tab(selectedTab == 3, onClick = { selectedTab = 3; viewModel.loadAuthRequests() }) { Text("🔖 申请授权", Modifier.padding(10.dp)) }
                Tab(selectedTab == 4, onClick = { selectedTab = 4 }) { Text("📹 设备管理", Modifier.padding(10.dp)) }
            }
            when (selectedTab) {
                0 -> BoundElderlyTab(state)
                1 -> HealthRecordTab(state, viewModel)
                2 -> EmergencyPermissionTab(state, viewModel)
                3 -> HospitalAuthRequestTab(state, viewModel)
                4 -> HospitalDeviceBindTab(state, viewModel)
            }
        }
    }
}

@Composable
private fun StatItem(emoji: String, label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 24.sp)
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ═══════════════════ 绑定老人 ═══════════════════
@Composable
private fun BoundElderlyTab(state: HospitalUiState) {
    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = HospitalAccent) }
        return
    }
    if (state.elderlyList.isEmpty()) {
        EmptyPlaceholder("暂无绑定老人", "尚未建立诊疗绑定关系，请联系家属授权")
        return
    }
    LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(state.elderlyList) { elderly ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(Modifier.size(44.dp), shape = RoundedCornerShape(22.dp), color = HospitalAccent.copy(alpha = 0.15f)) {
                        Box(contentAlignment = Alignment.Center) { Text("👴", fontSize = 22.sp) }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(elderly.name, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                        Text("${elderly.gender.ifEmpty { "—" }} · 出生: ${elderly.birthDate.ifEmpty { "—" }}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        Text(
                            "病历: ${elderly.medicalHistory.ifEmpty { "暂无记录" }}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(shape = RoundedCornerShape(6.dp), color = SuccessColor.copy(alpha = 0.15f)) {
                        Text("已绑定", Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = SuccessColor, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// ═══════════════════ 健康档案 ═══════════════════
@Composable
private fun HealthRecordTab(state: HospitalUiState, viewModel: HospitalViewModel) {
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedElderlyId by remember { mutableStateOf(state.selectedHealthElderlyId) }

    Column(Modifier.fillMaxSize()) {
        // 老人选择器
        if (state.elderlyList.isNotEmpty()) {
            LazyRow(
                Modifier.height(56.dp).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(state.elderlyList) { elderly ->
                    val isSelected = elderly.id == selectedElderlyId
                    Surface(
                        onClick = {
                            selectedElderlyId = elderly.id
                            viewModel.loadHealthRecords(elderly.id)
                        },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) HospitalAccent else MaterialTheme.colorScheme.surface,
                        contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Text(
                            "👴 ${elderly.name}",
                            Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        if (state.healthRecords.isEmpty()) {
            EmptyPlaceholder("暂无健康档案", if (selectedElderlyId.isBlank()) "请先选择老人" else "该老人暂无健康档案，点击下方按钮录入")
        } else {
            LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.healthRecords) { record ->
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(shape = RoundedCornerShape(6.dp), color = recordTypeColor(record.recordType).copy(alpha = 0.15f)) {
                                        Text(
                                            recordTypeLabel(record.recordType),
                                            Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            color = recordTypeColor(record.recordType), fontSize = 11.sp, fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(record.recordDate.take(10), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                }
                                Surface(shape = RoundedCornerShape(4.dp), color = if (record.visibility == "both") SuccessColor.copy(alpha = 0.1f) else WarningColor.copy(alpha = 0.1f)) {
                                    Text(
                                        if (record.visibility == "both") "家属可见" else "仅医院",
                                        Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        color = if (record.visibility == "both") SuccessColor else WarningColor,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(record.contentJson, color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp, maxLines = 5, overflow = TextOverflow.Ellipsis)
                            if (record.doctorName.isNotBlank() || record.hospitalName.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Row {
                                    if (record.doctorName.isNotBlank()) {
                                        Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(2.dp))
                                        Text(record.doctorName, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                    }
                                    if (record.hospitalName.isNotBlank()) {
                                        Spacer(Modifier.width(12.dp))
                                        Icon(Icons.Default.LocalHospital, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(2.dp))
                                        Text(record.hospitalName, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 录入按钮
        if (selectedElderlyId.isNotBlank()) {
            Button(
                onClick = { showAddDialog = true },
                Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HospitalAccent)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("录入健康档案")
            }
        }
    }

    // 录入档案对话框
    if (showAddDialog) {
        var recType by remember { mutableStateOf("诊断") }
        var recContent by remember { mutableStateOf("") }
        var recDoctor by remember { mutableStateOf("") }
        var recHospital by remember { mutableStateOf("") }
        var recVisibility by remember { mutableStateOf("both") }
        var recDate by remember { mutableStateOf(java.time.LocalDate.now().toString()) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("录入健康档案", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("档案类型", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("诊断", "处方", "检查报告", "用药记录", "疫苗接种").forEach { t ->
                            FilterChip(
                                selected = recType == t,
                                onClick = { recType = t },
                                label = { Text(t, fontSize = 12.sp) }
                            )
                        }
                    }
                    OutlinedTextField(recDate, { recDate = it }, label = { Text("记录日期") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(recDoctor, { recDoctor = it }, label = { Text("医生姓名") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(recHospital, { recHospital = it }, label = { Text("医院名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        recContent, { recContent = it },
                        label = { Text("档案内容（JSON或自由文本）") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("可见范围", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("both" to "家属可见", "hospital" to "仅医院").forEach { (k, v) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = recVisibility == k, onClick = { recVisibility = k })
                                Text(v, fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addHealthRecord(
                            elderlyId = selectedElderlyId,
                            recordType = recType,
                            contentJson = recContent,
                            doctorName = recDoctor,
                            hospitalName = recHospital,
                            visibility = recVisibility
                        )
                        showAddDialog = false
                    },
                    enabled = recContent.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = HospitalAccent)
                ) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("取消") } }
        )
    }
}

// ═══════════════════ 急救权限 ═══════════════════
@Composable
private fun EmergencyPermissionTab(state: HospitalUiState, viewModel: HospitalViewModel) {
    var requestElderlyId by remember { mutableStateOf("") }
    var requestReason by remember { mutableStateOf("") }
    var showConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadEmergencyStatus() }

    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        // 急救状态卡片
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (state.emergencyStatus?.active == true)
                    SuccessColor.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
            )
        ) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    if (state.emergencyStatus?.active == true) Icons.Default.VerifiedUser else Icons.Default.LocalHospital,
                    null,
                    modifier = Modifier.size(56.dp),
                    tint = if (state.emergencyStatus?.active == true) SuccessColor else HospitalAccent.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    if (state.emergencyStatus?.active == true) "急救权限已激活" else "暂无活跃急救权限",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp, fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                if (state.emergencyStatus?.active == true) {
                    val expiresAt = state.emergencyStatus!!.expiresAt
                    Text(
                        "👴 ${state.emergencyStatus!!.elderlyName.ifEmpty { "老人" }} · 有效期至 ${expiresAt.take(16).replace("T", " ")}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Surface(shape = RoundedCornerShape(8.dp), color = SuccessColor.copy(alpha = 0.12f)) {
                        Text(
                            "可临时查看监控视频、告警数据、健康档案",
                            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = SuccessColor, fontSize = 12.sp
                        )
                    }
                } else {
                    Text(
                        "紧急情况下可发起24小时临时监控权限请求\n家属端即时收到通知，审批后自动在24小时后过期",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // 请求表单
        if (state.emergencyStatus?.active != true) {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp)) {
                    Text("发起急救权限请求", color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))

                    Text("选择老人", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    if (state.elderlyList.isEmpty()) {
                        Text("暂无可选择的绑定老人", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    } else {
                        LazyColumn(Modifier.height(120.dp)) {
                            items(state.elderlyList) { elderly ->
                                Row(
                                    Modifier.fillMaxWidth().clickable { requestElderlyId = elderly.id }.padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = requestElderlyId == elderly.id,
                                        onClick = { requestElderlyId = elderly.id }
                                    )
                                    Text("  ${elderly.name} (${elderly.careLevel})", color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        requestReason, { requestReason = it },
                        label = { Text("急救理由") },
                        placeholder = { Text("如：老人突发胸痛需紧急查看体征数据") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { showConfirmDialog = true },
                        Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HospitalAccent),
                        enabled = requestElderlyId.isNotBlank() && requestReason.isNotBlank()
                    ) {
                        Icon(Icons.Default.Warning, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("发起急救权限请求")
                    }
                }
            }
        }
    }

    // 确认对话框
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            icon = { Icon(Icons.Default.Warning, null, tint = ErrorColor) },
            title = { Text("确认发起急救权限请求？") },
            text = {
                Text(
                    "将向老人家属发送24小时临时监控权限请求。家属审批后，您将可临时查看监控视频、告警数据和健康档案。权限将在24小时后自动失效。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.requestEmergencyAccess(requestElderlyId, requestReason)
                        showConfirmDialog = false
                        requestReason = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HospitalAccent)
                ) { Text("确认发起") }
            },
            dismissButton = { TextButton(onClick = { showConfirmDialog = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun recordTypeLabel(t: String) = when (t) {
    "诊断" -> "诊断"; "处方" -> "处方"; "检查报告" -> "检查报告"; "用药记录" -> "用药记录"; "疫苗接种" -> "疫苗接种"
    else -> t
}

@Composable
private fun recordTypeColor(t: String): Color = when (t) {
    "诊断" -> HospitalAccent; "处方" -> Blue600; "检查报告" -> WarningColor; "用药记录" -> SuccessColor; "疫苗接种" -> Color(0xFF8B5CF6)
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

// ═══════════════════ 授权申请 Tab ═══════════════════
@Composable
private fun HospitalAuthRequestTab(state: HospitalUiState, viewModel: HospitalViewModel) {
    var showRequestDialog by remember { mutableStateOf(false) }
    var requestElderlyId by remember { mutableStateOf("") }
    var requestPermType by remember { mutableStateOf("monitoring") }
    var requestDataScope by remember { mutableStateOf("""{"video":true,"alarm":true}""") }

    LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // 已发起的申请
        if (state.authRequests.isNotEmpty()) {
            item { Text("我的授权申请", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
            items(state.authRequests) { req ->
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("👴 ${req.elderlyName.ifEmpty { "老人" }}", color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(permLabel(req.permissionType) + " · ${authStatusLabel(req.status)}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            Text(req.createdAt.take(16).replace("T", " "), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        }
                        Surface(shape = RoundedCornerShape(6.dp), color = when(req.status) {
                            "pending" -> WarningColor.copy(alpha = 0.15f)
                            "active" -> SuccessColor.copy(alpha = 0.15f)
                            "rejected" -> ErrorColor.copy(alpha = 0.15f)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                        }) {
                            Text(authStatusLabel(req.status), Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = when(req.status) {
                                    "pending" -> WarningColor; "active" -> SuccessColor; "rejected" -> ErrorColor
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }, fontSize = 11.sp)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }

        // 新建申请按钮
        item {
            Button(
                onClick = { showRequestDialog = true },
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HospitalAccent)
            ) {
                Icon(Icons.Default.Send, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("发起新授权申请")
            }
        }
    }

    if (showRequestDialog) {
        AlertDialog(
            onDismissRequest = { showRequestDialog = false },
            title = { Text("申请老人数据访问授权") },
            text = {
                Column {
                    Text("选择老人", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    state.elderlyList.takeIf { it.isNotEmpty() }?.let { list ->
                        list.forEach { elderly ->
                            Row(Modifier.fillMaxWidth().clickable { requestElderlyId = elderly.id }.padding(vertical = 4.dp)) {
                                RadioButton(
                                    selected = requestElderlyId == elderly.id,
                                    onClick = { requestElderlyId = elderly.id }
                                )
                                Text("  ${elderly.name} (${elderly.careLevel})", color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(start = 4.dp))
                            }
                        }
                    } ?: Text("暂无可申请的绑定老人\n请联系家属先建立绑定关系", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)

                    Spacer(Modifier.height(8.dp))
                    Text("授权类型", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    Row {
                        listOf("monitoring" to "监控", "health_records" to "健康", "all" to "全部").forEach { (k, v) ->
                            FilterChip(
                                selected = requestPermType == k,
                                onClick = { requestPermType = k; requestDataScope = when(k) {
                                    "monitoring" -> """{"video":true,"alarm":true}"""
                                    "health_records" -> """{"medical":true}"""
                                    "all" -> """{"video":true,"medical":true,"alarm":true}"""
                                    else -> """{"video":true,"alarm":true}"""
                                }},
                                label = { Text(v) },
                                modifier = Modifier.padding(2.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("申请通过后可访问：${requestDataScope}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.requestAuthorization(requestElderlyId, requestPermType, requestDataScope)
                        showRequestDialog = false
                    },
                    enabled = requestElderlyId.isNotBlank()
                ) { Text("提交申请") }
            },
            dismissButton = { TextButton(onClick = { showRequestDialog = false }) { Text("取消") } }
        )
    }
}

// ═══════════════════ 设备绑定 Tab ═══════════════════
@Composable
private fun HospitalDeviceBindTab(state: HospitalUiState, viewModel: HospitalViewModel) {
    var codeInput by remember { mutableStateOf("") }

    LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // 验证码输入
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp)) {
                    Text("📱 输入验证码绑定设备", color = MaterialTheme.colorScheme.onBackground, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text("请联系家属获取6位设备验证码", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = codeInput,
                            onValueChange = { if (it.length <= 6) codeInput = it },
                            label = { Text("6位验证码") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                                cursorColor = HospitalAccent,
                                focusedBorderColor = HospitalAccent,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { viewModel.bindDeviceByCode(codeInput); codeInput = "" },
                            enabled = codeInput.length == 6,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HospitalAccent)
                        ) { Text("绑定") }
                    }
                }
            }
        }

        // 按老人查看已绑设备
        if (state.elderlyList.isNotEmpty()) {
            item { Text("已绑定老人的设备", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Medium) }

            state.elderlyList.forEach { elderly ->
                item {
                    Surface(
                        onClick = { viewModel.loadBoundDevices(elderly.id) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (state.selectedDeviceElderlyId == elderly.id) HospitalAccent.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("👴 ${elderly.name}", color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            Text("查看设备 ▼", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                    }
                }

                if (elderly.id == state.selectedDeviceElderlyId && state.boundDevices.isNotEmpty()) {
                    items(state.boundDevices) { device ->
                        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    deviceIcon(device.deviceType), null,
                                    tint = if (device.status == "online") SuccessColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(device.deviceName, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                    Text(
                                        "${deviceTypeLabel(device.deviceType)} · ${device.location.ifEmpty { "—" }} · ${if (device.status == "online") "在线" else "离线"}",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

                if (elderly.id == state.selectedDeviceElderlyId && state.boundDevices.isEmpty()) {
                    item {
                        Text("该老人暂无绑定设备", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(start = 16.dp))
                    }
                }
            }
        }
    }
}

private fun deviceIcon(t: String): androidx.compose.ui.graphics.vector.ImageVector = when (t) {
    "camera" -> Icons.Default.Videocam; "wearable" -> Icons.Default.Watch
    "bed_sensor" -> Icons.Default.Bed; "smoke_sensor" -> Icons.Default.Warning
    else -> Icons.Default.DevicesOther
}

private fun deviceTypeLabel(t: String) = when (t) {
    "camera" -> "摄像头"; "wearable" -> "穿戴设备"; "bed_sensor" -> "床垫传感器"
    "smoke_sensor" -> "烟感"; "gas_sensor" -> "燃气"; else -> t
}

private fun permLabel(p: String) = when (p) {
    "monitoring" -> "监控"; "health_records" -> "健康"; "alarm_video" -> "告警视频"; "all" -> "全部数据"; else -> p
}

private fun authStatusLabel(s: String) = when (s) {
    "pending" -> "待审批"; "active" -> "已生效"; "revoked" -> "已撤销"; "expired" -> "已过期"; "rejected" -> "已拒绝"; else -> s
}

@Composable
private fun EmptyPlaceholder(title: String, subtitle: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📭", fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text(title, color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
    }
}
