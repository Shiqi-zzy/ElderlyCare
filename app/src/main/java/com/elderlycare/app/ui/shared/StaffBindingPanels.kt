package com.elderlycare.app.ui.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elderlycare.app.data.binding.BindingRepository.BindingUi
import com.elderlycare.app.data.binding.BindingRepository.RequestUi
import com.elderlycare.app.data.binding.BindingStatus
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.ui.components.StatusBadge
import com.elderlycare.app.ui.theme.*
import kotlinx.coroutines.launch

/** 绑定申请面板色值（薄荷绿主题，与工作资格页统一） */
private val PageCard = Color.White
private val TextDark = Color(0xFF1A2E25)
private val TextGray = Color(0xFF6B7C74)
private val TextHint = Color(0xFF9AA8A2)
private val MintGreen = Color(0xFF4CAF8A)

/** 申请状态 → 色条颜色 */
private fun statusBarColor(statusName: String): Color = when (statusName) {
    BindingStatus.PENDING.name -> StatusYellow
    BindingStatus.APPROVED.name -> StatusGreen
    BindingStatus.REJECTED.name -> StatusRed
    else -> TextSecondary
}

/** 申请状态 → 图标 */
private fun statusIcon(statusName: String): ImageVector = when (statusName) {
    BindingStatus.PENDING.name -> Icons.Filled.HourglassEmpty
    BindingStatus.APPROVED.name -> Icons.Filled.Verified
    BindingStatus.REJECTED.name -> Icons.Filled.Cancel
    else -> Icons.Filled.Info
}

/**
 * 工作人员「我的申请」：卡片式列表，每个申请独立卡片，左侧状态色条。
 * 全部状态（待审核 / 已同意 / 已拒绝 / 已取消）。
 */
@Composable
fun MyRequestsPanel(staffPhone: String) {
    var requests by remember { mutableStateOf<List<RequestUi>>(emptyList()) }
    LaunchedEffect(staffPhone) {
        requests = ServiceLocator.bindingRepository.getSentRequests(staffPhone)
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // 标题行
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MintGreen.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Description, null, tint = MintGreen, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("我的申请", color = TextDark, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(modifier = Modifier.weight(1f))
            Text("${requests.size}条", color = TextHint, fontSize = 12.sp)
        }

        if (requests.isEmpty()) {
            EmptyStateCard(Icons.Filled.Inbox, "暂无申请记录")
        } else {
            requests.forEach { req ->
                RequestCard(req)
            }
        }
    }
}

/** 单条申请卡片：左侧状态色条 + 老人姓名/状态 + 时间信息 */
@Composable
private fun RequestCard(req: RequestUi) {
    val barColor = statusBarColor(req.status)
    val statusLabel = runCatching { BindingStatus.valueOf(req.status).label }.getOrElse { req.status }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = PageCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            // 左侧状态色条
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(barColor)
            )
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 状态图标
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(barColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(statusIcon(req.status), null, tint = barColor, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(req.elderlyName, color = TextDark, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("申请绑定", color = TextGray, fontSize = 11.sp)
                    }
                    StatusBadge(text = statusLabel, color = barColor)
                }
                Spacer(modifier = Modifier.height(10.dp))
                // 时间信息
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TimeInfo(Icons.Filled.Schedule, "申请", formatTimestamp(req.createdAt))
                    if (req.status != BindingStatus.PENDING.name) {
                        TimeInfo(Icons.Filled.FactCheck, "审核", formatTimestamp(req.reviewedAt))
                    }
                }
            }
        }
    }
}

/** 时间信息行：图标 + 标签 + 时间 */
@Composable
private fun TimeInfo(icon: ImageVector, label: String, time: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = TextHint, modifier = Modifier.size(12.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("$label：", color = TextHint, fontSize = 11.sp)
        Text(time, color = TextGray, fontSize = 11.sp)
    }
}

/**
 * 工作人员「已绑定用户」：卡片式列表，每个用户独立卡片，带头像和解除绑定按钮。
 * 本账号 ACTIVE 绑定关系，支持解除绑定。
 */
@Composable
fun BoundUsersPanel(staffPhone: String, elderWord: String = "服务对象") {
    val scope = rememberCoroutineScope()
    var bindings by remember { mutableStateOf<List<BindingUi>>(emptyList()) }
    var revokingId by remember { mutableStateOf<String?>(null) }

    suspend fun load() {
        bindings = ServiceLocator.bindingRepository.getBindingsForStaff(staffPhone)
    }
    LaunchedEffect(staffPhone) { load() }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // 标题行
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MintGreen.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.People, null, tint = MintGreen, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("已绑定用户", color = TextDark, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(modifier = Modifier.weight(1f))
            Text("${bindings.size}人", color = TextHint, fontSize = 12.sp)
        }

        if (bindings.isEmpty()) {
            EmptyStateCard(Icons.Filled.PersonOff, "暂未绑定$elderWord")
        } else {
            bindings.forEach { b ->
                BoundUserCard(
                    binding = b,
                    isRevoking = revokingId == b.id,
                    onRevoke = {
                        revokingId = b.id
                        scope.launch {
                            ServiceLocator.bindingRepository.revoke(b.id, staffPhone)
                            load()
                            revokingId = null
                        }
                    }
                )
            }
        }
    }
}

/** 单条已绑定用户卡片：头像 + 姓名/机构 + 绑定时间 + 解除绑定 */
@Composable
private fun BoundUserCard(
    binding: BindingUi,
    isRevoking: Boolean,
    onRevoke: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = PageCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 头像
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MintGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        binding.elderlyName.take(1),
                        color = MintGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(binding.elderlyName, color = TextDark, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(StatusGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Check, null, tint = StatusGreen, modifier = Modifier.size(10.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("${binding.orgName} · ${binding.orgTypeLabel}", color = TextGray, fontSize = 11.sp)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            // 绑定时间 + 解除按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CalendarMonth, null, tint = TextHint, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("绑定：", color = TextHint, fontSize = 11.sp)
                    Text(formatTimestamp(binding.createdAt), color = TextGray, fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.weight(1f))
                // 解除绑定按钮
                Button(
                    onClick = onRevoke,
                    enabled = !isRevoking,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Error.copy(alpha = 0.1f),
                        contentColor = Error,
                        disabledContainerColor = Error.copy(alpha = 0.05f)
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    if (isRevoking) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Error, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Icon(Icons.Filled.LinkOff, null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("解除绑定", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

/** 空状态卡片 */
@Composable
private fun EmptyStateCard(icon: ImageVector, text: String) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = PageCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(icon, null, tint = TextHint, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(text, color = TextHint, fontSize = 13.sp)
            }
        }
    }
}
