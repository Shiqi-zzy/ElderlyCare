package com.elderlycare.app.ui.family

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.model.ElderlyProfile
import com.elderlycare.app.ui.components.RiskLevel
import com.elderlycare.app.ui.components.StatusBadge
import com.elderlycare.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyHomeScreen(
    onNavigateToProfile: () -> Unit,
    onNavigateToReport: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToVideo: () -> Unit,
    onNavigateToAlertCenter: () -> Unit,
    onNavigateToAuthorizationMgmt: () -> Unit,
    onLogout: () -> Unit = {}
) {
    val profile = remember { ElderlyProfile(name = "张**", gender = com.elderlycare.app.data.model.Gender.MALE, age = "72", height = "170", weight = "68", bloodPressureHigh = "128", bloodPressureLow = "85") }
    val boundDevice = remember { ServiceLocator.deviceBindingStore.load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${profile.name}的看护助手", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = onLogout) { Icon(Icons.Filled.Logout, contentDescription = "退出登录") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. 实时视频
            VideoPreviewCard(
                deviceSerial = boundDevice?.deviceSerial,
                onClick = onNavigateToVideo
            )

            // 2. 用户状态卡片
            ElderlyStatusCard()

            // 3. 告警中心摘要
            AlertSummaryCard(onClick = onNavigateToAlertCenter)

            // 4. 机构授权管理摘要
            AuthorizationSummaryCard(onClick = onNavigateToAuthorizationMgmt)

            // 5. 档案摘要
            ProfileSummaryCard(profile = profile, onClick = onNavigateToProfile)

            // 6. 情绪倾向报告
            ReportSummaryCard(onClick = onNavigateToReport)

            // 7. 今日日程
            TodayScheduleCard(onClick = onNavigateToCalendar)
        }
    }
}

@Composable
private fun ElderlyStatusCard() {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(24.dp), color = Primary.copy(alpha = 0.1f), modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) { Text("张", fontWeight = FontWeight.Bold, color = Primary) }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("张**", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusBadge(text = "在线", color = StatusGreen)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("RK3 摄像头: 开启", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Switch(checked = true, onCheckedChange = {}, modifier = Modifier.height(20.dp))
                }
            }
            Button(onClick = {}, shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.buttonColors(containerColor = Error), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                Icon(Icons.Filled.Phone, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("紧急通话", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun AlertSummaryCard(onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("异常告警中心", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onClick, contentPadding = PaddingValues(0.dp)) { Text("查看全部", color = Primary) }
            }
            Spacer(modifier = Modifier.height(8.dp))
            AlertItem("跌倒检测", "15:30 客厅区域疑似跌倒", RiskLevel.RISK)
            AlertItem("久坐不动", "12:00 连续久坐超过4小时", RiskLevel.ATTENTION)
            AlertItem("设备离线", "08:00 RK3设备离线5分钟", RiskLevel.NORMAL)
        }
    }
}

@Composable
private fun AlertItem(type: String, desc: String, level: RiskLevel) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(level.color))
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) { Text(type, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium); Text(desc, style = MaterialTheme.typography.labelSmall, color = TextSecondary) }
        StatusBadge(text = level.label, color = level.color)
    }
}

@Composable
private fun AuthorizationSummaryCard(onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("机构授权管理", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onClick, contentPadding = PaddingValues(0.dp)) { Text("管理授权", color = Primary) }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Surface(shape = RoundedCornerShape(10.dp), color = Primary.copy(alpha = 0.06f), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) { Text("幸福社区养老驿站", style = MaterialTheme.typography.bodyMedium); Text("有效期至 2024-12-31", style = MaterialTheme.typography.labelSmall, color = TextHint) }
                    StatusBadge(text = "生效中", color = StatusGreen)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Surface(shape = RoundedCornerShape(10.dp), color = Secondary.copy(alpha = 0.06f), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) { Text("新华社区医院", style = MaterialTheme.typography.bodyMedium); Text("有效期至 2024-09-15", style = MaterialTheme.typography.labelSmall, color = TextHint) }
                    StatusBadge(text = "生效中", color = StatusGreen)
                }
            }
        }
    }
}

// --- 以下为原 HomeScreen 的卡片（保留不变）---

@Composable
private fun VideoPreviewCard(deviceSerial: String?, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)).background(Brush.verticalGradient(listOf(Color(0xFF3D5A73), Color(0xFF2C3E50)))), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Videocam, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    if (deviceSerial != null) "RK3 实时画面 · $deviceSerial" else "未绑定 RK3 设备",
                    color = Color.White.copy(alpha = 0.7f)
                )
                Row(modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)) {
                    StatusBadge(text = if (deviceSerial != null) "已绑定" else "未绑定", color = if (deviceSerial != null) StatusGreen else StatusYellow)
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("实时视频", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onClick) { Text(if (deviceSerial != null) "查看实时画面" else "请在档案录入中绑定", color = Primary); Icon(Icons.Filled.KeyboardArrowRight, null, tint = Primary, modifier = Modifier.size(18.dp)) }
            }
        }
    }
}

@Composable
private fun ProfileSummaryCard(profile: ElderlyProfile, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(24.dp), color = Primary.copy(alpha = 0.1f), modifier = Modifier.size(56.dp)) { Box(contentAlignment = Alignment.Center) { Text("张", fontWeight = FontWeight.Bold, color = Primary, style = MaterialTheme.typography.titleLarge) } }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row { Text("${profile.name} . ${profile.gender.label} . ${profile.age}岁", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium) }
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("BMI 23.5", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text("血压 ${profile.bloodPressureHigh}/${profile.bloodPressureLow}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
            Icon(Icons.Filled.KeyboardArrowRight, null, tint = TextHint)
        }
    }
}

@Composable
private fun ReportSummaryCard(onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) { Text("最新情绪倾向报告", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium); Spacer(modifier = Modifier.width(8.dp)); StatusBadge(text = "关注", color = StatusYellow) }
                Text("7月10日", style = MaterialTheme.typography.bodyMedium, color = TextHint)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
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
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("今日日程", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium); Text("3项", style = MaterialTheme.typography.bodyMedium, color = TextSecondary) }
            Spacer(modifier = Modifier.height(10.dp))
            ScheduleItem("8:00 服用降压药", completed = true)
            ScheduleItem("14:00 社区医院复诊", completed = false)
            ScheduleItem("19:00 正念呼吸练习", completed = false)
        }
    }
}

@Composable
private fun ScheduleItem(text: String, completed: Boolean) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), color = if (completed) TextHint else TextPrimary)
        if (completed) Icon(Icons.Filled.CheckCircle, null, tint = StatusGreen, modifier = Modifier.size(20.dp))
        else Surface(shape = RoundedCornerShape(12.dp), color = Primary.copy(alpha = 0.1f)) { Text("待办", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = Primary) }
    }
}
