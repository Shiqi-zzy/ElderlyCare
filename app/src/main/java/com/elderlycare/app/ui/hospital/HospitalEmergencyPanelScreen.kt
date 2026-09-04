package com.elderlycare.app.ui.hospital

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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.model.AppUser
import com.elderlycare.app.ui.components.StatusBadge
import com.elderlycare.app.ui.components.charts.ChartLegend
import com.elderlycare.app.ui.components.charts.DonutChart
import com.elderlycare.app.ui.components.charts.MetricRing
import com.elderlycare.app.ui.components.charts.PieSlice
import com.elderlycare.app.ui.shared.HealthCategory
import com.elderlycare.app.ui.shared.color
import com.elderlycare.app.ui.shared.formatTimestamp
import com.elderlycare.app.ui.shared.hasDevice
import com.elderlycare.app.ui.shared.healthCategory
import com.elderlycare.app.ui.theme.*
import kotlinx.coroutines.flow.flowOf

/** 医院急救大屏色值（深青绿色主题，与参考图统一） */
private val PageBg = Color(0xFFF5F9F8)
private val TealDark = Color(0xFF2A9D8F)
private val TealLight = Color(0xFF52B7A8)
private val CardWhite = Color.White
private val TextDark = Color(0xFF1A2E2A)
private val TextGray = Color(0xFF6B7C78)
private val TextHint = Color(0xFF9AA8A4)

/**
 * 医院「急救大屏」（参考图风格重构）。
 * 数据来源：bindingRepository.observeAccessibleElderly(当前医院工作人员) + communityRepository。
 * 点击事件：onUserClick / onLogout / onNavigateToAlarm / onNavigateToAllEvents。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HospitalEmergencyPanelScreen(
    onLogout: () -> Unit = {},
    onUserClick: (String) -> Unit = {},
    onNavigateToAlarm: () -> Unit = {},
    onNavigateToAllEvents: () -> Unit = {}
) {
    var staff by remember { mutableStateOf<AppUser?>(null) }
    LaunchedEffect(Unit) { staff = ServiceLocator.staffUserStore.getCurrentStaffUser() }

    val elderly by remember(staff?.phone) {
        if (staff != null) ServiceLocator.bindingRepository.observeAccessibleElderly(staff!!)
        else flowOf(emptyList())
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    val total = elderly.size
    val normal = elderly.count { it.profile.healthCategory() == HealthCategory.NORMAL }
    val attention = elderly.count { it.profile.healthCategory() == HealthCategory.ATTENTION }
    val abnormal = elderly.count { it.profile.healthCategory() == HealthCategory.ABNORMAL }
    val focus = elderly.filter { it.profile.healthCategory() != HealthCategory.NORMAL }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg)
            .verticalScroll(rememberScrollState())
    ) {
        // ===== 顶部 Logo 栏 =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(TealDark),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.LocalHospital, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text("银龄心语", color = TextDark, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("医护管理 · 急救大屏", color = TextGray, fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onLogout) {
                Icon(Icons.Filled.Logout, "退出登录", tint = TextGray, modifier = Modifier.size(22.dp))
            }
        }

        // ===== 深青绿色渐变横幅 =====
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.horizontalGradient(listOf(TealDark, TealLight))
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("当前重点: ${abnormal}人", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Text("需紧急关注", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("待响应事件", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("${abnormal}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "立即处理 ›",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable(onClick = onNavigateToAlarm)
                        )
                    }
                }
                // 救护车图标
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.LocalHospital, null, tint = Color.White, modifier = Modifier.size(36.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ===== 近期急救事件 =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("近期急救事件", color = TextDark, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                "全部",
                color = TealDark,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(onClick = onNavigateToAllEvents)
            )
            Icon(Icons.Filled.KeyboardArrowRight, null, tint = TealDark, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.height(10.dp))

        if (elderly.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Inbox, null, tint = TextHint, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("暂无可访问患者，可先发起绑定申请", color = TextHint, fontSize = 13.sp)
                    }
                }
            }
        }

        // 患者卡片（异常老人优先，最多显示2条，点击进入详情）
        val sortedElderly = elderly.sortedByDescending { it.profile.healthCategory() == HealthCategory.ABNORMAL }
        sortedElderly.take(2).forEach { item ->
            EmergencyPatientCard(
                name = item.profile.name,
                age = item.profile.age,
                gender = item.profile.gender.label,
                orgName = item.orgName,
                badgeText = item.profile.healthCategory().label,
                badgeColor = item.profile.healthCategory().color(),
                deviceText = "设备：${if (item.profile.hasDevice()) "已绑定" else "未绑定"} · ${formatTimestamp(item.bindingCreatedAt)}",
                onClick = { onUserClick(item.elderlyId) }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
        if (sortedElderly.size > 2) {
            Text(
                "还有 ${sortedElderly.size - 2} 位患者，点击\"全部\"查看",
                color = TextHint,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // ===== 底部：健康状态分布 + 服务患者（高度一致）=====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 健康状态分布
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Column(modifier = Modifier.padding(14.dp).fillMaxHeight()) {
                    Text("健康状态分布", color = TextDark, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(Modifier.height(10.dp))
                    val slices = listOf(
                        PieSlice("正常", normal.toFloat(), StatusGreen),
                        PieSlice("异常", abnormal.toFloat(), StatusRed)
                    )
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        DonutChart(slices = slices, modifier = Modifier.size(90.dp), centerLabel = total.toString())
                    }
                    Spacer(Modifier.height(8.dp))
                    ChartLegend(slices = slices)
                }
            }

            // 服务患者
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Column(modifier = Modifier.padding(14.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("服务患者", color = TextDark, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(Modifier.height(10.dp))
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        MetricRing("绑定患者", total.toString(), if (total > 0) 1f else 0f, TealDark)
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.TrendingUp, null, tint = StatusGreen, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("较昨日 +3", color = StatusGreen, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/** 急救事件患者卡片：头像 + 姓名/年龄/性别 + 机构 + 设备信息 + 状态徽章 + 查看详情按钮 */
@Composable
internal fun EmergencyPatientCard(
    name: String,
    age: String,
    gender: String,
    orgName: String,
    badgeText: String,
    badgeColor: Color,
    deviceText: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 头像
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(TealDark.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(name.take(1), color = TealDark, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(name, color = TextDark, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("${age}岁 · $gender", color = TextGray, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocationOn, null, tint = TextHint, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(orgName, color = TextGray, fontSize = 11.sp)
                    }
                }
                StatusBadge(text = badgeText, color = badgeColor)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(deviceText, color = TextHint, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth().height(40.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealDark, contentColor = Color.White)
            ) {
                Text("查看详情", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
