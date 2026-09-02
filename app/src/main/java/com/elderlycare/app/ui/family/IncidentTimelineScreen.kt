package com.elderlycare.app.ui.family

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.incident.IncidentEntity
import com.elderlycare.app.data.incident.IncidentStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Bg = Color(0xFFF5F8FD)
private val White = Color.White
private val Dark = Color(0xFF16243A)
private val Gray = Color(0xFF5E6B80)
private val Green = Color(0xFF52C41A)
private val Orange = Color(0xFFFAAD14)
private val Red = Color(0xFFEA6668)
private val Blue = Color(0xFF3F8FE0)

/**
 * 家属端「处置记录」：本老人全部跌倒/紧急事件的四端处置时间线。
 * 让家属看到：何时触发、社区何时接收/联系、何时呼叫医院、医院何时出警与完成、社区何时闭环。
 */
@Composable
fun IncidentTimelineScreen() {
    var elderlyIds by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(Unit) {
        val uid = ServiceLocator.userStore.getCurrentUserId() ?: ""
        elderlyIds = ServiceLocator.profileStore.getProfilesByUser(uid).map { it.userId }
            .ifEmpty { listOf(uid) }
    }
    val incidents by remember(elderlyIds) {
        if (elderlyIds.isEmpty()) kotlinx.coroutines.flow.flowOf(emptyList())
        else ServiceLocator.incidentRepository.observeByFamily(elderlyIds)
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    Column(Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(12.dp))
        if (incidents.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Inbox, null, tint = Gray.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("暂无事件记录", color = Gray, fontSize = 13.sp)
                    Text("可在「我的 → 模拟 RK3 跌倒告警」发起演示", color = Gray.copy(alpha = 0.8f), fontSize = 12.sp)
                }
            }
        } else {
            incidents.forEach { IncidentCard(it) }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun IncidentCard(inc: IncidentEntity) {
    val terminal = IncidentStatus.isTerminal(inc.status)
    val statusColor = when {
        terminal -> Green
        inc.status == IncidentStatus.ESCALATED_UNANSWERED -> Red
        inc.status == IncidentStatus.URGENT || inc.status == IncidentStatus.ESCALATED -> Orange
        else -> Blue
    }
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Sensors, null, tint = Red, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("跌倒事件 ${inc.incidentNo}", color = Dark, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                Surface(shape = RoundedCornerShape(8.dp), color = statusColor.copy(alpha = 0.12f)) {
                    Text(
                        IncidentStatus.labelOf(inc.status),
                        color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("${inc.elderlyName} · ${inc.buildingNo}栋${inc.unitNo.ifBlank { "" }}单元${inc.roomNo}", color = Gray, fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            TimelineNode("设备触发告警", inc.triggeredAt, Blue, true)
            TimelineNode("社区已接收", inc.communityReceivedAt, Blue)
            TimelineNode("社区已联系家属", inc.familyContactedAt, Blue)
            TimelineNode("社区呼叫医院出警", inc.dispatchRequestedAt, Orange)
            TimelineNode("医院一键处警", inc.hospitalAcceptedAt, Orange)
            TimelineNode("医院处置完成", inc.hospitalDoneAt, Orange)
            TimelineNode("社区闭环完成", inc.communityDoneAt.takeIf { it != null } ?: inc.closedAt.takeIf { terminal }, Green)
        }
    }
}

@Composable
private fun TimelineNode(label: String, ts: Long?, accent: Color, isFirst: Boolean = false) {
    val happened = ts != null
    Row(Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(16.dp).clip(CircleShape)
                    .background(if (happened) accent else Color(0xFFD7DEEA)),
                contentAlignment = Alignment.Center
            ) {
                if (happened) Icon(Icons.Filled.Check, null, tint = White, modifier = Modifier.size(11.dp))
            }
            if (!isFirst) Box(Modifier.width(2.dp).height(18.dp).background(Color(0xFFE4EAF3)))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.padding(bottom = 8.dp)) {
            Text(label, color = if (happened) Dark else Gray.copy(alpha = 0.7f), fontSize = 13.sp,
                fontWeight = if (happened) FontWeight.Medium else FontWeight.Normal)
            if (happened) Text(fmt(ts), color = Gray, fontSize = 11.sp)
        }
    }
}

private val tlFmt = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())
private fun fmt(ts: Long?): String = if (ts == null) "" else tlFmt.format(Date(ts))
