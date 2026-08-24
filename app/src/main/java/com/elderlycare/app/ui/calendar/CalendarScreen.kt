package com.elderlycare.app.ui.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.elderlycare.app.ui.theme.*

// 页面风格常量（与首页统一：极浅蓝背景 + 极浅蓝渐变横幅）
private val PageBg = Color(0xFFF5F7FA)
private val BannerBlueStart = Color(0xFFEAF2FF)
private val BannerBlueEnd = Color(0xFFF5F9FF)
private val BannerText = Color(0xFF1A2332)
private val BannerTextSecondary = Color(0xFF4A5568)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CalendarScreen() {
    var selectedDay by remember { mutableIntStateOf(10) }
    var showCreateSheet by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val fabVisible by remember {
        derivedStateOf {
            scrollState.value == 0 || scrollState.value == scrollState.maxValue
        }
    }

    Scaffold(
        containerColor = PageBg,
        floatingActionButton = {
            AnimatedVisibility(
                visible = fabVisible,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                FloatingActionButton(
                    onClick = { showCreateSheet = true },
                    containerColor = Primary,
                    contentColor = OnPrimary,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "新建日程")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            // 蓝色渐变顶部横幅（标题 + 年月 + 视图切换）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(BannerBlueStart, BannerBlueEnd)
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Column {
                    Text(
                        "日程",
                        color = BannerText,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "2024年 7月",
                            color = BannerText,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("月", "周", "日").forEach { view ->
                                FilterChip(
                                    selected = view == "月",
                                    onClick = { },
                                    label = { Text(view, color = if (view == "月") Primary else BannerTextSecondary) },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color.White,
                                        containerColor = Color.White.copy(alpha = 0.6f)
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 日历网格
            CalendarGrid(selectedDay = selectedDay, onDayClick = { selectedDay = it })

            Spacer(modifier = Modifier.height(12.dp))

            // 选中日期的日程列表
            HorizontalDivider(color = CardBorder)
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "7月${selectedDay}日 日程",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (selectedDay == 10) {
                        DayScheduleItem("8:00", "服用降压药（硝苯地平）", completed = true, "用药提醒")
                        DayScheduleItem("14:00", "社区医院复诊 — 心内科", completed = false, "医院复诊")
                        DayScheduleItem("19:00", "5分钟正念呼吸练习", completed = false, "正念练习")
                    } else if (selectedDay == 9) {
                        DayScheduleItem("8:00", "服用降压药", completed = true, "用药提醒")
                        DayScheduleItem("10:00", "买菜", completed = true, "社区活动")
                    } else if (selectedDay == 15) {
                        DayScheduleItem("9:00", "年度体检", completed = false, "体检")
                    } else {
                        Text(
                            "暂无日程安排",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextHint,
                            modifier = Modifier.padding(vertical = 20.dp, horizontal = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // 新建日程弹窗
    if (showCreateSheet) {
        CreateScheduleSheet(
            onDismiss = { showCreateSheet = false },
            onSave = { showCreateSheet = false }
        )
    }
}

@Composable
private fun CalendarGrid(selectedDay: Int, onDayClick: (Int) -> Unit) {
    val daysInMonth = 31
    val startDayOffset = 1 // July 2024 starts on Monday (offset 1)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 星期标题
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("一", "二", "三", "四", "五", "六", "日").forEach { day ->
                    Text(
                        day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 日期网格
            for (row in 0..4) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0..6) {
                        val dayNumber = row * 7 + col - startDayOffset + 1
                        if (dayNumber in 1..daysInMonth) {
                            val hasSchedule = dayNumber in listOf(9, 10, 15)
                            val isSelected = dayNumber == selectedDay

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) Primary
                                        else if (hasSchedule) Primary.copy(alpha = 0.08f)
                                        else androidx.compose.ui.graphics.Color.Transparent
                                    )
                                    .clickable { onDayClick(dayNumber) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "$dayNumber",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isSelected) OnPrimary else TextPrimary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (hasSchedule) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) OnPrimary else Primary)
                                        )
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayScheduleItem(time: String, title: String, completed: Boolean, type: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 时间点圆圈标记
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (completed) StatusGreen else Primary)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (completed) TextHint else TextPrimary,
                textDecoration = if (completed) TextDecoration.LineThrough else TextDecoration.None
            )
            Row {
                Text(time, style = MaterialTheme.typography.labelSmall, color = TextHint)
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Primary.copy(alpha = 0.08f)
                ) {
                    Text(
                        type,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Primary
                    )
                }
            }
        }

        if (completed) {
            Icon(Icons.Filled.CheckCircle, contentDescription = "已完成", tint = StatusGreen, modifier = Modifier.size(24.dp))
        } else {
            IconButton(onClick = { }) {
                Icon(Icons.Filled.Circle, contentDescription = "标记完成", tint = TextHint.copy(alpha = 0.3f), modifier = Modifier.size(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateScheduleSheet(
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    var selectedType by remember { mutableStateOf("用药提醒") }
    var date by remember { mutableStateOf("2024-07-10") }
    var time by remember { mutableStateOf("08:00") }
    var repeatMode by remember { mutableStateOf("单次") }
    var note by remember { mutableStateOf("") }
    var customTypeText by remember { mutableStateOf("") }
    var customTypes by remember { mutableStateOf<List<String>>(emptyList()) }

    // 合并预设 + 自定义类型
    val presetTypes = listOf(
        "用药提醒", "医院复诊", "正念练习", "体检", "买菜", "社区活动",
        "血压测量", "血糖测量", "康复训练", "视频通话", "外出散步", "亲友探访"
    )
    val allTypes = presetTypes + customTypes

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text("新建日程", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))

            // 日程类型
            Text("日程类型", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                allTypes.forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text(type, style = MaterialTheme.typography.labelMedium) },
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }

            // 自定义类型输入
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = customTypeText,
                    onValueChange = { customTypeText = it },
                    label = { Text("自定义类型") },
                    placeholder = { Text("输入新类型名称") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Button(
                    onClick = {
                        val trimmed = customTypeText.trim()
                        if (trimmed.isNotBlank() && trimmed !in allTypes) {
                            customTypes = customTypes + trimmed
                            selectedType = trimmed
                            customTypeText = ""
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text("添加")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 日期 + 时间
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("日期") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("时间") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 重复模式
            Text("重复模式", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("单次", "每日重复", "每周重复").forEach { mode ->
                    FilterChip(
                        selected = repeatMode == mode,
                        onClick = { repeatMode = mode },
                        label = { Text(mode) },
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 备注
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("备注（可选）") },
                placeholder = { Text("关联病史、兴趣等信息") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 保存按钮
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("保存并下发至 RK3", modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}
