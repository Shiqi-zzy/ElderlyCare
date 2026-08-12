package com.ezvizpro.ui.community

import androidx.compose.foundation.clickable
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
import com.ezvizpro.data.remote.dto.WorkOrderDto
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityMainScreen(
    token: String,
    onLogout: () -> Unit,
    onBack: () -> Unit = {},
    viewModel: CommunityViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(token) { viewModel.setToken(token) }
    LaunchedEffect(state.toastMessage) {
        state.toastMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearToast() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("🏘 社区端", color = MaterialTheme.colorScheme.onBackground) },
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    StatItem("📋", "待处理工单", "${state.pendingWorkOrders}", WarningColor)
                    StatItem("👴", "绑定老人", "${state.elderlyList.size}", MaterialTheme.colorScheme.primary)
                    StatItem("📹", "已绑设备", "${state.boundDevices.size}", MaterialTheme.colorScheme.primary)
                    StatItem("🔖", "授权申请", "${state.authRequests.size}", MaterialTheme.colorScheme.primary)
                }
            }
            // ── Tab 栏 ──
            TabRow(selectedTabIndex = selectedTab, containerColor = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.primary) {
                Tab(selectedTab == 0, onClick = { selectedTab = 0 }) { Text("🔔 工单处置", Modifier.padding(10.dp)) }
                Tab(selectedTab == 1, onClick = { selectedTab = 1 }) { Text("👴 老人台账", Modifier.padding(10.dp)) }
                Tab(selectedTab == 2, onClick = { selectedTab = 2 }) { Text("📹 设备管理", Modifier.padding(10.dp)) }
                Tab(selectedTab == 3, onClick = { selectedTab = 3; viewModel.loadAuthRequests() }) { Text("🔖 申请授权", Modifier.padding(10.dp)) }
            }
            when (selectedTab) {
                0 -> WorkOrderTab(state, viewModel)
                1 -> ElderlyLedgerTab(state)
                2 -> DeviceManagementTab(state, viewModel)
                3 -> AuthorizationRequestTab(state, viewModel)
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

// ═══════════════════ 工单处置 Tab ═══════════════════
@Composable
private fun WorkOrderTab(state: CommunityUiState, viewModel: CommunityViewModel) {
    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
        return
    }
    if (state.workOrders.isEmpty()) {
        EmptyPlaceholder("暂无工单", "社区管辖范围内暂无待处理工单")
        return
    }
    LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(state.workOrders) { order ->
            WorkOrderCard(order) { action, result ->
                when (action) {
                    "accept" -> viewModel.acceptOrder(order.id)
                    "complete" -> viewModel.completeOrder(order.id, result ?: "处理完成", null)
                }
            }
        }
    }
}

