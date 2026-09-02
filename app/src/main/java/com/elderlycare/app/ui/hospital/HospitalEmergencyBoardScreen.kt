package com.elderlycare.app.ui.hospital

import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.incident.CommunityCell
import com.elderlycare.app.data.incident.IncidentEntity
import com.elderlycare.app.data.incident.IncidentStatus
import com.elderlycare.app.data.model.AppUser
import com.elderlycare.app.ui.components.charts.ChartLegend
import com.elderlycare.app.ui.components.charts.DonutChart
import com.elderlycare.app.ui.components.charts.MetricRing
import com.elderlycare.app.ui.components.charts.PieSlice
import com.elderlycare.app.ui.shared.HealthCategory
import com.elderlycare.app.ui.shared.healthCategory
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Bg = Color(0xFFF5F9F8)
private val White = Color.White
private val Dark = Color(0xFF16243A)
private val Gray = Color(0xFF5E6B80)
private val Blue = Color(0xFF3F8FE0)
private val Teal = Color(0xFF2FB5AC)
private val TealDark = Color(0xFF2A9D8F)
private val TealLight = Color(0xFF52B7A8)
private val Green = Color(0xFF52C41A)
private val Orange = Color(0xFFFA8C16)
private val Red = Color(0xFFEA4E4E)

/** 大屏每行排布的社区数（调大可在社区增多时控制纵向占地） */
private const val GRID_COLUMNS = 3

