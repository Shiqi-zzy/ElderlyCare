package com.elderlycare.app.ui.hospital

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elderlycare.app.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HospitalEmergencyVideoScreen() {
    var emergencyActive by remember { mutableStateOf(false) }
    var remainingSeconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(emergencyActive) {
        if (emergencyActive) {
            remainingSeconds = 300
            while (remainingSeconds > 0) { delay(1000); remainingSeconds-- }
            emergencyActive = false
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("应急视频", fontWeight = FontWeight.SemiBold) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)) }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = RoundedCornerShape(12.dp), color = Error.copy(alpha = 0.08f), modifier = Modifier.fillMaxWidth()) {
                Text("本功能仅在紧急情况下开放临时视频授权，平时不可查看", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.labelMedium, color = Error)
            }

            if (!emergencyActive) {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("当前无紧急授权", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("无法查看任何视频画面", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { emergencyActive = true },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Error)
                ) { Text("模拟紧急授权 (演示)") }
            } else {
                val min = remainingSeconds / 60
                val sec = remainingSeconds % 60
                Surface(shape = RoundedCornerShape(12.dp), color = Primary.copy(alpha = 0.08f), modifier = Modifier.fillMaxWidth()) {
                    Text("紧急授权中 · 剩余 ${min}:${sec.toString().padStart(2, '0')}", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.labelLarge, color = Primary, fontWeight = FontWeight.SemiBold)
                }
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) { Text("张** (3号楼)", fontWeight = FontWeight.SemiBold); Text("跌倒呼救 · 15:30", style = MaterialTheme.typography.bodyMedium, color = TextSecondary) }
                        Button(onClick = {}, shape = RoundedCornerShape(16.dp)) { Text("查看实时画面") }
                    }
                }
            }
        }
    }
}
