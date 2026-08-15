package com.elderlycare.app.ui.hospital

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elderlycare.app.ui.components.StatusBadge
import com.elderlycare.app.ui.components.charts.BarChart
import com.elderlycare.app.ui.components.charts.ChartLegend
import com.elderlycare.app.ui.components.charts.DonutChart
import com.elderlycare.app.ui.components.charts.MetricRing
import com.elderlycare.app.ui.components.charts.PieSlice
import com.elderlycare.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HospitalEmergencyPanelScreen(onLogout: () -> Unit = {}, onUserClick: (String) -> Unit = {}) {
    val pulseAlpha by rememberInfiniteTransition(label = "pulse").animateFloat(0.3f, 1f, infiniteRepeatable(animation = tween(800), repeatMode = RepeatMode.Reverse))

    Scaffold(
        topBar = { TopAppBar(title = { Text("急救大屏", fontWeight = FontWeight.SemiBold) }, actions = { IconButton(onClick = onLogout) { Icon(Icons.Filled.Logout, "退出登录") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)) }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // 紧急状态条
            Surface(shape = RoundedCornerShape(12.dp), color = Error.copy(alpha = pulseAlpha), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("当前紧急: 2人", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = OnError)
                    Spacer(modifier = Modifier.weight(1f))
                    Text("立即响应", color = OnError, style = MaterialTheme.typography.labelLarge)
                }
            }
            // 急救患者卡片
            EmergencyPatientCard("张**", "72岁 · 男", "跌倒呼救", "15:30 · 3号楼", "派遣急救", onClick = { onUserClick("张**") })
            EmergencyPatientCard("王**", "75岁 · 男", "SOS求救", "09:15 · 1号楼", "派遣急救", onClick = { onUserClick("王**") })

            // 紧急程度分布 + 响应时效
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface), modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("紧急程度分布", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))
                        val severitySlices = listOf(
                            PieSlice("危重", 1f, StatusRed),
                            PieSlice("紧急", 1f, StatusYellow),
                            PieSlice("一般", 3f, StatusGreen)
                        )
                        DonutChart(slices = severitySlices, modifier = Modifier.size(100.dp), centerLabel = "5")
                        Spacer(Modifier.height(8.dp))
                        ChartLegend(slices = severitySlices)
                    }
                }
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface), modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("响应时效", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))
                        MetricRing("平均响应(分)", "3.2", 3.2f / 5f, StatusGreen)
                        Spacer(Modifier.height(4.dp))
                        Text("目标 ≤ 5 分钟", style = MaterialTheme.typography.labelSmall, color = TextHint)
                    }
                }
            }

            // 今日急救时段趋势
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("今日急救时段趋势", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    val hourly = listOf("0-4" to 1f, "4-8" to 2f, "8-12" to 4f, "12-16" to 5f, "16-20" to 3f, "20-24" to 2f)
                    BarChart(entries = hourly, modifier = Modifier.fillMaxWidth().height(150.dp))
                }
            }

            Text("今日急救记录", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    EmergencyRecord("李**", "跌倒", "10:30", "已处理", onClick = { onUserClick("李**") })
                    EmergencyRecord("赵**", "胸闷", "08:15", "已处理", onClick = { onUserClick("赵**") })
                }
            }
        }
    }
}

@Composable
private fun EmergencyPatientCard(name: String, info: String, type: String, loc: String, action: String, onClick: () -> Unit) {
    Card(modifier = Modifier.clickable(onClick = onClick), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium); StatusBadge(text = type, color = StatusRed) }
            Spacer(modifier = Modifier.height(4.dp))
            Text("$info · $loc", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Spacer(modifier = Modifier.height(10.dp))
            Button(onClick = {}, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.buttonColors(containerColor = Error)) { Text(action) }
        }
    }
}

@Composable
private fun EmergencyRecord(name: String, type: String, time: String, status: String, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), color = Surface) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(name, modifier = Modifier.weight(1f)); Text(type, style = MaterialTheme.typography.bodyMedium, color = TextSecondary); Spacer(modifier = Modifier.width(12.dp)); Text(time, style = MaterialTheme.typography.labelSmall, color = TextHint); Spacer(modifier = Modifier.width(8.dp)); Text(status, color = StatusGreen, style = MaterialTheme.typography.labelMedium)
        }
    }
}