/**
 * 医院前台急救大屏 Tab（自上而下）：
 * 1) 最顶端深青绿渐变概览横幅；2) 排班/社区绑定/绩效快捷入口；
 * 3) 紧凑合作社区虚拟网格（收到急救告警方块闪红，处警/完成后停止；每行 3 个防止社区过多过长）；
 * 4) 本院急救事件队列（一键处警先接先得 → 处置完成）；5) 健康分布/服务患者统计。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HospitalEmergencyBoardScreen(
    onNavigateToBinding: () -> Unit,
    onNavigateToShift: () -> Unit,
    onNavigateToPerformance: () -> Unit,
    onUserClick: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var staff by remember { mutableStateOf<AppUser?>(null) }
    LaunchedEffect(Unit) { staff = ServiceLocator.staffUserStore.getCurrentStaffUser() }
    val orgId = staff?.organizationId ?: "org_hospital_01"

    // 可访问患者（健康分布/服务患者统计）
    val accessible by remember(staff?.phone) {
        if (staff != null) ServiceLocator.bindingRepository.observeAccessibleElderly(staff!!) else flowOf(emptyList())
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val total = accessible.size
    val normal = accessible.count { it.profile.healthCategory() == HealthCategory.NORMAL }
    val abnormal = total - normal

    var cells by remember { mutableStateOf<List<CommunityCell>>(emptyList()) }
    LaunchedEffect(orgId) {
        while (isActive) {
            cells = ServiceLocator.incidentRepository.buildHospitalGrid(orgId)
            delay(1500)
        }
    }
    val incidents by remember(orgId) {
        ServiceLocator.incidentRepository.observeByHospital(orgId)
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val active = incidents.filter { !IncidentStatus.isTerminal(it.status) }
    val recent = incidents.filter { IncidentStatus.isTerminal(it.status) }.take(10)

    val transition = rememberInfiniteTransition(label = "h")
    val phase by transition.animateFloat(0.25f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "hp")

    var completeTarget by remember { mutableStateOf<IncidentEntity?>(null) }
    fun toast(m: String) = Toast.makeText(context, m, Toast.LENGTH_SHORT).show()

    Column(Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState())) {
        // ===== 1) 最顶端：深青绿渐变概览横幅 =====
        Box(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp).clip(RoundedCornerShape(20.dp))
                .background(Brush.horizontalGradient(listOf(TealDark, TealLight))).padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("待处警: ${active.size}起", color = White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(Modifier.width(8.dp))
                        Surface(shape = RoundedCornerShape(8.dp), color = White.copy(alpha = 0.2f)) {
                            Text(if (active.isEmpty()) "暂无急救" else "需立即处理", color = White, fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("服务患者 $total 人 · 重点关注 $abnormal 人", color = White.copy(0.9f), fontSize = 13.sp)
                }
                Box(Modifier.size(60.dp).clip(RoundedCornerShape(12.dp)).background(White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.LocalHospital, null, tint = White, modifier = Modifier.size(34.dp))
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ===== 2) 快捷入口 =====
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniEntry(Modifier.weight(1f), Icons.Filled.Handshake, "社区绑定", Teal, onNavigateToBinding)
            MiniEntry(Modifier.weight(1f), Icons.Filled.CalendarMonth, "我的排班", Blue, onNavigateToShift)
            MiniEntry(Modifier.weight(1f), Icons.Filled.Insights, "值班绩效", Orange, onNavigateToPerformance)
        }

        Spacer(Modifier.height(14.dp))

        // ===== 3) 紧凑大屏：绑定社区网格（每行 GRID_COLUMNS 个）=====
        Card(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = White),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column(Modifier.padding(14.dp)) {
                Text("急救大屏 · 合作社区", color = Dark, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text(
                    if (active.isEmpty()) "全部社区暂无急救告警" else "${active.size} 起急救事件待处置",
                    color = if (active.isEmpty()) Green else Red, fontSize = 12.sp
                )
                Spacer(Modifier.height(10.dp))
                if (cells.isEmpty()) {
                    Text("尚未绑定社区，请先在「社区绑定」申请并经管理端审批", color = Gray, fontSize = 12.sp)
                } else {
                    cells.chunked(GRID_COLUMNS).forEach { rowCells ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowCells.forEach { c -> CommunityBlock(c, phase, Modifier.weight(1f)) }
                            repeat(GRID_COLUMNS - rowCells.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Text("  待处置急救事件", color = Dark, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Spacer(Modifier.height(6.dp))
        if (active.isEmpty()) {
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = White)) {
                Box(Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
                    Text("暂无待处置事件", color = Gray, fontSize = 13.sp)
                }
            }
        } else {
            active.forEach { inc ->
                HospitalEventCard(
                    inc = inc,
                    currentDoctorId = staff?.phone.orEmpty(),
                    onAccept = {
                        val doctor = staff ?: return@HospitalEventCard
                        scope.launch {
                            val ok = ServiceLocator.incidentRepository.acceptByDoctor(inc.id, doctor)
                            toast(if (ok) "已接单（先接先得），请尽快处警" else "该事件已被其他值班医生接走")
                        }
                    },
                    onComplete = { completeTarget = inc },
                    onUserClick = onUserClick
                )
            }
        }

        if (recent.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Text("  近期已处置（${recent.size}）", color = Gray, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            recent.forEach { inc -> HospitalEventCard(inc, staff?.phone.orEmpty(), {}, {}, onUserClick) }
        }

        Spacer(Modifier.height(14.dp))
        // ===== 5) 健康状态分布 + 服务患者 =====
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(1.dp), modifier = Modifier.weight(1f).fillMaxHeight()) {
                Column(Modifier.padding(14.dp).fillMaxHeight()) {
                    Text("健康状态分布", color = Dark, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(Modifier.height(10.dp))
                    val slices = listOf(PieSlice("正常", normal.toFloat(), Green), PieSlice("重点", abnormal.toFloat(), Red))
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        DonutChart(slices = slices, modifier = Modifier.size(88.dp), centerLabel = total.toString())
                    }
                    Spacer(Modifier.height(8.dp))
                    ChartLegend(slices = slices)
                }
            }
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(1.dp), modifier = Modifier.weight(1f).fillMaxHeight()) {
                Column(Modifier.padding(14.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("服务患者", color = Dark, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(Modifier.height(10.dp))
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        MetricRing("绑定患者", total.toString(), if (total > 0) 1f else 0f, TealDark)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("覆盖合作社区", color = Green, fontSize = 11.sp)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    val target = completeTarget
    if (target != null) {
        var treatment by remember(target.id) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { completeTarget = null },
            title = { Text("处置完成 · ${target.elderlyName}") },
            text = {
                OutlinedTextField(
                    value = treatment, onValueChange = { treatment = it },
                    label = { Text("处置措施（必填，如：现场包扎后送院观察）") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val doctor = staff
                    if (doctor == null) { completeTarget = null; return@TextButton }
                    if (treatment.isBlank()) { toast("请填写处置措施"); return@TextButton }
                    scope.launch {
                        runCatching { ServiceLocator.incidentRepository.hospitalComplete(target.id, treatment, doctor) }
                            .onSuccess { toast("已处置完成，社区侧可闭环"); completeTarget = null }
                            .onFailure { toast(it.message ?: "操作失败") }
                    }
                }) { Text("确认完成", color = Green) }
            },
            dismissButton = { TextButton(onClick = { completeTarget = null }) { Text("取消") } }
        )
    }
}

/** 紧凑社区方块：固定较小高度、社区名单行省略，社区增多时整体不会过长 */
@Composable
private fun CommunityBlock(c: CommunityCell, phase: Float, modifier: Modifier) {
    val alarm = c.activeIncidentCount > 0
    val bg = if (alarm) Red.copy(alpha = 0.16f + 0.5f * phase) else Teal.copy(alpha = 0.08f)
    val bd = if (alarm) Red.copy(alpha = phase) else Teal.copy(alpha = 0.3f)
    Box(
        modifier
            .heightIn(min = 66.dp).clip(RoundedCornerShape(12.dp)).background(bg)
            .border(1.5.dp, bd, RoundedCornerShape(12.dp)).padding(horizontal = 9.dp, vertical = 8.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AccountBalance, null, tint = if (alarm) Red else Teal, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    c.communityName, color = Dark, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false)
                )
            }
            Spacer(Modifier.height(5.dp))
            Text(
                if (alarm) "告警 ${c.activeIncidentCount}" else "运行正常",
                color = if (alarm) Red else Gray, fontSize = 11.sp,
                fontWeight = if (alarm) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun MiniEntry(modifier: Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Surface(modifier = modifier, shape = RoundedCornerShape(12.dp), color = White, onClick = onClick, shadowElevation = 1.dp) {
        Column(Modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, fontSize = 12.sp, color = Dark)
        }
    }
}

