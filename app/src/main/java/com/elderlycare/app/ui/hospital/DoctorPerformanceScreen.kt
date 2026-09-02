package com.elderlycare.app.ui.hospital

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
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
import com.elderlycare.app.data.incident.DoctorPerformance
import com.elderlycare.app.data.incident.PenaltyLevel
import com.elderlycare.app.data.incident.PenaltyType
import com.elderlycare.app.data.model.AppUser
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Bg = Color(0xFFF2F6FC)
private val White = Color.White
private val Dark = Color(0xFF16243A)
private val Gray = Color(0xFF5E6B80)
private val Orange = Color(0xFFFA8C16)
private val Red = Color(0xFFEA4E4E)
private val Green = Color(0xFF52C41A)

/**
 * 医生值班绩效：初始 100 分；在班漏接 -5、升级后仍漏接 -10；
 * 累计 3 次院内通报、5 次暂停排班。无人在班不扣医生分（只记排班管理问题）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorPerformanceScreen(onNavigateBack: () -> Unit) {
    var staff by remember { mutableStateOf<AppUser?>(null) }
    var perf by remember { mutableStateOf<DoctorPerformance?>(null) }
    LaunchedEffect(Unit) {
        staff = ServiceLocator.staffUserStore.getCurrentStaffUser()
        val doctor = staff ?: return@LaunchedEffect
        while (isActive) {
            perf = ServiceLocator.incidentRepository.buildPerformance(doctor)
            delay(2000)
        }
    }
    val penalties by remember(staff?.phone) {
        if (staff == null) kotlinx.coroutines.flow.flowOf(emptyList())
        else ServiceLocator.incidentRepository.observePenalties(staff!!.phone)
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    Scaffold(
        containerColor = Bg,
        topBar = {
            TopAppBar(
                title = { Text("值班绩效", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Filled.ArrowBack, "返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = White, titleContentColor = Dark, navigationIconContentColor = Dark)
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp)) {
            val p = perf
            // 分数大卡
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF3F8FE0), Color(0xFF6FB1F2))))
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(staff?.name ?: "医生", color = White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("值班服务分（初始 100）", color = White.copy(0.85f), fontSize = 12.sp)
                    }
                    Icon(Icons.Filled.Star, null, tint = White, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("${p?.score ?: 100}", color = White, fontWeight = FontWeight.Bold, fontSize = 34.sp)
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(Modifier.weight(1f), "成功处警", "${p?.acceptedCount ?: 0}", Green)
                StatCard(Modifier.weight(1f), "在班漏接", "${p?.missedCount ?: 0}", Red)
                StatCard(Modifier.weight(1f), "平均响应", p?.avgResponseSeconds?.let { "${it}s" } ?: "—", Orange)
            }
            Spacer(Modifier.height(8.dp))
            Card(
                Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = White)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("处罚规则", color = Dark, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(Modifier.height(6.dp))
                    RuleLine("在班漏接（加急 3 次未接）", "-5 分/次")
                    RuleLine("升级承接后仍未响应", "-10 分/次")
                    RuleLine("累计漏接 3 次", "院内通报")
                    RuleLine("累计漏接 5 次", "暂停排班")
                    RuleLine("事发时无人在班", "不罚医生，记排班管理问题")
                }
            }
            Spacer(Modifier.height(14.dp))
            Text("处罚记录（${penalties.size}）", color = Dark, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(6.dp))
            if (penalties.isEmpty()) Text("暂无处罚记录", color = Gray, fontSize = 13.sp)
            penalties.forEach { pen ->
                Card(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = White)
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(36.dp).clip(CircleShape).background(Red.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                            Text("${pen.scoreDelta}", color = Red, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(PenaltyType.LABEL[pen.penaltyType] ?: pen.penaltyType, color = Dark, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(
                                "${PenaltyLevel.LABEL[pen.level] ?: pen.level} · 事件 #${pen.incidentId} · ${pf.format(Date(pen.createdAt))}",
                                color = Gray, fontSize = 12.sp
                            )
                        }
                        if (pen.status != "active") Text("已撤销", color = Gray, fontSize = 11.sp)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatCard(modifier: Modifier, label: String, value: String, tint: Color) {
    Card(modifier, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = White)) {
        Column(Modifier.padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = tint, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.height(2.dp))
            Text(label, color = Gray, fontSize = 11.sp)
        }
    }
}

@Composable
private fun RuleLine(a: String, b: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text("· $a", color = Dark, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(b, color = Red, fontSize = 12.sp)
    }
}

private val pf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
