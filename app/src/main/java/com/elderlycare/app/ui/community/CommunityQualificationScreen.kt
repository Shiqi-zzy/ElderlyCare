package com.elderlycare.app.ui.community

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.model.AppUser
import com.elderlycare.app.data.model.QualificationStatus
import com.elderlycare.app.ui.components.InfoRow
import com.elderlycare.app.ui.components.StatusBadge
import com.elderlycare.app.ui.shared.BoundUsersPanel
import com.elderlycare.app.ui.shared.MyRequestsPanel
import com.elderlycare.app.ui.theme.*
import kotlinx.coroutines.launch

/**
 * 社区端「资质管理」：个人信息/工作证/授权函（演示资质卡保留），
 * 接入真实绑定业务：发起绑定申请 + 我的申请 + 已绑定用户（可解除）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityQualificationScreen(onApplyBinding: () -> Unit = {}) {
    var staff by remember { mutableStateOf<AppUser?>(null) }
    var orgName by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        val s = ServiceLocator.staffUserStore.getCurrentStaffUser()
        staff = s
        orgName = s?.organizationId?.let { ServiceLocator.bindingDao.getOrganization(it)?.name } ?: ""
    }
    val currentStaff = staff
    val staffPhone = currentStaff?.phone ?: ""
    // 工作资格状态（旧账号 null → 已通过）
    val qualStatus = runCatching {
        QualificationStatus.valueOf(currentStaff?.qualification ?: QualificationStatus.APPROVED.name)
    }.getOrDefault(QualificationStatus.APPROVED)

    fun demoSetQualification(status: QualificationStatus) {
        val s = staff ?: return
        scope.launch {
            ServiceLocator.staffUserStore.updateUser(s.copy(qualification = status.name))
            staff = ServiceLocator.staffUserStore.getCurrentStaffUser()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("资质管理", fontWeight = FontWeight.SemiBold) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)) }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("个人信息", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoRow("姓名", currentStaff?.name ?: "-")
                    InfoRow("身份证号", "110***********1234")
                    InfoRow("所属机构", orgName.ifEmpty { "未关联机构" })
                }
            }
            // 工作资格（真实状态 + 演示审核）
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("工作资格", fontWeight = FontWeight.SemiBold)
                        when (qualStatus) {
                            QualificationStatus.APPROVED -> StatusBadge(text = "已通过", color = StatusGreen)
                            QualificationStatus.PENDING -> StatusBadge(text = "审核中", color = StatusYellow)
                            QualificationStatus.REJECTED -> StatusBadge(text = "已驳回", color = StatusRed)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        when (qualStatus) {
                            QualificationStatus.APPROVED -> "已通过审核，可正常访问业务功能"
                            QualificationStatus.PENDING -> "审核通过后才能使用业务功能（演示环境可在下方模拟审核）"
                            QualificationStatus.REJECTED -> "审核未通过，业务功能暂不可用"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    if (qualStatus == QualificationStatus.PENDING) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { demoSetQualification(QualificationStatus.APPROVED) }, shape = RoundedCornerShape(16.dp)) { Text("模拟审核通过（演示）") }
                            OutlinedButton(onClick = { demoSetQualification(QualificationStatus.REJECTED) }, shape = RoundedCornerShape(16.dp)) { Text("模拟驳回（演示）") }
                        }
                    }
                }
            }
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("网格员工作证", fontWeight = FontWeight.SemiBold); StatusBadge(text = "已认证", color = StatusGreen) }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("有效期至 2024-12-31 (剩余148天)", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = {}, shape = RoundedCornerShape(16.dp), modifier = Modifier.align(Alignment.End)) { Text("续期申请") }
                }
            }
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("社区养老服务授权函", fontWeight = FontWeight.SemiBold); StatusBadge(text = "审核中", color = StatusYellow) }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("提交日期 2024-07-01 · 预计3个工作日内完成", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onApplyBinding, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = ButtonDefaults.buttonColors(containerColor = Secondary)) { Text("发起新绑定申请") }
            if (staffPhone.isNotBlank()) {
                MyRequestsPanel(staffPhone)
                BoundUsersPanel(staffPhone)
            }
        }
    }
}