@Composable
private fun HospitalEventCard(
    inc: IncidentEntity,
    currentDoctorId: String,
    onAccept: () -> Unit,
    onComplete: () -> Unit,
    onUserClick: (String) -> Unit
) {
    val statusColor = when {
        IncidentStatus.isTerminal(inc.status) -> Green
        inc.status == IncidentStatus.URGENT || inc.status == IncidentStatus.ESCALATED -> Red
        inc.status == IncidentStatus.HOSPITAL_ACCEPTED -> Blue
        else -> Orange
    }
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
        shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Sensors, null, tint = Red, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(6.dp))
                Text("${inc.elderlyName} · ${inc.buildingNo}栋${inc.roomNo}", color = Dark, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                Surface(shape = RoundedCornerShape(8.dp), color = statusColor.copy(alpha = 0.12f)) {
                    Text(IncidentStatus.labelOf(inc.status), color = statusColor, fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }
            Spacer(Modifier.height(3.dp))
            Text("事件号 ${inc.incidentNo} · 转警 ${inc.dispatchRequestedAt?.let { fmt(it) } ?: "—"}", color = Gray, fontSize = 12.sp)
            if (inc.urgentCount > 0) Text("社区已加急 ${inc.urgentCount} 次", color = Red, fontSize = 12.sp)
            inc.hospitalDoctorId?.let { Text("处警医生账号：$it", color = Gray, fontSize = 12.sp) }
            if (inc.hospitalTreatment.isNotBlank()) Text("处置：${inc.hospitalTreatment}", color = Dark, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            val canAccept = IncidentStatus.canHospitalAccept(inc.status) && inc.hospitalDoctorId == null
            val mineAccepted = inc.status == IncidentStatus.HOSPITAL_ACCEPTED && inc.hospitalDoctorId == currentDoctorId
            when {
                canAccept -> Button(onClick = onAccept, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Red)) { Text("一键处警（先接先得）") }
                mineAccepted -> Button(onClick = onComplete, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text("处置完成") }
                inc.status == IncidentStatus.HOSPITAL_ACCEPTED -> Text("其他值班医生处置中", color = Gray, fontSize = 12.sp)
                IncidentStatus.isTerminal(inc.status) -> Text("已完成归档", color = Green, fontSize = 12.sp)
                else -> Text("等待接单…", color = Gray, fontSize = 12.sp)
            }
        }
    }
}

private val f = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())
private fun fmt(ts: Long) = f.format(Date(ts))
