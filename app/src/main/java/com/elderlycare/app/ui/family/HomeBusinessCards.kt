package com.elderlycare.app.ui.family

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elderlycare.app.R
import com.elderlycare.app.data.model.ElderlyProfile
import com.elderlycare.app.ui.components.RiskLevel
import com.elderlycare.app.ui.components.StatusBadge
import com.elderlycare.app.ui.theme.*
import com.elderlycare.app.util.BMICalculator

/**
 * 原首页业务卡片组件（Phase 3 UI 重设计时从 FamilyHomeScreen 原样迁移到「我的」Tab 展示）。
 *
 * ⚠️ 卡片内部文案 / Mock 数据 / 点击逻辑与迁移前完全一致，业务零改动；
 * 仅把 private 提升为 internal 并换承载页面（MyScreen）。
 */

/** 用户状态卡片（含紧急通话入口） */
@Composable
internal fun ElderlyStatusCard(profile: ElderlyProfile, onEmergencyCall: () -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(24.dp), color = Primary.copy(alpha = 0.1f), modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(profile.name.firstOrNull()?.toString() ?: "老", fontWeight = FontWeight.Bold, color = Primary)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(profile.name.ifBlank { "未录入姓名" }, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusBadge(text = "在线", color = StatusGreen)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("RK3 摄像头: 开启", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Switch(checked = true, onCheckedChange = {}, modifier = Modifier.height(20.dp))
                }
            }
            Button(onClick = onEmergencyCall, shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.buttonColors(containerColor = Error), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                Icon(Icons.Filled.Phone, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("紧急通话", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

/** 异常告警中心摘要卡片 */
@Composable
internal fun AlertSummaryCard(onClick: () -> Unit) {
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

/** 留言入口卡片（带未读角标，数据来自留言 Room 未读数） */
@Composable
internal fun MessageSummaryCard(unreadCount: Int, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(12.dp), color = Primary.copy(alpha = 0.1f), modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.RecordVoiceOver, null, tint = Primary, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.message_title), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                    if (unreadCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(shape = RoundedCornerShape(10.dp), color = StatusRed) {
                            Text("$unreadCount", color = Color.White, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(stringResource(R.string.message_home_subtitle), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            }
            Icon(Icons.Filled.KeyboardArrowRight, null, tint = TextHint)
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

/** 机构授权管理摘要卡片 */
@Composable
internal fun AuthorizationSummaryCard(onClick: () -> Unit) {
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

/** 档案摘要卡片（BMI 由档案身高体重实算） */
@Composable
internal fun ProfileSummaryCard(profile: ElderlyProfile, onClick: () -> Unit) {
    val height = profile.height.toFloatOrNull() ?: 0f
    val weight = profile.weight.toFloatOrNull() ?: 0f
    val bmi = BMICalculator.calculate(weight, height)
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(24.dp), color = Primary.copy(alpha = 0.1f), modifier = Modifier.size(56.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(profile.name.firstOrNull()?.toString() ?: "老", fontWeight = FontWeight.Bold, color = Primary, style = MaterialTheme.typography.titleLarge)
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row {
                    Text(
                        buildString {
                            append(profile.name.ifBlank { "未录入姓名" })
                            append(" · ${profile.gender.label}")
                            if (profile.age.isNotBlank()) append(" · ${profile.age}岁")
                        },
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        if (bmi > 0f) "BMI $bmi" else "BMI 未录入",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Text(
                        if (profile.bloodPressureHigh.isNotBlank() || profile.bloodPressureLow.isNotBlank()) {
                            "血压 ${profile.bloodPressureHigh.ifBlank { "-" }}/${profile.bloodPressureLow.ifBlank { "-" }}"
                        } else {
                            "血压 未录入"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
            Icon(Icons.Filled.KeyboardArrowRight, null, tint = TextHint)
        }
    }
}

/** 最新情绪倾向报告摘要卡片 */
@Composable
internal fun ReportSummaryCard(onClick: () -> Unit) {
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

/** 今日日程摘要卡片 */
@Composable
internal fun TodayScheduleCard(onClick: () -> Unit) {
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
