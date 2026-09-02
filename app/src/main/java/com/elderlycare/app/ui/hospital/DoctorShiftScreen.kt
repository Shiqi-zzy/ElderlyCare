package com.elderlycare.app.ui.hospital

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elderlycare.app.data.community.StaffScheduleRecord
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.incident.ScheduleMode
import com.elderlycare.app.data.model.AppUser
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Bg = Color(0xFFF2F6FC)
private val White = Color.White
private val Dark = Color(0xFF16243A)
private val Gray = Color(0xFF5E6B80)
private val Blue = Color(0xFF3F8FE0)

private val WEEK = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

/**
 * 医生值班排班：按周循环（weekday + 时段）/ 指定日期双模式。
 * 在班判定由 IncidentRepository 依据此处排班执行；无人在班时漏接不罚医生、只记排班空缺。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DoctorShiftScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var staff by remember { mutableStateOf<AppUser?>(null) }
    LaunchedEffect(Unit) { staff = ServiceLocator.staffUserStore.getCurrentStaffUser() }
    val doctorId = staff?.phone ?: ""

    val shifts by remember(doctorId) {
        if (doctorId.isBlank()) kotlinx.coroutines.flow.flowOf(emptyList())
        else ServiceLocator.incidentRepository.observeShifts(doctorId, "hospital")
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    var weekday by remember { mutableStateOf(0) }
    var start by remember { mutableStateOf("08:00") }
    var end by remember { mutableStateOf("16:00") }

    Scaffold(
        containerColor = Bg,
        topBar = {
            TopAppBar(
                title = { Text("我的值班排班", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Filled.ArrowBack, "返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = White, titleContentColor = Dark, navigationIconContentColor = Dark)
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Card(
                Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = White), elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("新增班次（按周循环）", color = Dark, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Spacer(Modifier.height(10.dp))
                    Text("星期", color = Gray, fontSize = 12.sp)
                    FlowRow(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        WEEK.forEachIndexed { i, name ->
                            FilterChip(
                                selected = weekday == i,
                                onClick = { weekday = i },
                                label = { Text(name, fontSize = 12.sp) }
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(value = start, onValueChange = { start = it }, label = { Text("开始 HH:mm") },
                            modifier = Modifier.weight(1f), singleLine = true)
                        Text("—", color = Gray)
                        OutlinedTextField(value = end, onValueChange = { end = it }, label = { Text("结束 HH:mm") },
                            modifier = Modifier.weight(1f), singleLine = true)
                    }
                    Spacer(Modifier.height(8.dp))
                    // 快捷时段
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(onClick = { start = "00:00"; end = "12:00" }, label = { Text("00-12") })
                        AssistChip(onClick = { start = "12:00"; end = "23:59" }, label = { Text("12-24") })
                        AssistChip(onClick = { start = "08:00"; end = "16:00" }, label = { Text("白班") })
                        AssistChip(onClick = { start = "16:00"; end = "23:59" }, label = { Text("晚班") })
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val doctor = staff ?: return@Button
                            if (!isHhmm(start) || !isHhmm(end)) {
                                Toast.makeText(context, "时间格式应为 HH:mm", Toast.LENGTH_SHORT).show(); return@Button
                            }
                            scope.launch {
                                ServiceLocator.incidentRepository.createShift(
                                    staffId = doctor.phone,
                                    title = "${WEEK[weekday]}值班 $start-$end",
                                    scheduleDate = System.currentTimeMillis(),
                                    startTime = start, endTime = end,
                                    location = "新华社区医院急诊前台",
                                    scheduleMode = ScheduleMode.WEEKLY,
                                    weekday = weekday + 1,
                                    role = "hospital"
                                )
                                Toast.makeText(context, "班次已添加", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Blue)
                    ) { Text("添加班次") }
                }
            }

            Spacer(Modifier.height(14.dp))
            Text("我的班次（${shifts.size}）", color = Dark, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            if (shifts.isEmpty()) Text("暂无排班", color = Gray, fontSize = 13.sp)
            shifts.sortedWith(compareBy({ it.weekday }, { it.startTime })).forEach { ShiftRow(it) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun isHhmm(s: String): Boolean = Regex("""^\d{1,2}:\d{2}$""").matches(s)

@Composable
private fun ShiftRow(s: StaffScheduleRecord) {
    Card(
        Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = White), elevation = CardDefaults.cardElevation(0.5.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.Icon(Icons.Filled.Schedule, null, tint = Blue, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(if (s.weekday in 1..7) WEEK[s.weekday - 1] else "指定日期", color = Dark, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text("${s.startTime} - ${s.endTime} · ${s.location}", color = Gray, fontSize = 12.sp)
            }
        }
    }
}
