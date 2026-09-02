package com.elderlycare.app.ui.family

import android.content.Intent
import android.net.Uri
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
import com.elderlycare.app.data.binding.OrganizationEntity
import com.elderlycare.app.data.community.ServiceRecord
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.model.ElderlyProfile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val CareBg = Color(0xFFF5F8FD)
private val CareBlue = Color(0xFF3F8FE0)
private val CareTeal = Color(0xFF33B5B0)
private val White = Color.White
private val Dark = Color(0xFF16243A)
private val Gray = Color(0xFF5E6B80)

/**
 * 家属端「我的社区 / 我的医院」：机构基础信息（含预留联系电话，可一键拨打）+ 该老人对应服务记录。
 * @param side community=我的社区，hospital=我的医院
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyCareScreen(side: String) {
    val context = LocalContext.current
    val isCommunity = side == "community"
    val main = if (isCommunity) CareBlue else CareTeal

    var profile by remember { mutableStateOf<ElderlyProfile?>(null) }
    var org by remember { mutableStateOf<OrganizationEntity?>(null) }
    LaunchedEffect(side) {
        val uid = ServiceLocator.userStore.getCurrentUserId() ?: ""
        val p = ServiceLocator.profileStore.getPrimaryProfile(uid)
        profile = p
        if (p != null) {
            org = if (isCommunity) {
                ServiceLocator.bindingDao.getOrganization(p.communityId)
            } else {
                val hospitals = ServiceLocator.bindingDao.getActiveHospitalsByCommunity(p.communityId)
                hospitals.firstOrNull()?.let { ServiceLocator.bindingDao.getOrganization(it.hospitalOrgId) }
            }
        }
    }

    val elderlyId = profile?.userId ?: ""
    val records by remember(elderlyId, side) {
        if (elderlyId.isBlank()) kotlinx.coroutines.flow.flowOf(emptyList())
        else ServiceLocator.incidentRepository.observeElderlyServiceRecords(elderlyId)
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val sideRecords = records.filter { it.side == side }.sortedByDescending { it.createdAt }

    fun dial(phone: String) {
        if (phone.isBlank()) return
        runCatching {
            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
        }
    }

    Column(Modifier.fillMaxSize().background(CareBg).verticalScroll(rememberScrollState())) {
        // 信息头
        Box(
            Modifier.fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(main, main.copy(alpha = 0.7f))))
                .padding(20.dp)
        ) {
            Column {
                Text(
                    if (isCommunity) "我的社区" else "我的医院",
                    color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(org?.name ?: if (isCommunity) "尚未绑定社区" else "社区尚未绑定合作医院",
                    color = White.copy(0.95f), fontSize = 15.sp)
            }
        }

        // 基础信息卡
        Card(
            Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("基础信息", color = Dark, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(Modifier.height(10.dp))
                InfoLine(Icons.Filled.Person, "联系人", org?.contactPerson ?: "—")
                InfoLine(Icons.Filled.LocationOn, "地址", org?.address ?: "—")
                InfoLine(Icons.Filled.GridView, "服务范围", org?.serviceArea ?: "—")
                if (!org?.intro.isNullOrBlank()) InfoLine(Icons.Filled.Info, "简介", org!!.intro)
                val phone = org?.contactPhone.orEmpty()
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { dial(phone) },
                    enabled = phone.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(23.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = main)
                ) {
                    Icon(Icons.Filled.Call, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (phone.isNotBlank()) "拨打预留电话 $phone" else "暂无预留电话")
                }
            }
        }

        // 服务记录
        Card(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    if (isCommunity) "社区服务记录" else "医院服务记录",
                    color = Dark, fontWeight = FontWeight.SemiBold, fontSize = 15.sp
                )
                Spacer(Modifier.height(8.dp))
                if (sideRecords.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                        Text("暂无服务记录", color = Gray, fontSize = 13.sp)
                    }
                } else {
                    sideRecords.forEachIndexed { idx, r ->
                        RecordItem(r, main, idx == sideRecords.lastIndex)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun InfoLine(icon: ImageVector, label: String, value: String) {
    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = CareBlue, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("$label：", color = Gray, fontSize = 13.sp)
        Text(value.ifBlank { "—" }, color = Dark, fontSize = 13.sp, modifier = Modifier.weight(1f))
    }
}

private val recordFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

@Composable
private fun RecordItem(r: ServiceRecord, accent: Color, isLast: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(accent.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Assignment, null, tint = accent, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(r.serviceType.ifBlank { "服务" }, color = Dark, fontWeight = FontWeight.Medium, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text(recordFmt.format(Date(r.createdAt)), color = Gray, fontSize = 11.sp)
            }
            Spacer(Modifier.height(2.dp))
            Text("处置人：${r.staffName.ifBlank { "—" }}", color = Gray, fontSize = 12.sp)
            if (r.content.isNotBlank()) Text(r.content, color = Color(0xFF33415C), fontSize = 12.sp)
            if (r.durationMinutes > 0) Text("耗时 ${r.durationMinutes} 分钟", color = Gray, fontSize = 11.sp)
        }
    }
    if (!isLast) HorizontalDivider(color = Color(0xFFEDF1F7), thickness = 0.5.dp)
}
