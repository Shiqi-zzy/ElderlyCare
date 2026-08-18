package com.elderlycare.app.ui.community

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun CommunityRosterScreen(onNavigateToDetail: (String) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("用户台账", fontWeight = FontWeight.SemiBold) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface))
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Surface(shape = RoundedCornerShape(0.dp), color = StatusYellow.copy(alpha = 0.08f), modifier = Modifier.fillMaxWidth()) {
                Text("隐私保护模式：姓名、详细地址已脱敏处理", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            }
            LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(mockElderly) { elder ->
                    Card(modifier = Modifier.fillMaxWidth().clickable { onNavigateToDetail(elder.name) }, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) { Text(elder.name, fontWeight = FontWeight.SemiBold); Spacer(modifier = Modifier.width(8.dp)); StatusBadge(text = elder.cameraStatus, color = if (elder.cameraStatus == "在线") StatusGreen else StatusYellow) }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("${elder.address} · ${elder.age}岁 · ${elder.gender}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("上次巡访: ${elder.lastPatrol}", style = MaterialTheme.typography.labelSmall, color = TextHint)
                            }
                            TextButton(onClick = { onNavigateToDetail(elder.name) }) { Text("查看档案", color = Primary) }
                        }
                    }
                }
            }
        }
    }
}

private data class ElderInfo(val name: String, val gender: String, val age: String, val address: String, val cameraStatus: String, val lastPatrol: String)
private val mockElderly = listOf(
    ElderInfo("张**", "男", "72", "3号楼", "在线", "2024-07-09"),
    ElderInfo("李**", "女", "68", "5号楼", "在线", "2024-07-10"),
    ElderInfo("王**", "男", "75", "1号楼", "离线", "2024-07-05"),
    ElderInfo("赵**", "女", "81", "2号楼", "在线", "2024-07-08"),
    ElderInfo("刘**", "男", "70", "4号楼", "在线", "2024-07-07")
)
