package com.elderlycare.app.ui.hospital

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import com.elderlycare.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HospitalEmergencyPanelScreen(onLogout: () -> Unit = {}) {
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
            EmergencyPatientCard("张**", "72岁 · 男", "跌倒呼救", "15:30 · 3号楼", "派遣急救")
            EmergencyPatientCard("王**", "75岁 · 男", "SOS求救", "09:15 · 1号楼", "派遣急救")

            Text("今日急救记录", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    EmergencyRecord("李**", "跌倒", "10:30", "已处理")
                    EmergencyRecord("赵**", "胸闷", "08:15", "已处理")
                }
            }
        }
    }
}

@Composable
private fun EmergencyPatientCard(name: String, info: String, type: String, loc: String, action: String) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
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
private fun EmergencyRecord(name: String, type: String, time: String, status: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(name, modifier = Modifier.weight(1f)); Text(type, style = MaterialTheme.typography.bodyMedium, color = TextSecondary); Spacer(modifier = Modifier.width(12.dp)); Text(time, style = MaterialTheme.typography.labelSmall, color = TextHint); Spacer(modifier = Modifier.width(8.dp)); Text(status, color = StatusGreen, style = MaterialTheme.typography.labelMedium)
    }
}
