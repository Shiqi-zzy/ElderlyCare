package com.elderlycare.app.ui.shared

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elderlycare.app.data.binding.BindingRepository.BindingUi
import com.elderlycare.app.data.binding.BindingRepository.RequestUi
import com.elderlycare.app.data.binding.BindingStatus
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.ui.components.StatusBadge
import com.elderlycare.app.ui.theme.*
import kotlinx.coroutines.launch

/** 申请状态 → 徽章颜色（社区/医院资质管理共用） */
private fun statusColor(statusName: String): Color = when (statusName) {
    BindingStatus.PENDING.name -> StatusYellow
    BindingStatus.APPROVED.name -> StatusGreen
    BindingStatus.REJECTED.name -> StatusRed
    else -> TextSecondary
}

/**
 * 工作人员「我的申请」：全部状态（待审核 / 已同意 / 已拒绝 / 已取消）。
 * 挂在社区/医院资质管理页的滚动列表内。
 */
@Composable
fun MyRequestsPanel(staffPhone: String) {
    var requests by remember { mutableStateOf<List<RequestUi>>(emptyList()) }
    LaunchedEffect(staffPhone) {
        requests = ServiceLocator.bindingRepository.getSentRequests(staffPhone)
    }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("我的申请", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            if (requests.isEmpty()) {
                Text("暂无申请记录", style = MaterialTheme.typography.bodyMedium, color = TextHint)
            } else {
                requests.forEachIndexed { i, req ->
                    if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(req.elderlyName, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.width(8.dp))
                                StatusBadge(text = runCatching { BindingStatus.valueOf(req.status).label }.getOrElse { req.status }, color = statusColor(req.status))
                            }
                            Text("申请时间：${formatTimestamp(req.createdAt)}", style = MaterialTheme.typography.labelSmall, color = TextHint)
                            if (req.status != BindingStatus.PENDING.name) {
                                Text("审核时间：${formatTimestamp(req.reviewedAt)}", style = MaterialTheme.typography.labelSmall, color = TextHint)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 工作人员「已绑定用户」：本账号 ACTIVE 绑定关系，支持解除绑定（只改关系状态，不删老人档案）。
 */
@Composable
fun BoundUsersPanel(staffPhone: String) {
    val scope = rememberCoroutineScope()
    var bindings by remember { mutableStateOf<List<BindingUi>>(emptyList()) }
    suspend fun load() {
        bindings = ServiceLocator.bindingRepository.getBindingsForStaff(staffPhone)
    }
    LaunchedEffect(staffPhone) { load() }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("已绑定用户", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            if (bindings.isEmpty()) {
                Text("暂未绑定老人", style = MaterialTheme.typography.bodyMedium, color = TextHint)
            } else {
                bindings.forEachIndexed { i, b ->
                    if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(b.elderlyName, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                            Text("${b.orgName} · ${b.orgTypeLabel} · 绑定时间 ${formatTimestamp(b.createdAt)}", style = MaterialTheme.typography.labelSmall, color = TextHint)
                        }
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    ServiceLocator.bindingRepository.revoke(b.id, staffPhone)
                                    load()
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Error),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) { Text("解除绑定") }
                    }
                }
            }
        }
    }
}
