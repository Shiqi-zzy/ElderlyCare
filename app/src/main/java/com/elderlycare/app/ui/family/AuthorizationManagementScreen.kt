package com.elderlycare.app.ui.family

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elderlycare.app.data.binding.BindingLifecycle
import com.elderlycare.app.data.binding.BindingRepository.BindingUi
import com.elderlycare.app.data.binding.BindingRepository.RequestUi
import com.elderlycare.app.data.binding.BindingStatus
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.ui.components.StatusBadge
import com.elderlycare.app.ui.shared.formatTimestamp
import com.elderlycare.app.ui.theme.*
import kotlinx.coroutines.launch

/**
 * 家属端「授权管理」：
 * 1. 待处理申请（PENDING）—— 社区/医院发起的绑定申请，家属可同意/拒绝；
 * 2. 当前授权（ACTIVE）—— 已生效的绑定，可解除；
 * 3. 授权历史（REVOKED）—— 已解除的绑定历史，只读。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthorizationManagementScreen(onNavigateBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var familyUserId by remember { mutableStateOf<String?>(null) }
    var requests by remember { mutableStateOf<List<RequestUi>>(emptyList()) }
    var bindings by remember { mutableStateOf<List<BindingUi>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var pendingAction by remember { mutableStateOf<String?>(null) } // 正在处理的申请 id
    var pendingRevoke by remember { mutableStateOf<BindingUi?>(null) }

    suspend fun load() {
        val uid = familyUserId ?: return
        requests = ServiceLocator.bindingRepository.getIncomingRequests(uid)
        bindings = ServiceLocator.bindingRepository.getBindingsForFamily(uid)
        loading = false
    }

    LaunchedEffect(Unit) {
        familyUserId = ServiceLocator.userStore.getCurrentUserId()
        load()
    }

    val pendingRequests = requests.filter { it.status == BindingStatus.PENDING.name }
    val active = bindings.filter { it.status == BindingLifecycle.ACTIVE.name }
    val revoked = bindings.filter { it.status == BindingLifecycle.REVOKED.name }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("授权管理", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ===== 待处理申请 =====
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("待处理申请", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    if (pendingRequests.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Error),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${pendingRequests.size}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            if (!loading && pendingRequests.isEmpty()) {
                item { Text("暂无待处理申请", style = MaterialTheme.typography.bodyMedium, color = TextHint) }
            }
            items(pendingRequests, key = { it.id }) { req ->
                PendingRequestCard(
                    request = req,
                    isProcessing = pendingAction == req.id,
                    onApprove = {
                        val uid = familyUserId ?: return@PendingRequestCard
                        pendingAction = req.id
                        scope.launch {
                            ServiceLocator.bindingRepository.approve(req.id, uid)
                            load()
                            pendingAction = null
                        }
                    },
                    onReject = {
                        val uid = familyUserId ?: return@PendingRequestCard
                        pendingAction = req.id
                        scope.launch {
                            ServiceLocator.bindingRepository.reject(req.id, uid)
                            load()
                            pendingAction = null
                        }
                    }
                )
            }

            // ===== 当前授权 =====
            item { Spacer(modifier = Modifier.height(8.dp)) }
            item { Text("当前授权", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium) }
            if (!loading && active.isEmpty()) {
                item { Text("暂无授权", style = MaterialTheme.typography.bodyMedium, color = TextHint) }
            }
            items(active, key = { it.id }) { b ->
                ActiveBindingCard(b) { pendingRevoke = b }
            }

            // ===== 授权历史 =====
            item { Spacer(modifier = Modifier.height(8.dp)) }
            item { Text("授权历史", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium, color = TextHint) }
            if (!loading && revoked.isEmpty()) {
                item { Text("暂无历史记录", style = MaterialTheme.typography.bodyMedium, color = TextHint) }
            }
            items(revoked, key = { it.id }) { b ->
                RevokedBindingCard(b)
            }
        }
    }

    // 解除授权确认弹窗
    val revokeTarget = pendingRevoke
    if (revokeTarget != null) {
        AlertDialog(
            onDismissRequest = { pendingRevoke = null },
            title = { Text("解除授权") },
            text = { Text("确定解除「${revokeTarget.orgName}」对 ${revokeTarget.elderlyName} 的授权吗？\n老人档案与设备数据不会被删除。") },
            confirmButton = {
                TextButton(onClick = {
                    val target = revokeTarget
                    pendingRevoke = null
                    scope.launch {
                        val uid = familyUserId ?: return@launch
                        ServiceLocator.bindingRepository.revoke(target.id, uid)
                        load()
                    }
                }) { Text("解除授权", color = Error) }
            },
            dismissButton = { TextButton(onClick = { pendingRevoke = null }) { Text("取消") } }
        )
    }
}

/** 待处理申请卡片：申请方信息 + 老人 + 申请说明 + 同意/拒绝按钮 */
@Composable
private fun PendingRequestCard(
    request: RequestUi,
    isProcessing: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 机构图标
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Business, null, tint = Primary, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(request.orgName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.width(6.dp))
                        StatusBadge(text = request.orgTypeLabel, color = Primary)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("申请人：${request.requesterName} · ${request.requesterRole}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                StatusBadge(text = BindingStatus.PENDING.label, color = StatusYellow)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text("申请绑定老人：${request.elderlyName}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            if (request.message.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("申请说明：${request.message}", style = MaterialTheme.typography.bodySmall, color = TextHint)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("申请时间：${formatTimestamp(request.createdAt)}", style = MaterialTheme.typography.labelSmall, color = TextHint)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    enabled = !isProcessing,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Error),
                    modifier = Modifier.weight(1f).height(42.dp)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Error, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.Close, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("拒绝")
                    }
                }
                Button(
                    onClick = onApprove,
                    enabled = !isProcessing,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusGreen, contentColor = Color.White),
                    modifier = Modifier.weight(1f).height(42.dp)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.Check, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("同意")
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveBindingCard(b: BindingUi, onRevoke: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(b.orgName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                StatusBadge(text = BindingLifecycle.ACTIVE.label, color = StatusGreen)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text("机构类型：${b.orgTypeLabel} · ${b.userRoleLabel}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Text("老人：${b.elderlyName}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Text("授权状态：生效 · 权限：查看", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Text("绑定时间：${formatTimestamp(b.createdAt)}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onRevoke,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Error),
                modifier = Modifier.align(Alignment.End)
            ) { Text("解除授权") }
        }
    }
}

@Composable
private fun RevokedBindingCard(b: BindingUi) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceVariant)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(b.orgName, style = MaterialTheme.typography.bodyMedium)
                Text("老人：${b.elderlyName} · 绑定时间：${formatTimestamp(b.createdAt)}", style = MaterialTheme.typography.labelSmall, color = TextHint)
            }
            StatusBadge(text = BindingLifecycle.REVOKED.label, color = TextSecondary)
        }
    }
}
