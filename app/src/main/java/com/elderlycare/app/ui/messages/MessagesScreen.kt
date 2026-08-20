package com.elderlycare.app.ui.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elderlycare.app.ui.components.RiskLevel
import com.elderlycare.app.ui.components.RiskLevelIndicator
import com.elderlycare.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("全部", "高风险", "日报", "日程", "系统")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("消息中心", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab 切换（与报告页面一致的 TabRow 风格）
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Surface,
                contentColor = Primary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // 消息列表 — 分栏竖屏布局
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val allMessages = getMockMessages()
                val filteredMessages = if (selectedTab == 0) allMessages
                else allMessages.filter { it.category == tabs[selectedTab] }

                items(filteredMessages) { message ->
                    MessageRow(message = message)
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }
}

data class Message(
    val title: String,
    val content: String,
    val time: String,
    val category: String,
    val level: RiskLevel
)

@Composable
fun MessageRow(message: Message) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 左侧风险等级指示条
            if (message.level == RiskLevel.RISK) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(72.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(message.level.color)
                )
                Spacer(modifier = Modifier.width(10.dp))
            }

            // 消息内容
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(message.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                    Text(message.time, style = MaterialTheme.typography.labelSmall, color = TextHint)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    RiskLevelIndicator(level = message.level, showLabel = true)
                    Spacer(modifier = Modifier.width(10.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Primary.copy(alpha = 0.08f)
                    ) {
                        Text(
                            message.category,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Primary
                        )
                    }
                }
            }

            // 侧面操作按钮列
            Column(
                modifier = Modifier.padding(start = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = { /* 标记已读 */ },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.DoneAll,
                        contentDescription = "标记已读",
                        tint = TextHint,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = { /* 删除 */ },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "删除",
                        tint = TextHint.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

fun getMockMessages(): List<Message> = listOf(
    Message(
        title = "中风险心理预警",
        content = "用户连续独处超过6小时，语音情绪分析显示低落倾向，建议关注",
        time = "今天 15:30",
        category = "高风险",
        level = RiskLevel.RISK
    ),
    Message(
        title = "今日情绪倾向日报",
        content = "7月10日报已生成：孤独指数38、抑郁倾向42（轻微上升）、活跃度65。综合评估：轻度孤独",
        time = "今天 08:00",
        category = "日报",
        level = RiskLevel.ATTENTION
    ),
    Message(
        title = "日程提醒：社区医院复诊",
        content = "14:00 社区医院心内科复诊，请确保用户按时前往",
        time = "今天 07:00",
        category = "日程",
        level = RiskLevel.NORMAL
    ),
    Message(
        title = "上午用药未确认",
        content = "8:00降压药提醒已下发至RK3，但用户尚未点击「已服药」确认",
        time = "今天 09:30",
        category = "日程",
        level = RiskLevel.ATTENTION
    ),
    Message(
        title = "昨日情绪倾向日报",
        content = "7月9日报：孤独指数32、抑郁倾向36、活跃度68。各项指标正常",
        time = "昨天 08:00",
        category = "日报",
        level = RiskLevel.NORMAL
    ),
    Message(
        title = "设备上线通知",
        content = "RK3机器人（SN: RK3-2024-A1B2C3）已恢复在线状态",
        time = "昨天 06:30",
        category = "系统",
        level = RiskLevel.NORMAL
    ),
    Message(
        title = "正念练习完成",
        content = "用户已完成19:00的5分钟正念呼吸练习，情绪状态记录为「平静」",
        time = "前天 19:05",
        category = "日程",
        level = RiskLevel.NORMAL
    )
)
