package com.elderlycare.app.ui.community

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elderlycare.app.data.binding.BindingRepository.AccessibleElderlyUi
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.model.AppUser
import com.elderlycare.app.ui.components.StatusBadge
import com.elderlycare.app.ui.components.charts.BarChart
import com.elderlycare.app.ui.components.charts.MetricRing
import com.elderlycare.app.ui.shared.HealthCategory
import com.elderlycare.app.ui.shared.color
import com.elderlycare.app.ui.shared.hasDevice
import com.elderlycare.app.ui.shared.healthCategory
import com.elderlycare.app.ui.theme.*
import kotlinx.coroutines.flow.flowOf

/**
 * 社区「辖区看板」（第四阶段真实数据）。
 * 数据来源：bindingRepository.observeAccessibleElderly(当前工作人员) —— 仅本人 ACTIVE 绑定的老人。
 * 统计由档案现有字段保守推导（healthCategory/hasDevice），不读全局设备/告警。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityDashboardScreen(onLogout: () -> Unit = {}, onUserClick: (String) -> Unit = {}) {
    var staff by remember { mutableStateOf<AppUser?>(null) }
    var detailTitle by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { staff = ServiceLocator.staffUserStore.getCurrentStaffUser() }

    val elderly by remember(staff?.phone) {
        if (staff != null) ServiceLocator.bindingRepository.observeAccessibleElderly(staff!!)
        else flowOf(emptyList())
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    val total = elderly.size
    val base = maxOf(1, total)
    val normal = elderly.count { it.profile.healthCategory() == HealthCategory.NORMAL }
    val attention = elderly.count { it.profile.healthCategory() == HealthCategory.ATTENTION }
    val abnormal = elderly.count { it.profile.healthCategory() == HealthCategory.ABNORMAL }
    val withDevice = elderly.count { it.profile.hasDevice() }

    fun categoryList(title: String): List<AccessibleElderlyUi> = when (title) {
        "服务老人" -> elderly
        "有设备" -> elderly.filter { it.profile.hasDevice() }
        "正常" -> elderly.filter { it.profile.healthCategory() == HealthCategory.NORMAL }
        "关注" -> elderly.filter { it.profile.healthCategory() == HealthCategory.ATTENTION }
        "异常" -> elderly.filter { it.profile.healthCategory() == HealthCategory.ABNORMAL }
        else -> emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("辖区看板", fontWeight = FontWeight.SemiBold) },
                actions = { IconButton(onClick = onLogout) { Icon(Icons.Filled.Logout, "退出登录") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (total == 0) {
                Surface(shape = RoundedCornerShape(16.dp), color = StatusYellow.copy(alpha = 0.08f), modifier = Modifier.fillMaxWidth()) {
                    Text("暂无可访问老人，可先在「资质管理」中发起绑定申请", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }

            // 关键指标（五个环形图：服务老人 / 有设备 / 正常 / 关注 / 异常）
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricRing("服务老人", total.toString(), if (total > 0) 1f else 0f, Primary, Modifier.weight(1f)) { detailTitle = "服务老人" }
                MetricRing("有设备", withDevice.toString(), withDevice.toFloat() / base, StatusGreen, Modifier.weight(1f)) { detailTitle = "有设备" }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricRing("正常", normal.toString(), normal.toFloat() / base, StatusGreen, Modifier.weight(1f)) { detailTitle = "正常" }
                MetricRing("关注", attention.toString(), attention.toFloat() / base, StatusYellow, Modifier.weight(1f)) { detailTitle = "关注" }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricRing("异常", abnormal.toString(), abnormal.toFloat() / base, StatusRed, Modifier.weight(1f)) { detailTitle = "异常" }
            }

            Text("重点关注", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)

            // 重点关注（关注/异常老人，可点进详情）
            val focus = elderly.filter { it.profile.healthCategory() != HealthCategory.NORMAL }
            if (focus.isEmpty()) {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
                    Text("暂无重点关注老人", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium, color = TextHint)
                }
            } else {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        focus.forEachIndexed { index, item ->
                            if (index > 0) HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
                            OverviewRow(
                                name = item.profile.name,
                                meta = "${item.profile.age}岁 · ${item.profile.gender.label} · ${item.orgName}",
                                badgeText = item.profile.healthCategory().label,
                                badgeColor = item.profile.healthCategory().color()
                            ) { onUserClick(item.elderlyId) }
                        }
                    }
                }
            }

            // 健康状态分布（柱状图 · 可点详情）
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Surface)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .clickable { detailTitle = "服务老人" }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("健康状态分布", fontWeight = FontWeight.SemiBold)
                        Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = TextHint)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    val distribution = listOf("正常" to normal.toFloat(), "关注" to attention.toFloat(), "异常" to abnormal.toFloat())
                    BarChart(entries = distribution, modifier = Modifier.fillMaxWidth().height(150.dp))
                }
            }
        }
    }

    detailTitle?.let { title ->
        MetricDetailDialog(title, categoryList(title), onUserClick) { detailTitle = null }
    }
}

@Composable
private fun OverviewRow(name: String, meta: String, badgeText: String, badgeColor: Color, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), color = Surface) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusBadge(text = badgeText, color = badgeColor)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(meta, style = MaterialTheme.typography.labelSmall, color = TextHint)
            }
            Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = TextHint)
        }
    }
}

@Composable
private fun MetricDetailDialog(title: String, items: List<AccessibleElderlyUi>, onUserClick: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        text = {
            if (items.isEmpty()) {
                Text("暂无数据", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            } else {
                Column {
                    items.forEach { item ->
                        Surface(onClick = { onDismiss(); onUserClick(item.elderlyId) }, color = Surface) {
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.profile.name, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyLarge)
                                    Text("${item.profile.age}岁 · ${item.profile.gender.label}", style = MaterialTheme.typography.labelSmall, color = TextHint)
                                }
                                StatusBadge(text = item.profile.healthCategory().label, color = item.profile.healthCategory().color())
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}
