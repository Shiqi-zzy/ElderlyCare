package com.elderlycare.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.elderlycare.app.data.model.ElderlyProfile
import com.elderlycare.app.ui.components.StatusBadge
import com.elderlycare.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToProfile: () -> Unit,
    onNavigateToReport: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToVideo: () -> Unit
) {
    // Mock 数据
    val profile = remember {
        ElderlyProfile(
            name = "张**", gender = com.elderlycare.app.data.model.Gender.MALE, age = "72",
            height = "170", weight = "68", bloodPressureHigh = "128", bloodPressureLow = "85"
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${profile.name}的看护助手", fontWeight = FontWeight.SemiBold)
                    }
                },
                actions = {
                    IconButton(onClick = { /* 消息 */ }) {
                        Icon(Icons.Filled.Notifications, contentDescription = "消息")
                    }
                },
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ① 视频快捷卡片
            VideoPreviewCard(onNavigateToVideo = onNavigateToVideo)

            // ② 用户档案摘要卡片
            ProfileSummaryCard(
                profile = profile,
                onClick = onNavigateToProfile
            )

            // ③ 最新情绪倾向报告摘要卡片
            ReportSummaryCard(onClick = onNavigateToReport)

            // ④ 今日日程卡片
            TodayScheduleCard(onClick = onNavigateToCalendar)
        }
    }
}

@Composable
private fun VideoPreviewCard(onNavigateToVideo: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onNavigateToVideo),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // 视频预览区（Mock占位）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .then(
                        Modifier.background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF3D5A73), Color(0xFF2C3E50))
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Videocam,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("RK3 实时画面", color = Color.White.copy(alpha = 0.7f))
                }

                // 设备在线状态
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusBadge(text = "在线", color = StatusGreen)
                }

                // 情绪标签
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Face,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("平静", color = Color.White, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // 底部操作
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("实时视频", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onNavigateToVideo) {
                    Text("查看实时画面", color = Primary)
                    Icon(
                        Icons.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileSummaryCard(
    profile: ElderlyProfile,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 头像
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Primary.copy(alpha = 0.1f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("EL", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Primary)
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${profile.name} · ${profile.gender.label} · ${profile.age}岁",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "BMI 23.5",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Text(
                        "血压 ${profile.bloodPressureHigh}/${profile.bloodPressureLow}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            Icon(
                Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = TextHint
            )
        }
    }
}

@Composable
private fun ReportSummaryCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("最新情绪倾向报告", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusBadge(text = "关注", color = StatusYellow)
                }
                Text("7月10日", style = MaterialTheme.typography.bodyMedium, color = TextHint)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 指标行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem("孤独指数", "38", "正常", StatusGreen)
                MetricItem("抑郁倾向", "42", "轻微上升", StatusYellow)
                MetricItem("活跃度", "65", "正常", StatusGreen)
            }
        }
    }
}

@Composable
private fun MetricItem(label: String, value: String, status: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Text(status, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
private fun TodayScheduleCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("今日日程", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                Text("3项", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }

            Spacer(modifier = Modifier.height(10.dp))

            ScheduleItem("8:00 服用降压药", completed = true)
            ScheduleItem("14:00 社区医院复诊", completed = false)
            ScheduleItem("19:00 正念呼吸练习", completed = false)
        }
    }
}

@Composable
private fun ScheduleItem(text: String, completed: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            color = if (completed) TextHint else TextPrimary,
            textDecoration = if (completed) androidx.compose.ui.text.style.TextDecoration.LineThrough else androidx.compose.ui.text.style.TextDecoration.None
        )
        if (completed) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = "已完成",
                tint = StatusGreen,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Primary.copy(alpha = 0.1f)
            ) {
                Text(
                    "待办",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Primary
                )
            }
        }
    }
}
