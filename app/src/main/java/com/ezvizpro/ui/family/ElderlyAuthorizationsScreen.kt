package com.ezvizpro.ui.family

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private val DarkBg = Color(0xFF0F172A)
private val CardBg = Color(0xFF1E293B)
private val TextPrimary = Color(0xFFE2E8F0)
private val TextSecondary = Color(0xFF94A3B8)
private val Accent = Color(0xFF3B82F6)
private val Warning = Color(0xFFF59E0B)
private val Success = Color(0xFF22C55E)

/**
 * 授权管理（独立子页面，从家属端 MainScreen → 我的Tab 导航进入）
 *
 * 功能：
 * 1. 顶部展示社区/医院发来的待审批授权申请 → 家属可审批/拒绝
 * 2. 下方展示已有授权列表 → 可撤销
 * 3. 底部可新建主动授权
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElderlyAuthorizationsScreen(
    onBack: () -> Unit,
    viewModel: FamilyViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pendingRequests by viewModel.pendingRequests.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showGrantDialog by remember { mutableStateOf(false) }
    var grantUserId by remember { mutableStateOf("") }
    var grantType by remember { mutableStateOf("monitoring") }
    var grantUntil by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.loadPendingRequests() }
    LaunchedEffect(state.toastMessage) {
        state.toastMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearToast() }
    }
    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        containerColor = DarkBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("🔐 授权管理", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // ════════════════════════════════════════
            // 待审批的授权申请（社区/医院发来的）
            // ════════════════════════════════════════
            if (pendingRequests.isNotEmpty()) {
                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Warning.copy(alpha = 0.1f))
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NotificationsActive, null, tint = Warning, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("待处理授权申请", color = Warning, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Surface(shape = RoundedCornerShape(10.dp), color = Warning) {
                                Text(
                                    " ${pendingRequests.size} ",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
                LazyColumn(
                    Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(pendingRequests) { req ->
                        PendingRequestCard(
                            request = req,
                            onApprove = { viewModel.approveRequest(req.id) },
                            onReject = { viewModel.rejectRequest(req.id) }
                        )
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }

            // ── 老人选择器 ──
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

            // ── 已有授权 + 新建 ──
            Column(Modifier.padding(16.dp)) {
                Button(
                    onClick = { showGrantDialog = true },
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("新建授权")
                }
                Spacer(Modifier.height(12.dp))

                if (state.authorizations.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📭", fontSize = 48.sp)
                            Spacer(Modifier.height(12.dp))
                            Text("暂无授权记录", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                            Text("点击上方按钮为社区/医院人员授权", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.authorizations) { auth ->
                            AuthCard(auth) { viewModel.revokeAuthorization(auth.id) }
                        }
                    }
                }
            }
        }
    }

    // ── 新建授权弹窗 ──
    if (showGrantDialog) {
        AlertDialog(
            onDismissRequest = { showGrantDialog = false },
            title = { Text("新建授权") },
            text = {
                Column {
                    OutlinedTextField(
                        grantUserId, { grantUserId = it },
                        label = { Text("被授权用户UUID") },
                        singleLine = true,
                        isError = grantUserId.isBlank()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("授权类型", color = TextSecondary, fontSize = 13.sp)
                    Row {
                        listOf("monitoring" to "监控", "health_records" to "健康", "all" to "全部").forEach { (k, v) ->
                            FilterChip(
                                selected = grantType == k,
                                onClick = { grantType = k },
                                label = { Text(v) },
                                modifier = Modifier.padding(2.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        grantUntil, { grantUntil = it },
                        label = { Text("有效期至 (YYYY-MM-DD，留空默认30天)") },
                        singleLine = true
                    )
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

// ════════════════════════════════════════
// 待审批申请卡片
// ════════════════════════════════════════
@Composable
private fun PendingRequestCard(
    request: com.ezvizpro.data.remote.dto.AuthorizationDto,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "👴 ${request.elderlyName.ifEmpty { "老人" }}",
                        color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp
                    )
                    Text(
                        "申请人: ${request.granteeName.ifEmpty { "—" }} · ${request.institutionName.ifEmpty { "—" }}",
                        color = TextSecondary, fontSize = 12.sp
                    )
                    Row {
                        Surface(shape = RoundedCornerShape(4.dp), color = Accent.copy(alpha = 0.15f)) {
                            Text(
                                permLabel(request.permissionType),
                                Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = Accent, fontSize = 11.sp
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "申请于 ${request.createdAt.take(16).replace("T", " ")}",
                            color = TextSecondary, fontSize = 11.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(
                    onClick = onReject,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                ) { Text("拒绝", fontSize = 13.sp) }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onApprove,
                    colors = ButtonDefaults.buttonColors(containerColor = Success)
                ) { Text("通过", fontSize = 13.sp) }
            }
        }
    }
}

@Composable
private fun AuthCard(auth: com.ezvizpro.data.remote.dto.AuthorizationDto, onRevoke: () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Row(
            Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "👤 ${auth.granteeName.ifEmpty { "用户" }}",
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Text(auth.granteePhone.ifEmpty { "无电话" }, color = TextSecondary, fontSize = 12.sp)
                Row {
                    Surface(shape = RoundedCornerShape(4.dp), color = Accent.copy(alpha = 0.15f)) {
                        Text(
                            permLabel(auth.permissionType),
                            Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = Accent,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    val stColor = when (auth.status) {
                        "active" -> Color(0xFF22C55E)
                        "revoked" -> Color(0xFFEF4444)
                        else -> TextSecondary
                    }
                    Surface(shape = RoundedCornerShape(4.dp), color = stColor.copy(alpha = 0.15f)) {
                        Text(
                            authStatusLabel(auth.status),
                            Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = stColor,
                            fontSize = 11.sp
                        )
                    }
                }
                Text("至 ${auth.effectiveUntil.take(10)}", color = TextSecondary, fontSize = 11.sp)
            }
            if (auth.status == "active") {
                IconButton(onClick = onRevoke) {
                    Icon(Icons.Default.Cancel, "撤销", tint = Color(0xFFEF4444))
                }
            }
        }
    }
}

private fun permLabel(p: String) = when (p) {
    "monitoring" -> "监控"
    "health_records" -> "健康"
    "alarm_video" -> "告警视频"
    "all" -> "全部数据"
    else -> p
}

private fun authStatusLabel(s: String) = when (s) {
    "active" -> "有效"
    "revoked" -> "已撤销"
    "expired" -> "已过期"
    "rejected" -> "已拒绝"
    "pending" -> "待审批"
    else -> s
}
