package com.elderlycare.app.ui.shared

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

/** 发起绑定申请页色值（薄荷绿主题，与工作资格页统一） */
private val PageBg = Color(0xFFF5FAF7)
private val MintGreen = Color(0xFF4CAF8A)
private val MintGreenLight = Color(0xFF6BC9A8)
private val CardWhite = Color.White
private val TextDark = Color(0xFF1A2E25)
private val TextGray = Color(0xFF6B7C74)
private val TextHint = Color(0xFF9AA8A2)

/**
 * 社区/医院工作人员「发起绑定申请」共享页（薄荷绿风格重构，与工作资格页统一）。
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg)
    ) {
        // ===== 顶部 Logo 栏（带返回） =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = TextDark, modifier = Modifier.size(24.dp))
            }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(MintGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Favorite, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text("ElderlyCare", color = TextDark, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("发起绑定申请", color = TextGray, fontSize = 10.sp)
            }
        }

        // ===== 绿色渐变横幅 =====
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.horizontalGradient(listOf(MintGreen, MintGreenLight))
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("发起绑定申请", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("选择老人并提交申请，审核通过后即可访问", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.GroupAdd, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (currentStaff == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("未登录，无法发起申请", color = Error, style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ===== 选择老人 =====
                item {
                    SectionTitle("选择老人", "可申请绑定的老人列表")
                }
                if (elderlyList.isEmpty()) {
                    item {
                        EmptyCard("暂无可申请绑定的老人")
                    }
                }
                items(elderlyList, key = { it.userId }) { profile ->
                    val selected = profile.userId == selectedId
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                selectedId = if (selectedId == profile.userId) null else profile.userId
                            },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) MintGreen.copy(alpha = 0.08f) else CardWhite
                        ),
                        border = if (selected) BorderStroke(1.5.dp, MintGreen) else null,
                        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 2.dp else 1.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 头像
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MintGreen.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    profile.name.take(1),
                                    color = MintGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(profile.name, fontWeight = FontWeight.SemiBold, color = TextDark, fontSize = 15.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("${profile.age}岁 · ${profile.gender.label}", color = TextGray, fontSize = 12.sp)
                            }
                            if (selected) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(MintGreen),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                // ===== 老人信息 =====
                val p = selectedProfile
                if (p != null) {
                    item { Spacer(modifier = Modifier.height(4.dp)) }
                    item {
                        SectionTitle("老人信息", "已选择老人的详细资料")
                    }
                    item {
                        InfoCard {
                            InfoRow("姓名", p.name)
                            InfoRow("性别", p.gender.label)
                            InfoRow("年龄", p.age)
                            InfoRow("联系电话", p.phone.ifBlank { "未填写" })
                            InfoRow("身高", p.height.ifBlank { "未填写" })
                            InfoRow("体重", p.weight.ifBlank { "未填写" })
                        }
                    }

                    // ===== 设备信息 =====
                    item {
                        SectionTitle("设备信息", "绑定的看护设备")
                    }
                    item {
                        InfoCard {
                            InfoRow("设备SN", p.deviceSn.ifBlank { "未绑定" })
                            InfoRow("绑定状态", if (p.deviceBound) "已绑定" else "未绑定")
                        }
                    }
                }

                // ===== 申请说明 =====
                item { Spacer(modifier = Modifier.height(4.dp)) }
                item {
                    SectionTitle("申请说明", "可选，填写申请理由")
                }
                item {
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = { Text("申请说明（可选）", color = TextGray) },
                        placeholder = { Text("请简要说明申请绑定的原因...", color = TextHint) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        minLines = 2,
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MintGreen,
                            unfocusedBorderColor = DividerColor,
                            cursorColor = MintGreen
                        )
                    )
                }

                if (error != null) {
                    item {
                        Text(error!!, color = Error, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                // ===== 提交按钮 =====
                item {
                    Spacer(modifier = Modifier.height(8.dp))
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MintGreen,
                            contentColor = Color.White,
                            disabledContainerColor = MintGreen.copy(alpha = 0.4f)
                        ),
                        enabled = selectedId != null && !submitting
                    ) {
                        if (submitting) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("提交中...", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        } else {
                            Icon(Icons.Filled.Send, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("提交申请", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

/** 区块标题：主标题 + 副标题 */
@Composable
private fun SectionTitle(title: String, subtitle: String = "") {
    Column {
        Text(title, color = TextDark, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        if (subtitle.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, color = TextGray, fontSize = 11.sp)
        }
    }
}

/** 信息卡片：白色圆角卡片 + 内边距 */
@Composable
private fun InfoCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            content()
        }
    }
}

/** 空状态卡片 */
@Composable
private fun EmptyCard(text: String) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.Inbox, null, tint = TextHint, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(text, color = TextHint, fontSize = 13.sp)
            }
        }
    }
}
