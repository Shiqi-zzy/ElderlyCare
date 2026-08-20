package com.elderlycare.app.ui.family

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elderlycare.app.data.binding.BindingLifecycle
import com.elderlycare.app.data.binding.BindingRepository.BindingUi
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.ui.components.StatusBadge
import com.elderlycare.app.ui.shared.formatTimestamp
import com.elderlycare.app.ui.theme.*
import kotlinx.coroutines.launch

/**
 * 家属端「授权管理」：真实展示社区/医院对本家庭的授权关系。
 * ACTIVE 为当前授权（可解除），REVOKED 为授权历史（只读）。解除后立即刷新为 REVOKED。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthorizationManagementScreen(onNavigateBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var familyUserId by remember { mutableStateOf<String?>(null) }
    var bindings by remember { mutableStateOf<List<BindingUi>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var pendingRevoke by remember { mutableStateOf<BindingUi?>(null) }

    suspend fun load() {
        val uid = familyUserId ?: return
        bindings = ServiceLocator.bindingRepository.getBindingsForFamily(uid)
        loading = false
    }

    LaunchedEffect(Unit) {
        familyUserId = ServiceLocator.userStore.getCurrentUserId()
        load()
    }

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
            item { Text("当前授权", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium) }
            if (!loading && active.isEmpty()) {
                item { Text("暂无授权", style = MaterialTheme.typography.bodyMedium, color = TextHint) }
            }
            items(active, key = { it.id }) { b ->
                ActiveBindingCard(b) { pendingRevoke = b }
            }
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
