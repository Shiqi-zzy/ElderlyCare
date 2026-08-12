package com.elderlycare.app.ui.community

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elderlycare.app.ui.components.StatusBadge
import com.elderlycare.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityDashboardScreen(onLogout: () -> Unit = {}) {
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
            // 2x2 统计卡片
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("辖区老人", "47人", Primary, Modifier.weight(1f))
                StatCard("在线设备", "42/47", StatusGreen, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("待处理告警", "3条", StatusRed, Modifier.weight(1f))
                StatCard("设备故障", "2台", StatusYellow, Modifier.weight(1f))
            }

            Text("今日概览", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)

            // 待处理跌倒告警
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("待处理跌倒告警", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    AlertRow("张**", "3号楼 15:30", "RISK")
                    AlertRow("李**", "5号楼 12:10", "ATTENTION")
                }
            }

            // 今日巡访计划
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("今日巡访计划", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    PatrolRow("张** (3号楼)", "14:00", "待巡访")
                    PatrolRow("王** (1号楼)", "16:00", "待巡访")
                }
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineMedium, color = color)
            Text(title, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        }
    }
}

@Composable
private fun AlertRow(name: String, loc: String, level: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(name, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.width(8.dp))
        Text(loc, style = MaterialTheme.typography.labelSmall, color = TextHint)
        Spacer(modifier = Modifier.weight(1f))
        StatusBadge(text = level, color = if (level == "RISK") StatusRed else StatusYellow)
    }
}

@Composable
private fun PatrolRow(name: String, time: String, status: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(time, style = MaterialTheme.typography.labelSmall, color = TextHint)
        Spacer(modifier = Modifier.width(8.dp))
        Text(status, color = Primary, style = MaterialTheme.typography.labelMedium)
    }
}
