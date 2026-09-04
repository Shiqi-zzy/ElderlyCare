package com.elderlycare.app.ui.shared

import android.widget.Toast
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.model.AppUser
import com.elderlycare.app.ui.theme.*
import kotlinx.coroutines.flow.flowOf

/** 工作人员「我的」页色值（薄荷绿主题，与参考图统一） */
private val PageBg = Color(0xFFF5FAF7)
private val MintGreen = Color(0xFF4CAF8A)
private val MintGreenLight = Color(0xFF6BC9A8)
private val CardWhite = Color.White
private val TextDark = Color(0xFF1A2E25)
private val TextGray = Color(0xFF6B7C74)
private val TextHint = Color(0xFF9AA8A2)

/** 指标图标色 */
private val MetricBlue = Color(0xFF4A90D9)
private val MetricOrange = Color(0xFFFF9F38)
private val MetricGreen = Color(0xFF42BD67)
private val MetricPurple = Color(0xFF9C6BC9)

/**
 * 社区/医院「我的」共享页（参考图风格重构）：
 * 绿色渐变个人信息头 + 4项服务指标 + 我的服务8宫格 + 设置列表 + 退出登录。
 *
 * 数据来源保持不变：staffUserStore / bindingDao。
 * 点击事件保持不变：onLogout。其他功能入口为静态展示（Toast 提示）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffMyScreen(
    onLogout: () -> Unit = {},
    onNavigateToBinding: () -> Unit = {},
    onNavigateToFollowUp: () -> Unit = {},
    onNavigateToSchedule: () -> Unit = {},
    onNavigateToServiceRecord: () -> Unit = {},
    bindingLabel: String = "绑定用户"
) {
    val context = LocalContext.current
    var staff by remember { mutableStateOf<AppUser?>(null) }
    var orgName by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val s = ServiceLocator.staffUserStore.getCurrentStaffUser()
        staff = s
        orgName = s?.organizationId?.let { ServiceLocator.bindingDao.getOrganization(it)?.name } ?: ""
    }
    val currentStaff = staff
    val staffPhone = currentStaff?.phone ?: ""

    // 4指标实时数据：服务老人=绑定数，服务次数=服务记录数
    val boundElderly by remember(staffPhone) {
        if (staffPhone.isNotBlank()) ServiceLocator.bindingRepository.observeAccessibleElderly(currentStaff!!)
        else flowOf(emptyList())
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    val serviceRecords by remember(staffPhone) {
        if (staffPhone.isNotBlank()) ServiceLocator.communityRepository.observeServiceRecords(staffPhone)
        else flowOf(emptyList())
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    val serviceElderlyCount = boundElderly.size
    val serviceCount = serviceRecords.size

    fun toast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg)
            .verticalScroll(rememberScrollState())
    ) {
        // ===== 绿色渐变个人信息头 =====
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(listOf(MintGreen, MintGreenLight))
                )
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // 右上角图标
            Row(
                modifier = Modifier.align(Alignment.TopEnd),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.QrCodeScanner, null, tint = Color.White, modifier = Modifier.size(22.dp))
                Icon(Icons.Filled.Settings, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 圆形头像
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.25f),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            (currentStaff?.name ?: "护").take(1),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            currentStaff?.name ?: "未登录",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.25f)
                        ) {
                            Text(
                                "已认证",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        orgName.ifEmpty { "未关联机构" },
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        currentStaff?.role?.label ?: "工作人员",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
                Icon(Icons.Filled.KeyboardArrowRight, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(24.dp))
            }
        }

        // ===== 4项服务指标（白色圆角卡片上浮） =====
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .offset(y = (-12).dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(vertical = 14.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem(Icons.Filled.People, MetricBlue, "$serviceElderlyCount", if (currentStaff?.role?.name == "HOSPITAL") "服务长者" else "服务对象", Modifier.weight(1f))
                MetricItem(Icons.Filled.Assignment, MetricOrange, "$serviceCount", "服务次数", Modifier.weight(1f))
                MetricItem(Icons.Filled.Verified, MetricGreen, "98%", "好评率", Modifier.weight(1f))
                MetricItem(Icons.Filled.Star, MetricPurple, "4.9", "服务评分", Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ===== 我的服务（8宫格） =====
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("我的服务", color = TextDark, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(12.dp))
                // 第一行
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ServiceGridItem(Icons.Filled.CalendarMonth, "我的排班", onClick = onNavigateToSchedule)
                    ServiceGridItem(Icons.Filled.Description, "服务记录", onClick = onNavigateToServiceRecord)
                    ServiceGridItem(Icons.Filled.HealthAndSafety, "健康档案") { toast("健康档案") }
                }
                Spacer(modifier = Modifier.height(16.dp))
                // 第二行
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ServiceGridItem(Icons.Filled.FactCheck, "随访计划", onClick = onNavigateToFollowUp)
                    ServiceGridItem(Icons.Filled.Group, "团队协作") { toast("团队协作") }
                    ServiceGridItem(Icons.Filled.School, "培训学习") { toast("培训学习") }
                }
                Spacer(modifier = Modifier.height(16.dp))
                // 第三行
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ServiceGridItem(Icons.Filled.HelpOutline, "帮助中心") { toast("帮助中心") }
                    ServiceGridItem(Icons.Filled.Feedback, "意见反馈") { toast("意见反馈") }
                    ServiceGridItem(Icons.Filled.Link, bindingLabel, onClick = onNavigateToBinding)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ===== 设置列表 =====
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column {
                SettingListItem(Icons.Filled.Person, "个人信息", null) { toast("个人信息") }
                SettingListItem(Icons.Filled.Notifications, "消息通知", "3") { toast("消息通知") }
                SettingListItem(Icons.Filled.Settings, "系统设置", null) { toast("系统设置") }
                SettingListItem(Icons.Filled.Info, "关于我们", null, isLast = true) { toast("关于我们") }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ===== 退出登录 =====
        Button(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(50.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Error, contentColor = Color.White)
        ) {
            Text("退出登录", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/** 服务指标项：彩色圆形图标 + 数值 + 标签（居中） */
@Composable
private fun MetricItem(icon: ImageVector, iconBg: Color, value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconBg.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconBg, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(value, color = TextDark, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, color = TextGray, fontSize = 11.sp)
    }
}

/** 我的服务宫格项：彩色圆形图标 + 标签 */
@Composable
private fun ServiceGridItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(70.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MintGreen.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = MintGreen, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, color = TextDark, fontSize = 12.sp)
    }
}

/** 设置列表项：图标 + 文字 + 角标/箭头 */
@Composable
private fun SettingListItem(
    icon: ImageVector,
    label: String,
    badge: String? = null,
    isLast: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = CardWhite
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MintGreen.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = MintGreen, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(label, color = TextDark, fontSize = 14.sp, modifier = Modifier.weight(1f))
                if (badge != null) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Error),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(badge, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Icon(Icons.Filled.KeyboardArrowRight, null, tint = TextHint, modifier = Modifier.size(20.dp))
            }
            if (!isLast) {
                HorizontalDivider(
                    color = DividerColor,
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(start = 60.dp)
                )
            }
        }
    }
}
