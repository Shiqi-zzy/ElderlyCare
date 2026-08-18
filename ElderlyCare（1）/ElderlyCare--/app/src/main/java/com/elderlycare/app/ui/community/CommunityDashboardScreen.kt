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
import com.elderlycare.app.ui.components.StatusBadge
import com.elderlycare.app.ui.components.charts.BarChart
import com.elderlycare.app.ui.components.charts.MetricRing
import com.elderlycare.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityDashboardScreen(onLogout: () -> Unit = {}, onUserClick: (String) -> Unit = {}) {
    var detailTitle by remember { mutableStateOf<String?>(null) }

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
            // 关键指标（四个环形图，两排两个）
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricRing("辖区用户", "47", 47f / 50f, Primary, Modifier.weight(1f)) { detailTitle = "辖区用户" }
                MetricRing("在线设备", "42", 42f / 47f, StatusGreen, Modifier.weight(1f)) { detailTitle = "在线设备" }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricRing("待处理告警", "3", 3f / 10f, StatusRed, Modifier.weight(1f)) { detailTitle = "待处理告警" }
                MetricRing("设备故障", "2", 2f / 10f, StatusYellow, Modifier.weight(1f)) { detailTitle = "设备故障" }
            }

            Text("今日概览", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)

            // 待处理告警（可点进用户）
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("待处理告警", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    OverviewRow("张**", "3号楼 · 15:30", "RISK", StatusRed) { onUserClick("张**") }
                    HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
                    OverviewRow("李**", "5号楼 · 12:10", "ATTENTION", StatusYellow) { onUserClick("李**") }
                }
            }

            // 今日巡访计划（柱状图 · 可点详情）
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Surface)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .clickable { detailTitle = "今日巡访计划" }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("今日巡访计划", fontWeight = FontWeight.SemiBold)
                        Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = TextHint)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    val patrolEntries = listOf("1号楼" to 2f, "2号楼" to 1f, "3号楼" to 3f, "4号楼" to 1f, "5号楼" to 2f)
                    BarChart(entries = patrolEntries, modifier = Modifier.fillMaxWidth().height(150.dp))
                }
            }
        }
    }

    detailTitle?.let { title ->
        MetricDetailDialog(title) { detailTitle = null }
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
private fun MetricDetailDialog(title: String, onDismiss: () -> Unit) {
    val lines = when (title) {
        "辖区用户" -> listOf("3号楼 12 户", "5号楼 10 户", "1号楼 9 户", "2号楼 8 户", "4号楼 8 户")
        "在线设备" -> listOf("RK3 摄像头 40 台在线", "离线 5 台（3号楼 2 台、1号楼 3 台）")
        "待处理告警" -> listOf("跌倒告警 2 条", "设备离线 1 条")
        "设备故障" -> listOf("摄像头离线 2 台", "待检修 1 台")
        "今日巡访计划" -> listOf("3号楼 张** 14:00", "1号楼 王** 16:00", "5号楼 李** 15:30", "2号楼 赵** 09:30")
        else -> emptyList()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                lines.forEach { line ->
                    Text("· $line", style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.padding(vertical = 3.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}
