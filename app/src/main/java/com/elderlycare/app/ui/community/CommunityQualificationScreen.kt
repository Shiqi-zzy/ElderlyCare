package com.elderlycare.app.ui.community

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.model.AppUser
import com.elderlycare.app.data.model.QualificationStatus
import com.elderlycare.app.ui.components.InfoRow
import com.elderlycare.app.ui.components.StatusBadge
import com.elderlycare.app.ui.theme.*
import kotlinx.coroutines.launch

/** 社区工作资格页色值（薄荷绿主题，与参考图统一） */
private val PageBg = Color(0xFFF5FAF7)
private val MintGreen = Color(0xFF4CAF8A)
private val MintGreenLight = Color(0xFF6BC9A8)
private val CardWhite = Color.White
private val TextDark = Color(0xFF1A2E25)
private val TextGray = Color(0xFF6B7C74)
private val TextHint = Color(0xFF9AA8A2)

/** 资格概览指标图标色 */
private val QualGreen = Color(0xFF42BD67)
private val QualOrange = Color(0xFFFF9F38)
private val QualBlue = Color(0xFF4A90D9)
private val QualRed = Color(0xFFF24848)

/**
 * 社区端「工作资格」（参考图风格重构）：
 * 顶部 Logo 栏 + 绿色渐变「持证上岗」横幅 + 资格概览4指标 + 我的资格证书列表。
 *
 * 数据来源保持不变：staffUserStore / bindingDao / qualification。
 * 点击事件保持不变：demoSetQualification。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityQualificationScreen() {
    var staff by remember { mutableStateOf<AppUser?>(null) }
    var orgName by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        val s = ServiceLocator.staffUserStore.getCurrentStaffUser()
        staff = s
        orgName = s?.organizationId?.let { ServiceLocator.bindingDao.getOrganization(it)?.name } ?: ""
    }
    val currentStaff = staff
    val qualStatus = runCatching {
        QualificationStatus.valueOf(currentStaff?.qualification ?: QualificationStatus.APPROVED.name)
    }.getOrDefault(QualificationStatus.APPROVED)

    fun demoSetQualification(status: QualificationStatus) {
        val s = staff ?: return
        scope.launch {
            ServiceLocator.staffUserStore.updateUser(s.copy(qualification = status.name))
            staff = ServiceLocator.staffUserStore.getCurrentStaffUser()
        }
    }

    // 资格概览统计
    val certifiedCount = if (qualStatus == QualificationStatus.APPROVED) 2 else 1
    val pendingCount = if (qualStatus == QualificationStatus.PENDING) 1 else 0
    val expiringCount = 0
    val expiredCount = 0

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
                    .background(MintGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Favorite, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text("银龄心语", color = TextDark, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("社区端 · 工作资格", color = TextGray, fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.weight(1f))
            Box {
                Icon(Icons.Filled.Notifications, null, tint = TextGray, modifier = Modifier.size(22.dp))
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(QualRed),
                    contentAlignment = Alignment.Center
                ) {
                    Text("3", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // ===== 绿色渐变「持证上岗」横幅 =====
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.horizontalGradient(listOf(MintGreen, MintGreenLight))
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("持证上岗 · 专业服务", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("证照齐全，服务更安心", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                }
                // 证书插画占位
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.WorkspacePremium, null, tint = Color.White, modifier = Modifier.size(36.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ===== 资格概览 =====
        Text("资格概览", color = TextDark, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QualMetricCard(Icons.Filled.Verified, QualGreen, "$certifiedCount", "已认证", Modifier.weight(1f))
            QualMetricCard(Icons.Filled.Schedule, QualOrange, "$pendingCount", "待审核", Modifier.weight(1f))
            QualMetricCard(Icons.Filled.HourglassEmpty, QualBlue, "$expiringCount", "即将到期", Modifier.weight(1f))
            QualMetricCard(Icons.Filled.Cancel, QualRed, "$expiredCount", "已过期", Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ===== 我的资格证书 =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("我的资格证书", color = TextDark, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(modifier = Modifier.weight(1f))
            Text("全部", color = TextGray, fontSize = 13.sp)
            Icon(Icons.Filled.KeyboardArrowRight, null, tint = TextHint, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.height(10.dp))

        // 证书列表
        CertificateItem(
            icon = Icons.Filled.Verified,
            iconBg = QualGreen,
            title = "照护护理员证",
            status = "已认证",
            statusColor = StatusGreen,
            detail1 = "证书编号：210***********28",
            detail2 = "有效期至：2027-06-30"
        )
        Spacer(modifier = Modifier.height(8.dp))
        CertificateItem(
            icon = Icons.Filled.Favorite,
            iconBg = QualBlue,
            title = "健康管理师证",
            status = "已认证",
            statusColor = StatusGreen,
            detail1 = "证书编号：210***********16",
            detail2 = "有效期至：2027-08-18"
        )
        Spacer(modifier = Modifier.height(8.dp))
        CertificateItem(
            icon = Icons.Filled.MedicalServices,
            iconBg = QualOrange,
            title = "急救培训证书",
            status = if (qualStatus == QualificationStatus.PENDING) "待审核" else "已认证",
            statusColor = if (qualStatus == QualificationStatus.PENDING) StatusYellow else StatusGreen,
            detail1 = "证书编号：E202***********09",
            detail2 = if (qualStatus == QualificationStatus.PENDING) "申请时间：2026-05-20" else "有效期至：2028-03-12"
        )

        // 审核中状态的演示操作
        if (qualStatus == QualificationStatus.PENDING) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { demoSetQualification(QualificationStatus.APPROVED) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) { Text("模拟审核通过（演示）", fontSize = 12.sp) }
                OutlinedButton(
                    onClick = { demoSetQualification(QualificationStatus.REJECTED) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) { Text("模拟驳回（演示）", fontSize = 12.sp) }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/** 资格概览指标卡片：彩色圆形图标 + 数值 + 标签（居中） */
@Composable
private fun QualMetricCard(
    icon: ImageVector,
    iconBg: Color,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clip(RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconBg.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconBg, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, color = TextDark, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(label, color = TextGray, fontSize = 11.sp)
        }
    }
}

/** 资格证书条目：彩色图标 + 标题/状态 + 详情 + 箭头 */
@Composable
private fun CertificateItem(
    icon: ImageVector,
    iconBg: Color,
    title: String,
    status: String,
    statusColor: Color,
    detail1: String,
    detail2: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconBg, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, color = TextDark, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusBadge(text = status, color = statusColor)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(detail1, color = TextGray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(detail2, color = TextHint, fontSize = 11.sp)
            }
            Icon(Icons.Filled.KeyboardArrowRight, null, tint = TextHint, modifier = Modifier.size(20.dp))
        }
    }
}
