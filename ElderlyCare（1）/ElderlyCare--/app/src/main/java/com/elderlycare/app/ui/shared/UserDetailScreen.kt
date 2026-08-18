package com.elderlycare.app.ui.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elderlycare.app.ui.components.StatusBadge
import com.elderlycare.app.ui.theme.*

/**
 * 共享「用户详情」屏：社区/医院点击用户后展示其脱敏信息。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetailScreen(userId: String, onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("用户详情", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 用户头部
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Primary.copy(alpha = 0.1f),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                userId.take(1).ifBlank { "用" },
                                fontWeight = FontWeight.Bold,
                                color = Primary,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(userId, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.width(8.dp))
                            StatusBadge(text = "在线", color = StatusGreen)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("72岁 · 男 · 3号楼", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }
            }

            // 基本信息
            DetailCard("基本信息") {
                InfoRow("姓名", userId)
                InfoRow("年龄", "72")
                InfoRow("性别", "男")
                InfoRow("地址", "3号楼 201室")
                InfoRow("联系电话", "138****1234")
            }

            // 设备状态
            DetailCard("设备状态") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("RK3 设备", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    StatusBadge(text = "在线", color = StatusGreen)
                }
                Spacer(Modifier.height(4.dp))
                Text("最近心跳：今天 15:30", style = MaterialTheme.typography.labelSmall, color = TextHint)
            }

            // 健康/告警摘要
            DetailCard("健康与告警摘要") {
                InfoRow("慢病", "高血压、高血脂")
                InfoRow("近7天告警", "跌倒 2 次 · 久坐 1 次")
                InfoRow("上次随访", "2024-07-10")
            }
        }
    }
}

@Composable
private fun DetailCard(title: String, content: @Composable () -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.width(88.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
