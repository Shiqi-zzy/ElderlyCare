package com.elderlycare.app.ui.community

import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elderlycare.app.R
import com.elderlycare.app.data.binding.BindingRepository.AccessibleElderlyUi
import com.elderlycare.app.data.community.TodoItem
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.model.AppUser
import com.elderlycare.app.ui.components.StatusBadge
import com.elderlycare.app.ui.components.charts.BarChart
import com.elderlycare.app.ui.shared.HealthCategory
import com.elderlycare.app.ui.shared.color
import com.elderlycare.app.ui.shared.hasDevice
import com.elderlycare.app.ui.shared.healthCategory
import com.elderlycare.app.ui.theme.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/** 社区工作台色值（薄荷绿主题，与参考图统一） */
private val PageBg = Color(0xFFF5FAF7)
private val MintGreen = Color(0xFF4CAF8A)
private val MintGreenLight = Color(0xFF6BC9A8)
private val MintGreenBg = Color(0xFFE8F5EF)
private val CardWhite = Color.White
private val TextDark = Color(0xFF1A2E25)
private val TextGray = Color(0xFF6B7C74)
private val TextHint = Color(0xFF9AA8A2)

/** 今日概览指标图标色 */
private val MetricBlue = Color(0xFF4A90D9)
private val MetricOrange = Color(0xFFFF9F38)
private val MetricRed = Color(0xFFF24848)
private val MetricGreen = Color(0xFF42BD67)

