package com.elderlycare.app.ui.community

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
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
fun CommunityAlertPlaybackScreen() {
    Scaffold(
        topBar = { TopAppBar(title = { Text("告警回放", fontWeight = FontWeight.SemiBold) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)) }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Surface(shape = RoundedCornerShape(0.dp), color = Secondary.copy(alpha = 0.08f), modifier = Modifier.fillMaxWidth()) {
                Text("仅可查看告警触发时15秒短视频片段，不提供实时画面预览", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelMedium, color = Secondary)
            }
            LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(mockClips) { clip ->
                    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = RoundedCornerShape(12.dp), color = SurfaceVariant, modifier = Modifier.size(64.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.PlayArrow, null, tint = Primary, modifier = Modifier.size(28.dp)) } }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(clip.alertType, fontWeight = FontWeight.SemiBold)
                                Text("${clip.elderly} · ${clip.time}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                Text("时长: ${clip.duration} · ${clip.date}", style = MaterialTheme.typography.labelSmall, color = TextHint)
                            }
                            if (clip.expired) StatusBadge(text = "已过期", color = TextHint) else TextButton(onClick = {}) { Text("播放") }
                        }
                    }
                }
            }
        }
    }
}

private data class ClipInfo(val alertType: String, val elderly: String, val time: String, val duration: String, val date: String, val expired: Boolean)
private val mockClips = listOf(
    ClipInfo("跌倒告警", "张** (3号楼)", "15:30:22", "15秒", "2024-07-10", false),
    ClipInfo("跌倒告警", "李** (5号楼)", "12:10:05", "15秒", "2024-07-10", false),
    ClipInfo("SOS呼救", "王** (1号楼)", "09:15:30", "15秒", "2024-07-09", true),
    ClipInfo("久坐不动", "张** (3号楼)", "16:45:00", "15秒", "2024-07-08", true)
)
