package com.elderlycare.app.ui.hospital

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowBack
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
import com.elderlycare.app.data.binding.HcBindingStatus
import com.elderlycare.app.data.binding.HospitalCommunityBindingEntity
import com.elderlycare.app.data.binding.OrganizationEntity
import com.elderlycare.app.data.binding.OrganizationType
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.model.AppUser
import kotlinx.coroutines.launch

private val Bg = Color(0xFFF2F6FC)
private val White = Color.White
private val Dark = Color(0xFF16243A)
private val Gray = Color(0xFF5E6B80)
private val Teal = Color(0xFF2FB5AC)
private val Green = Color(0xFF52C41A)
private val Orange = Color(0xFFFA8C16)
private val Red = Color(0xFFEA4E4E)

/**
 * 医院前台 → 申请绑定社区（多对多，管理端审批）。
 * ACTIVE 后医院大屏自动出现该社区、可见其脱敏老人并接收急救告警。
 * 本演示不另做独立管理端，PENDING 项提供「模拟管理端通过/驳回」按钮虚拟审批环节。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HospitalCommunityBindingScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var staff by remember { mutableStateOf<AppUser?>(null) }
    var communities by remember { mutableStateOf<List<OrganizationEntity>>(emptyList()) }
    LaunchedEffect(Unit) {
        staff = ServiceLocator.staffUserStore.getCurrentStaffUser()
        communities = ServiceLocator.bindingDao.getAllOrganizations()
            .filter { it.type == OrganizationType.COMMUNITY.name }
    }
    val hospitalOrgId = staff?.organizationId ?: "org_hospital_01"
    val bindings by remember(hospitalOrgId) {
        ServiceLocator.incidentRepository.observeHcBindings(hospitalOrgId)
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    fun toast(m: String) = Toast.makeText(context, m, Toast.LENGTH_SHORT).show()
    fun statusOf(communityId: String): HospitalCommunityBindingEntity? =
        bindings.firstOrNull { it.communityOrgId == communityId }

    Scaffold(
        containerColor = Bg,
        topBar = {
            TopAppBar(
                title = { Text("医院-社区绑定", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Filled.ArrowBack, "返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = White, titleContentColor = Dark, navigationIconContentColor = Dark)
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text("医院不再直接绑定老人，而是绑定社区；审批通过后自动获得该社区全部老人的脱敏视图。",
                color = Gray, fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            if (communities.isEmpty()) Text("暂无社区机构", color = Gray)
            communities.forEach { org ->
                val b = statusOf(org.id)
                Card(
                    Modifier.fillMaxWidth().padding(vertical = 5.dp), shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = White)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AccountBalance, null, tint = Teal, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(org.name, color = Dark, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                            when (b?.status) {
                                HcBindingStatus.ACTIVE.name -> StatusTag("已绑定", Green)
                                HcBindingStatus.PENDING.name -> StatusTag("待审批", Orange)
                                HcBindingStatus.REJECTED.name -> StatusTag("已驳回", Red)
                                HcBindingStatus.REVOKED.name -> StatusTag("已解除", Gray)
                                else -> StatusTag("未绑定", Gray)
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("覆盖：${org.serviceArea.ifBlank { "—" }} · ${org.address.ifBlank { "—" }}", color = Gray, fontSize = 12.sp)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            when (b?.status) {
                                null, HcBindingStatus.REJECTED.name, HcBindingStatus.REVOKED.name -> Button(
                                    onClick = {
                                        scope.launch {
                                            runCatching {
                                                ServiceLocator.incidentRepository.applyHospitalCommunity(
                                                    hospitalOrgId, org.id, "申请为${org.name}提供急救与健康服务"
                                                )
                                            }.onSuccess { toast("已提交绑定申请，等待管理端审批") }
                                                .onFailure { toast(it.message ?: "申请失败") }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Teal)
                                ) { Text("申请绑定") }
                                HcBindingStatus.PENDING.name -> {
                                    OutlinedButton(onClick = {
                                        scope.launch {
                                            ServiceLocator.incidentRepository.reviewHcBinding(b.id, true, staff?.phone ?: "admin", "演示：管理端审批通过")
                                            toast("管理端已审批通过")
                                        }
                                    }) { Text("模拟审批通过", color = Green) }
                                    OutlinedButton(onClick = {
                                        scope.launch {
                                            ServiceLocator.incidentRepository.reviewHcBinding(b.id, false, staff?.phone ?: "admin", "演示：管理端驳回")
                                        }
                                    }) { Text("驳回", color = Red) }
                                }
                                HcBindingStatus.ACTIVE.name -> Text("已可接收该社区急救告警", color = Green, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatusTag(text: String, c: Color) {
    Surface(shape = RoundedCornerShape(8.dp), color = c.copy(alpha = 0.12f)) {
        Text(text, color = c, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}