/**
 * 社区端工作台（参考图风格重构）：
 * 顶部 Logo 栏 + 绿色渐变问候横幅 + 今日概览4指标 + 待办事项列表 + 健康知识库横幅。
 *
 * 数据来源保持不变：bindingRepository.observeAccessibleElderly(当前工作人员)。
 * 点击事件保持不变：onLogout / onUserClick / 指标详情弹窗。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityDashboardScreen(
    onLogout: () -> Unit = {},
    onUserClick: (String) -> Unit = {},
    onNavigateToAllTodos: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var staff by remember { mutableStateOf<AppUser?>(null) }
    var detailTitle by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { staff = ServiceLocator.staffUserStore.getCurrentStaffUser() }

    val elderly by remember(staff?.phone) {
        if (staff != null) ServiceLocator.bindingRepository.observeAccessibleElderly(staff!!)
        else flowOf(emptyList())
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    val total = elderly.size
    val base = maxOf(1, total)
    val normal = elderly.count { it.profile.healthCategory() == HealthCategory.NORMAL }
    val attention = elderly.count { it.profile.healthCategory() == HealthCategory.ATTENTION }
    val abnormal = elderly.count { it.profile.healthCategory() == HealthCategory.ABNORMAL }
    val withDevice = elderly.count { it.profile.hasDevice() }

    // 待办事项（从真实数据获取，仅显示待处理）
    val todos by remember(staff?.phone) {
        if (staff != null) ServiceLocator.communityRepository.observePendingTodos(staff!!.phone)
        else flowOf(emptyList())
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    val pendingCount = attention + abnormal
    val completionRate = if (total > 0) (withDevice * 100 / total) else 0

    fun categoryList(title: String): List<AccessibleElderlyUi> = when (title) {
        "服务老人" -> elderly
        "待处理" -> elderly.filter { it.profile.healthCategory() != HealthCategory.NORMAL }
        "有设备" -> elderly.filter { it.profile.hasDevice() }
        "正常" -> elderly.filter { it.profile.healthCategory() == HealthCategory.NORMAL }
        "关注" -> elderly.filter { it.profile.healthCategory() == HealthCategory.ATTENTION }
        "异常" -> elderly.filter { it.profile.healthCategory() == HealthCategory.ABNORMAL }
        else -> emptyList()
    }

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
                Text("ElderlyCare", color = TextDark, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("社区端 · 工作台", color = TextGray, fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onLogout) {
                Icon(Icons.Filled.QrCodeScanner, "扫码/退出", tint = TextGray, modifier = Modifier.size(22.dp))
            }
        }

        // ===== 绿色渐变问候横幅 =====
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(MintGreen, MintGreenLight)
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "早上好，${staff?.name?.take(1) ?: ""}护士",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "幸福社区养老服务站",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp
                    )
                }
                // 天气
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.WbSunny, null, tint = Color.White, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("28°C", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                    }
                    Text("多云", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ===== 今日概览 =====
        Text(
            "今日概览",
            color = TextDark,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))

        // 4个指标卡片（1行并排）
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricCard(
                icon = Icons.Filled.People,
                iconBg = MetricBlue,
                value = "$total",
                label = "服务老人",
                onClick = { detailTitle = "服务老人" },
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                icon = Icons.Filled.Assignment,
                iconBg = MetricOrange,
                value = "$pendingCount",
                label = "待处理",
                onClick = { detailTitle = "待处理" },
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                icon = Icons.Filled.NotificationsActive,
                iconBg = MetricRed,
                value = "$abnormal",
                label = "重点关注",
                onClick = { detailTitle = "异常" },
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                icon = Icons.Filled.Verified,
                iconBg = MetricGreen,
                value = "$completionRate%",
                label = "完成率",
                onClick = { detailTitle = "有设备" },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ===== 待办事项 =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("待办事项", color = TextDark, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                "全部 (${todos.size})",
                color = MintGreen,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(onClick = onNavigateToAllTodos)
            )
            Icon(Icons.Filled.KeyboardArrowRight, null, tint = MintGreen, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.height(10.dp))

        // 待办列表（最多显示2条，完成后数据库Flow自动更新消失）
        if (todos.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite)
            ) {
                Text(
                    "暂无待办事项",
                    modifier = Modifier.padding(20.dp),
                    color = TextHint,
                    fontSize = 14.sp
                )
            }
        } else {
            todos.take(2).forEachIndexed { index, todo ->
                RealTodoItem(
                    todo = todo,
                    onComplete = {
                        scope.launch {
                            staff?.let { s ->
                                ServiceLocator.communityRepository.completeTodo(
                                    id = todo.id,
                                    staffId = s.phone,
                                    elderlyId = todo.elderlyId,
                                    elderlyName = todo.elderlyName,
                                    todoType = todo.todoType,
                                    content = todo.content
                                )
                            }
                        }
                    }
                )
                if (index < minOf(todos.size, 2) - 1) Spacer(modifier = Modifier.height(8.dp))
            }
            if (todos.size > 2) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "还有 ${todos.size - 2} 项待办，点击\"全部\"查看",
                    color = TextHint,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ===== 健康知识库横幅 =====
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MintGreenBg)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("健康知识库", color = TextDark, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("科学养老 · 健康生活", color = TextGray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { /* 健康知识库入口，暂留 */ },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MintGreen, contentColor = Color.White),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("去学习", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Image(
                    painter = painterResource(R.drawable.community_health_knowledge),
                    contentDescription = null,
                    modifier = Modifier.size(130.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    detailTitle?.let { title ->
        MetricDetailDialog(title, categoryList(title), onUserClick) { detailTitle = null }
    }
}

/** 今日概览指标卡片：彩色圆形图标 + 数值 + 标签 */
@Composable
private fun MetricCard(
    icon: ImageVector,
    iconBg: Color,
    value: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
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
                    .size(32.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(iconBg.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconBg, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, color = TextDark, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(label, color = TextGray, fontSize = 11.sp)
        }
    }
}

/** 待办事项条目：彩色图标 + 标题/姓名/描述 + 时间 + 箭头 */
@Composable
private fun TodoItem(
    icon: ImageVector,
    iconBg: Color,
    title: String,
    name: String,
    meta: String,
    time: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
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
                Icon(icon, null, tint = iconBg, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = TextDark, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text("$name · $meta", color = TextGray, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(time, color = iconBg, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Icon(Icons.Filled.KeyboardArrowRight, null, tint = TextHint, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** 真实待办事项条目：彩色图标 + 标题/内容 + 完成按钮 */
@Composable
private fun RealTodoItem(todo: TodoItem, onComplete: () -> Unit) {
    val iconBg = when (todo.priority) {
        TodoItem.PRIORITY_HIGH -> MetricRed
        else -> MetricOrange
    }
    val icon = when (todo.todoType) {
        "上门随访" -> Icons.Filled.DirectionsWalk
        "电话随访" -> Icons.Filled.Phone
        "健康随访" -> Icons.Filled.Favorite
        "告警消息" -> Icons.Filled.Warning
        else -> Icons.Filled.Assignment
    }
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
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(9.dp)).background(iconBg.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconBg, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(todo.title, color = TextDark, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1)
                Spacer(modifier = Modifier.height(2.dp))
                Text(todo.content, color = TextGray, fontSize = 11.sp, maxLines = 1)
            }
            IconButton(
                onClick = onComplete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Filled.CheckCircle, "完成", tint = MintGreen, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun MetricDetailDialog(title: String, items: List<AccessibleElderlyUi>, onUserClick: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        text = {
            if (items.isEmpty()) {
                Text("暂无数据", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            } else {
                Column {
                    items.forEach { item ->
                        Surface(onClick = { onDismiss(); onUserClick(item.elderlyId) }, color = Surface) {
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.profile.name, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyLarge)
                                    Text("${item.profile.age}岁 · ${item.profile.gender.label}", style = MaterialTheme.typography.labelSmall, color = TextHint)
                                }
                                StatusBadge(text = item.profile.healthCategory().label, color = item.profile.healthCategory().color())
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}