@Composable
private fun WorkOrderCard(order: WorkOrderDto, onAction: (String, String?) -> Unit) {
    var showCompleteDialog by remember { mutableStateOf(false) }
    var completeNote by remember { mutableStateOf("") }

    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(6.dp), color = orderPriorityColor(order.priority).copy(alpha = 0.2f)) {
                    Text(orderPriorityLabel(order.priority), Modifier.padding(horizontal = 8.dp, vertical = 3.dp), color = orderPriorityColor(order.priority), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Surface(shape = RoundedCornerShape(6.dp), color = orderStatusColor(order.status).copy(alpha = 0.2f)) {
                    Text(orderStatusLabel(order.status), Modifier.padding(horizontal = 8.dp, vertical = 3.dp), color = orderStatusColor(order.status), fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("📌 ${order.title}", color = MaterialTheme.colorScheme.onBackground, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(order.description.ifEmpty { "无详细描述" }, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${orderTypeLabel(order.orderType)} · ${order.createdAt.take(16).replace("T", " ")}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp
                )
                Row {
                    if (order.status == "pending") {
                        TextButton(onClick = { onAction("accept", null) }) { Text("接单", fontSize = 13.sp, color = SuccessColor) }
                    }
                    if (order.status == "accepted" || order.status == "in_progress") {
                        TextButton(onClick = { showCompleteDialog = true }) { Text("完成", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary) }
                    }
                }
            }
        }
    }
    if (showCompleteDialog) {
        AlertDialog(
            onDismissRequest = { showCompleteDialog = false },
            title = { Text("完成工单") },
            text = { OutlinedTextField(completeNote, { completeNote = it }, label = { Text("处理备注") }) },
            confirmButton = {
                Button(onClick = { onAction("complete", completeNote); showCompleteDialog = false }) { Text("确认完成") }
            },
            dismissButton = { TextButton(onClick = { showCompleteDialog = false }) { Text("取消") } }
        )
    }
}

// ═══════════════════ 老人台账 Tab ═══════════════════
@Composable
private fun ElderlyLedgerTab(state: CommunityUiState) {
    if (state.elderlyList.isEmpty()) {
        EmptyPlaceholder("无绑定老人", "暂无经授权可访问的老人档案")
        return
    }
    LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(state.elderlyList) { elderly ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        Modifier.size(44.dp), shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) { Text("👴", fontSize = 22.sp) }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(elderly.name, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                        Text(
                            "性别: ${elderly.gender.ifEmpty { "—" }} · 年龄: ${elderly.birthDate.take(4).let { if (it.isNotEmpty()) "${2026 - it.toIntOrNull()!!}岁" else "—" }}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp
                        )
                        Text(
                            "📞 ${elderly.phone} · 📍 ${elderly.address}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(shape = RoundedCornerShape(6.dp), color = careLevelColor(elderly.careLevel).copy(alpha = 0.15f)) {
                        Text(elderly.careLevel, Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = careLevelColor(elderly.careLevel), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// ═══════════════════ 设备管理 Tab ═══════════════════
@Composable
private fun DeviceManagementTab(state: CommunityUiState, viewModel: CommunityViewModel) {
    var codeInput by remember { mutableStateOf("") }
    var showInspection by remember { mutableStateOf(false) }
    var selectedDeviceId by remember { mutableStateOf("") }
    var showHistory by remember { mutableStateOf(false) }

    LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // 验证码绑定卡片
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
                                cursorColor = AccentColor,
                                focusedBorderColor = AccentColor,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { viewModel.bindDeviceByCode(codeInput); codeInput = "" },
                            enabled = codeInput.length == 6,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessColor)
                        ) { Text("绑定") }
                    }
                }
            }
        }

        // 按老人查看设备
        if (state.elderlyList.isNotEmpty()) {
            item { Text("已授权老人的设备", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Medium) }

            state.elderlyList.forEach { elderly ->
                item {
                    Surface(
                        onClick = { viewModel.loadBoundDevices(elderly.id) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (state.selectedDeviceElderlyId == elderly.id) AccentColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
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
                            Column(Modifier.padding(12.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(device.deviceName, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                        Text(
                                            "${deviceTypeLabel(device.deviceType)} · ${device.location.ifEmpty { "—" }} · ${if (device.status == "online") "在线" else "离线"}",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp
                                        )
                                    }
                                    Row {
                                        OutlinedButton(
                                            onClick = { selectedDeviceId = device.id; showInspection = true },
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) { Text("巡检", fontSize = 12.sp) }
                                        Spacer(Modifier.width(6.dp))
                                        OutlinedButton(
                                            onClick = { selectedDeviceId = device.id; showHistory = true; viewModel.loadMaintenanceHistory(device.id) },
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) { Text("历史", fontSize = 12.sp) }
                                    }
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

    // ── 巡检表单弹窗 ──
    if (showInspection && selectedDeviceId.isNotBlank()) {
        var inspType by remember { mutableStateOf("routine") }
        var inspStatus by remember { mutableStateOf("normal") }
        var inspFindings by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showInspection = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("📝 设备巡检记录", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("巡检类型", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("routine" to "日常", "repair" to "维修", "replace" to "更换", "emergency" to "应急").forEach { (k, v) ->
                            FilterChip(selected = inspType == k, onClick = { inspType = k }, label = { Text(v, fontSize = 12.sp) })
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("设备状态", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("normal" to "正常", "needs_repair" to "需维修", "replaced" to "已更换", "fault" to "故障").forEach { (k, v) ->
                            FilterChip(selected = inspStatus == k, onClick = { inspStatus = k }, label = { Text(v, fontSize = 12.sp) })
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        inspFindings, { inspFindings = it },
                        label = { Text("巡检发现") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.logInspection(selectedDeviceId, inspType, inspStatus, inspFindings)
                    showInspection = false
                }) { Text("提交") }
            },
            dismissButton = { TextButton(onClick = { showInspection = false }) { Text("取消") } }
        )
    }

    // ── 维护历史弹窗 ──
    if (showHistory && selectedDeviceId.isNotBlank()) {
        AlertDialog(
            onDismissRequest = { showHistory = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("📋 维护历史", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold) },
            text = {
                if (state.maintenanceHistory.isEmpty()) {
                    Text("暂无维护记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(state.maintenanceHistory) { record ->
                            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                Column(Modifier.padding(10.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(maintTypeLabel(record.maintenanceType), color = MaterialTheme.colorScheme.onBackground, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                        Surface(shape = RoundedCornerShape(4.dp), color = maintStatusColor(record.status).copy(alpha = 0.15f)) {
                                            Text(
                                                maintStatusLabel(record.status),
                                                Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                color = maintStatusColor(record.status),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                    if (record.findings.isNotBlank()) {
                                        Text(record.findings, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    }
                                    Text("${record.inspectionDate.take(16)} · ${record.inspectorName}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showHistory = false }) { Text("关闭") } }
        )
    }
}

// ═══════════════════ 授权申请 Tab ═══════════════════
@Composable
private fun AuthorizationRequestTab(state: CommunityUiState, viewModel: CommunityViewModel) {
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
                colors = ButtonDefaults.buttonColors(containerColor = AccentColor)
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
                    state.elderlyList.takeIf { it.isNotEmpty() }?.forEach { elderly ->
                        Row(Modifier.fillMaxWidth().clickable { requestElderlyId = elderly.id }.padding(vertical = 4.dp)) {
                            RadioButton(
                                selected = requestElderlyId == elderly.id,
                                onClick = { requestElderlyId = elderly.id }
                            )
                            Text("  ${elderly.name} (${elderly.careLevel})", color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(start = 4.dp))
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

// ──── 复用 ────
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

private fun orderStatusLabel(s: String) = when (s) {
    "pending" -> "待接单"; "accepted" -> "已接单"; "in_progress" -> "处理中"; "completed" -> "已完成"; "archived" -> "已归档"; else -> s
}
private fun orderStatusColor(s: String) = when (s) {
    "pending" -> WarningColor; "accepted" -> AccentColor; "in_progress" -> WarningColor; "completed" -> SuccessColor; "archived" -> SubtleText; else -> SubtleText
}
private fun orderPriorityLabel(p: String) = when (p) {
    "low" -> "低"; "normal" -> "普通"; "high" -> "高"; "urgent" -> "紧急"; else -> p
}
private fun orderPriorityColor(p: String) = when (p) {
    "low" -> SubtleText; "normal" -> AccentColor; "high" -> WarningColor; "urgent" -> ErrorColor; else -> SubtleText
}
private fun orderTypeLabel(t: String) = when (t) {
    "alarm_handling" -> "告警处置"; "device_maintenance" -> "设备维护"; "inspection" -> "巡检"; "emergency" -> "急救"; "binding_review" -> "绑定审核"; else -> t
}
private fun permLabel(p: String) = when (p) {
    "monitoring" -> "监控"; "health_records" -> "健康"; "alarm_video" -> "告警视频"; "all" -> "全部数据"; else -> p
}
private fun authStatusLabel(s: String) = when (s) {
    "pending" -> "待审批"; "active" -> "已生效"; "revoked" -> "已撤销"; "expired" -> "已过期"; "rejected" -> "已拒绝"; else -> s
}
private fun careLevelColor(l: String) = when (l) {
    "自理" -> SuccessColor; "半失能" -> WarningColor; "失能" -> ErrorColor; else -> SubtleText
}

private fun deviceTypeLabel(t: String) = when (t) {
    "camera" -> "摄像头"; "wearable" -> "穿戴设备"; "bed_sensor" -> "床垫传感器"
    "smoke_sensor" -> "烟感"; "gas_sensor" -> "燃气"; else -> t
}

private fun maintTypeLabel(t: String) = when (t) {
    "routine" -> "日常巡检"; "repair" -> "维修"; "replace" -> "更换"; "emergency" -> "应急维修"; else -> t
}

private fun maintStatusLabel(s: String) = when (s) {
    "normal" -> "正常"; "needs_repair" -> "需维修"; "replaced" -> "已更换"; "fault" -> "故障"; else -> s
}

private fun maintStatusColor(s: String) = when (s) {
    "normal" -> SuccessColor; "needs_repair" -> WarningColor; "replaced" -> AccentColor; "fault" -> ErrorColor; else -> SubtleText
}
