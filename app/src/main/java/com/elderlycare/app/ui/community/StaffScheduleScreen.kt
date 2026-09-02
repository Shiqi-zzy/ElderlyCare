package com.elderlycare.app.ui.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elderlycare.app.data.community.StaffScheduleRecord
import com.elderlycare.app.data.incident.ScheduleMode
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.model.AppUser
import com.elderlycare.app.ui.components.StatusBadge
import com.elderlycare.app.ui.theme.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/** 排班页色值（薄荷绿主题） */
private val PageBg = Color(0xFFF5FAF7)
private val MintGreen = Color(0xFF4CAF8A)
private val CardWhite = Color.White
private val TextDark = Color(0xFF1A2E25)
private val TextGray = Color(0xFF6B7C74)
private val TextHint = Color(0xFF9AA8A2)
private val WEEK_CN = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

/** 社区端「我的排班」页面：排班列表 + 添加排班 + 完成标记。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffScheduleScreen(onNavigateBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var staff by remember { mutableStateOf<AppUser?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { staff = ServiceLocator.staffUserStore.getCurrentStaffUser() }

    val schedules by remember(staff?.phone) {
        if (staff != null) ServiceLocator.communityRepository.observeSchedules(staff!!.phone)
        else flowOf(emptyList())
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    val pending = schedules.filter { it.status == StaffScheduleRecord.STATUS_PENDING }
    val done = schedules.filter { it.status == StaffScheduleRecord.STATUS_DONE }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的排班", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                actions = { IconButton(onClick = { showAddDialog = true }) { Icon(Icons.Filled.Add, "添加排班") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(PageBg).padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text("待执行 (${pending.size})", color = TextDark, fontWeight = FontWeight.SemiBold, fontSize = 16.sp) }
            if (pending.isEmpty()) {
                item { Text("暂无待执行排班", color = TextHint, fontSize = 13.sp) }
            }
            items(pending, key = { it.id }) { item ->
                ScheduleCard(
                    record = item,
                    onComplete = { scope.launch { ServiceLocator.communityRepository.completeSchedule(item.id) } }
                )
            }

            if (done.isNotEmpty()) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                item { Text("已完成 (${done.size})", color = TextGray, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }
                items(done, key = { it.id }) { item -> ScheduleCard(record = item, onComplete = null) }
            }
        }
    }

    if (showAddDialog) {
        AddScheduleDialog(
            staff = staff,
            onDismiss = { showAddDialog = false },
            onConfirm = { title, date, start, end, location ->
                scope.launch {
                    staff?.let { s ->
                        ServiceLocator.communityRepository.createSchedule(s.phone, title, date, start, end, location)
                    }
                    showAddDialog = false
                }
            }
        )
    }
}

@Composable
private fun ScheduleCard(record: StaffScheduleRecord, onComplete: (() -> Unit)?) {
    val isDone = record.status == StaffScheduleRecord.STATUS_DONE
    val dateStr = if (record.scheduleMode == ScheduleMode.WEEKLY && record.weekday in 1..7) "每${WEEK_CN[record.weekday - 1]}" else SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date(record.scheduleDate))
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(9.dp)).background(MintGreen.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.CalendarMonth, null, tint = MintGreen, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(record.title, color = TextDark, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("$dateStr ${record.startTime}-${record.endTime}", color = TextGray, fontSize = 12.sp)
                }
                StatusBadge(text = record.status, color = if (isDone) StatusGreen else StatusYellow)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocationOn, null, tint = TextHint, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(record.location, color = TextGray, fontSize = 12.sp)
            }
            if (onComplete != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onComplete,
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MintGreen, contentColor = Color.White)
                ) {
                    Icon(Icons.Filled.Check, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("标记已完成", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddScheduleDialog(
    staff: AppUser?,
    onDismiss: () -> Unit,
    onConfirm: (title: String, date: Long, start: String, end: String, location: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("09:00") }
    var endTime by remember { mutableStateOf("12:00") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加排班", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("排班标题") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("地点") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = startTime, onValueChange = { startTime = it }, label = { Text("开始") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = endTime, onValueChange = { endTime = it }, label = { Text("结束") }, modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank() && location.isNotBlank()) {
                        onConfirm(title, System.currentTimeMillis(), startTime, endTime, location)
                    }
                },
                enabled = title.isNotBlank() && location.isNotBlank()
            ) { Text("确认") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
