package com.elderlycare.app.ui.community

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Sensors
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
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.incident.IncidentEntity
import com.elderlycare.app.data.incident.IncidentStatus
import com.elderlycare.app.data.model.AppUser
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Bg = Color(0xFFF2F6FC)
private val White = Color.White
private val Dark = Color(0xFF16243A)
private val Gray = Color(0xFF5E6B80)
private val Blue = Color(0xFF3F8FE0)
private val Green = Color(0xFF52C41A)
private val Orange = Color(0xFFFA8C16)
private val Red = Color(0xFFEA4E4E)

/**
 * 社区事件处置中心：当前网格员名下跌倒事件的全流程操作。
 * 联系家属（含预留电话拨打）→ 评估后【紧急出警】转医院 或【自行闭环】；
 * 转警后实时显示医院状态；医院处置完成后【社区闭环】解锁；每步写服务记录。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityIncidentCenterScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var staff by remember { mutableStateOf<AppUser?>(null) }
    LaunchedEffect(Unit) { staff = ServiceLocator.staffUserStore.getCurrentStaffUser() }
    val phone = staff?.phone ?: ""

    val incidents by remember(phone) {
        if (phone.isBlank()) flowOf(emptyList())
        else ServiceLocator.incidentRepository.observeByCommunityStaff(phone)
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    val active = incidents.filter { !IncidentStatus.isTerminal(it.status) }
    val done = incidents.filter { IncidentStatus.isTerminal(it.status) }

    var actionTarget by remember { mutableStateOf<IncidentEntity?>(null) }
    var actionMode by remember { mutableStateOf("") } // contact / selfClose / communityClose

    fun toast(m: String) = Toast.makeText(context, m, Toast.LENGTH_SHORT).show()
    fun dial(p: String) = runCatching {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$p")))
    }

    Scaffold(
        containerColor = Bg,
        topBar = {
            TopAppBar(
                title = { Text("事件处置中心", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Filled.ArrowBack, "返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = White, titleContentColor = Dark, navigationIconContentColor = Dark)
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            if (incidents.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
                    Text("暂无事件，家属端「模拟跌倒」后此处将自动生成待办", color = Gray, fontSize = 13.sp)
                }
            }
            active.forEach { IncidentCard(it, ::dial, onContact = { actionTarget = it; actionMode = "contact" },
                onDispatch = { t ->
                    scope.launch {
                        runCatching { ServiceLocator.incidentRepository.requestDispatch(t.id) }
                            .onSuccess { toast("已向合作医院发起紧急出警") }
                            .onFailure { e -> toast(e.message ?: "出警失败") }
                    }
                },
                onSelfClose = { actionTarget = it; actionMode = "selfClose" },
                onCommunityClose = { actionTarget = it; actionMode = "communityClose" }) }
            if (done.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("已闭环（${done.size}）", color = Gray, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                done.forEach { IncidentCard(it, ::dial, {}, {}, {}, {}) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    // 通用备注弹窗
    val target = actionTarget
    if (target != null && actionMode.isNotBlank()) {
        var note by remember(target.id) { mutableStateOf("") }
        val title = when (actionMode) {
            "contact" -> "联系家属记录"
            "selfClose" -> "社区自行闭环"
            else -> "社区闭环（医院已处置完成）"
        }
        AlertDialog(
            onDismissRequest = { actionTarget = null; actionMode = "" },
            title = { Text(title) },
            text = {
                Column {
                    Text("${target.elderlyName} · ${target.buildingNo}栋${target.roomNo} · 家属预留电话 ${target.elderlyId}", color = Gray, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = note, onValueChange = { note = it },
                        label = { Text(if (actionMode == "contact") "沟通情况（可选）" else "处置/收尾说明") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        runCatching {
                            when (actionMode) {
                                "contact" -> ServiceLocator.incidentRepository.contactFamily(target.id, note)
                                "selfClose" -> ServiceLocator.incidentRepository.selfClose(target.id, note)
                                "communityClose" -> ServiceLocator.incidentRepository.communityComplete(target.id, note)
                            }
                        }.onSuccess {
                            toast(if (actionMode == "contact") "已记录联系家属" else "已完成并写入服务记录")
                        }.onFailure { toast(it.message ?: "操作失败") }
                        actionTarget = null; actionMode = ""
                    }
                }) { Text("确认", color = Blue) }
            },
            dismissButton = { TextButton(onClick = { actionTarget = null; actionMode = "" }) { Text("取消") } }
        )
    }
}

@Composable
private fun IncidentCard(
    inc: IncidentEntity,
    dial: (String) -> Unit,
    onContact: () -> Unit,
    onDispatch: (IncidentEntity) -> Unit,
    onSelfClose: () -> Unit,
    onCommunityClose: () -> Unit
) {
    val statusColor = when {
        IncidentStatus.isTerminal(inc.status) -> Green
        inc.status == IncidentStatus.URGENT || inc.status == IncidentStatus.ESCALATED -> Red
        inc.status == IncidentStatus.DISPATCH_REQUESTED -> Orange
        inc.status == IncidentStatus.HOSPITAL_ACCEPTED || inc.status == IncidentStatus.HOSPITAL_DONE -> Blue
        else -> Blue
    }
    Card(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Sensors, null, tint = Red, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("${inc.elderlyName} · ${inc.buildingNo}栋${inc.roomNo}", color = Dark, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                Surface(shape = RoundedCornerShape(8.dp), color = statusColor.copy(alpha = 0.12f)) {
                    Text(IncidentStatus.labelOf(inc.status), color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("事件号 ${inc.incidentNo} · 触发 ${fmt(inc.triggeredAt)}", color = Gray, fontSize = 12.sp)
            if (inc.urgentCount > 0) Text("已加急 ${inc.urgentCount} 次", color = Red, fontSize = 12.sp)
            inc.hospitalDoctorId?.let { Text("处警医生账号：$it", color = Gray, fontSize = 12.sp) }
            if (inc.status == IncidentStatus.ESCALATED_UNANSWERED)
                Text("医院暂无人响应，已升级，请电话联系医院！", color = Red, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

            Spacer(Modifier.height(10.dp))
            // 按钮门禁
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { dial(inc.elderlyId) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Call, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("家属电话")
                }
                if (IncidentStatus.canSelfClose(inc.status) || inc.status == IncidentStatus.RAISED) {
                    OutlinedButton(onClick = onContact, modifier = Modifier.weight(1f)) { Text("联系家属") }
                }
            }
            Spacer(Modifier.height(8.dp))
            when {
                IncidentStatus.canDispatch(inc.status) -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onDispatch(inc) }, modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Red)) {
                        Icon(Icons.Filled.LocalHospital, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("紧急出警")
                    }
                    OutlinedButton(onClick = onSelfClose, modifier = Modifier.weight(1f)) { Text("自行闭环") }
                }
                IncidentStatus.canCommunityClose(inc.status) -> Button(
                    onClick = onCommunityClose, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Green)
                ) { Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("医院已处置，社区闭环") }
                IncidentStatus.isTerminal(inc.status) -> Text("已完成，服务记录已归档", color = Green, fontSize = 12.sp)
                else -> Text("等待医院侧响应/处置…", color = Gray, fontSize = 12.sp)
            }
        }
    }
}

private val cardFmt = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())
private fun fmt(ts: Long): String = cardFmt.format(Date(ts))
