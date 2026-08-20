package com.elderlycare.app.ui.shared

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.model.AppUser
import com.elderlycare.app.data.model.ElderlyProfile
import com.elderlycare.app.data.model.UserRole
import com.elderlycare.app.ui.components.InfoRow
import com.elderlycare.app.ui.theme.*
import kotlinx.coroutines.launch

/**
 * 社区/医院工作人员「发起绑定申请」共享页（两端内层路由复用）。
 * 流程：选择可申请的老人 → 查看老人信息 + 设备信息 → 填写申请说明 → 提交 → 生成 PENDING 申请。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffBindingApplyScreen(role: UserRole, onNavigateBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var currentStaff by remember { mutableStateOf<AppUser?>(null) }
    var elderlyList by remember { mutableStateOf<List<ElderlyProfile>>(emptyList()) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val staff = ServiceLocator.staffUserStore.getCurrentStaffUser()
        currentStaff = staff
        if (staff != null) {
            elderlyList = ServiceLocator.bindingRepository.getAvailableElderly(staff)
        }
    }

    val selectedProfile = elderlyList.firstOrNull { it.userId == selectedId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("发起绑定申请", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        }
    ) { paddingValues ->
        if (currentStaff == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("未登录，无法发起申请", color = Error, style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Text("选择老人", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium) }
                if (elderlyList.isEmpty()) {
                    item { Text("暂无可申请绑定的老人", style = MaterialTheme.typography.bodyMedium, color = TextHint) }
                }
                items(elderlyList, key = { it.userId }) { profile ->
                    val selected = profile.userId == selectedId
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { selectedId = profile.userId },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) Primary.copy(alpha = 0.08f) else Surface
                        ),
                        border = if (selected) BorderStroke(1.dp, Primary) else null,
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(profile.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("${profile.userId} · ${profile.age}岁", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            }
                            if (selected) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = "已选择", tint = Primary)
                            }
                        }
                    }
                }

                val p = selectedProfile
                if (p != null) {
                    item { Text("老人信息", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium) }
                    item {
                        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                InfoRow("姓名", p.name)
                                InfoRow("性别", p.gender.label)
                                InfoRow("年龄", p.age)
                                InfoRow("联系电话", p.phone)
                                InfoRow("身高", p.height)
                                InfoRow("体重", p.weight)
                            }
                        }
                    }
                    item { Text("设备信息", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium) }
                    item {
                        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                InfoRow("设备SN", p.deviceSn)
                                InfoRow("绑定状态", if (p.deviceBound) "已绑定" else "未绑定")
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = { Text("申请说明（可选）") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )
                }

                if (error != null) {
                    item { Text(error!!, color = Error, style = MaterialTheme.typography.bodyMedium) }
                }

                item {
                    Button(
                        onClick = {
                            val staff = currentStaff ?: return@Button
                            val profile = selectedProfile ?: return@Button
                            scope.launch {
                                submitting = true
                                error = null
                                val err = ServiceLocator.bindingRepository.submitBindingRequest(staff, profile, message)
                                if (err == null) {
                                    onNavigateBack()
                                } else {
                                    error = err
                                    submitting = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        enabled = selectedId != null && !submitting
                    ) {
                        if (submitting) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = OnPrimary, strokeWidth = 2.dp)
                        } else {
                            Text("提交申请", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
