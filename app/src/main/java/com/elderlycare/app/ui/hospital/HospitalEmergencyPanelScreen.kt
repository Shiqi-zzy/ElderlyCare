package com.elderlycare.app.ui.hospital

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.model.AppUser
import com.elderlycare.app.ui.components.StatusBadge
import com.elderlycare.app.ui.components.charts.ChartLegend
import com.elderlycare.app.ui.components.charts.DonutChart
import com.elderlycare.app.ui.components.charts.MetricRing
import com.elderlycare.app.ui.components.charts.PieSlice
import com.elderlycare.app.ui.shared.HealthCategory
import com.elderlycare.app.ui.shared.color
import com.elderlycare.app.ui.shared.formatTimestamp
import com.elderlycare.app.ui.shared.hasDevice
import com.elderlycare.app.ui.shared.healthCategory
import com.elderlycare.app.ui.theme.*
import kotlinx.coroutines.flow.flowOf

/**
 * 医院「急救大屏」（第四阶段真实数据）。
 * 数据来源：bindingRepository.observeAccessibleElderly(当前医院工作人员) —— 仅本人 ACTIVE 绑定的患者；
 * 风险/关注按档案现有评估字段保守推导（healthCategory），不读取全局告警，不伪造医学风险算法。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HospitalEmergencyPanelScreen(onLogout: () -> Unit = {}, onUserClick: (String) -> Unit = {}) {
    val pulseAlpha by rememberInfiniteTransition(label = "pulse")
        .animateFloat(0.3f, 1f, infiniteRepeatable(animation = tween(800), repeatMode = RepeatMode.Reverse))

    var staff by remember { mutableStateOf<AppUser?>(null) }
    LaunchedEffect(Unit) { staff = ServiceLocator.staffUserStore.getCurrentStaffUser() }

    val elderly by remember(staff?.phone) {
        if (staff != null) ServiceLocator.bindingRepository.observeAccessibleElderly(staff!!)
        else flowOf(emptyList())
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    val total = elderly.size
    val normal = elderly.count { it.profile.healthCategory() == HealthCategory.NORMAL }
    val attention = elderly.count { it.profile.healthCategory() == HealthCategory.ATTENTION }
    val abnormal = elderly.count { it.profile.healthCategory() == HealthCategory.ABNORMAL }
    val focus = elderly.filter { it.profile.healthCategory() != HealthCategory.NORMAL }

    Scaffold(
        topBar = { TopAppBar(title = { Text("急救大屏", fontWeight = FontWeight.SemiBold) }, actions = { IconButton(onClick = onLogout) { Icon(Icons.Filled.Logout, "退出登录") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)) }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // 重点状态条（异常人数 = 按档案评估字段推导）
            Surface(shape = RoundedCornerShape(12.dp), color = Error.copy(alpha = pulseAlpha), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("当前重点: ${abnormal}人", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = OnError)
                    Spacer(modifier = Modifier.weight(1f))
                    Text("立即响应", color = OnError, style = MaterialTheme.typography.labelLarge)
                }
            }

            if (elderly.isEmpty()) {
                Surface(shape = RoundedCornerShape(16.dp), color = StatusYellow.copy(alpha = 0.08f), modifier = Modifier.fillMaxWidth()) {
                    Text("暂无可访问患者，可先在「资质管理」中发起绑定申请", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }

            // 患者卡片（本人 ACTIVE 绑定的老人，点击进入详情）
            elderly.forEach { item ->
                EmergencyPatientCard(
                    name = item.profile.name,
                    info = "${item.profile.age}岁 · ${item.profile.gender.label} · ${item.orgName}",
                    badgeText = item.profile.healthCategory().label,
                    badgeColor = item.profile.healthCategory().color(),
                    deviceText = "设备：${if (item.profile.hasDevice()) "已绑定" else "未绑定"} · 绑定时间 ${formatTimestamp(item.bindingCreatedAt)}",
                    onClick = { onUserClick(item.elderlyId) }
                )
            }

            // 健康状态分布 + 服务老人数
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface), modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("健康状态分布", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))
                        val slices = listOf(
                            PieSlice("正常", normal.toFloat(), StatusGreen),
                            PieSlice("关注", attention.toFloat(), StatusYellow),
                            PieSlice("异常", abnormal.toFloat(), StatusRed)
                        )
                        DonutChart(slices = slices, modifier = Modifier.size(100.dp), centerLabel = total.toString())
                        Spacer(Modifier.height(8.dp))
                        ChartLegend(slices = slices)
                    }
                }
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface), modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("服务患者", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))
                        MetricRing("绑定患者", total.toString(), if (total > 0) 1f else 0f, Primary)
                        Spacer(Modifier.height(4.dp))
                        Text("仅本人获授权患者", style = MaterialTheme.typography.labelSmall, color = TextHint)
                    }
                }
            }

            Text("重点关注老人", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
                if (focus.isEmpty()) {
                    Text("暂无重点关注老人", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium, color = TextHint)
                } else {
                    Column(modifier = Modifier.padding(14.dp)) {
                        focus.forEach { item ->
                            EmergencyRecord(
                                name = item.profile.name,
                                meta = "${item.profile.age}岁 · ${item.profile.gender.label} · ${item.orgName}",
                                badgeText = item.profile.healthCategory().label,
                                badgeColor = item.profile.healthCategory().color()
                            ) { onUserClick(item.elderlyId) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmergencyPatientCard(name: String, info: String, badgeText: String, badgeColor: Color, deviceText: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                StatusBadge(text = badgeText, color = badgeColor)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(info, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(deviceText, style = MaterialTheme.typography.labelSmall, color = TextHint)
            Spacer(modifier = Modifier.height(10.dp))
            Button(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.buttonColors(containerColor = Error)) { Text("查看详情") }
        }
    }
}

@Composable
private fun EmergencyRecord(name: String, meta: String, badgeText: String, badgeColor: Color, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), color = Surface) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Medium)
                Text(meta, style = MaterialTheme.typography.labelSmall, color = TextHint)
            }
            StatusBadge(text = badgeText, color = badgeColor)
        }
    }
}
