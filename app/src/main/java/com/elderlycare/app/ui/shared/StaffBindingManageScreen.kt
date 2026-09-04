package com.elderlycare.app.ui.shared

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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

/** 绑定管理页色值（薄荷绿主题） */
private val PageBg = Color(0xFFF5FAF7)
private val MintGreen = Color(0xFF4CAF8A)
private val MintGreenLight = Color(0xFF6BC9A8)
private val CardWhite = Color.White
private val TextDark = Color(0xFF1A2E25)
private val TextGray = Color(0xFF6B7C74)
private val TextHint = Color(0xFF9AA8A2)

/** 三个 Tab */
private enum class BindingTab(val label: String, val icon: ImageVector) {
    APPLY("绑定申请", Icons.Filled.GroupAdd),
    REQUESTS("我的申请", Icons.Filled.Description),
    BOUND("已绑定用户", Icons.Filled.People)
}

/**
 * 绑定管理页：三 Tab 布局（绑定申请 / 我的申请 / 已绑定用户）。
 * 从「我的」→「我的服务」→「绑定用户」进入。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffBindingManageScreen(
    role: UserRole,
    onNavigateBack: () -> Unit
) {
    val elderWord = if (role == UserRole.HOSPITAL) "长者" else "服务对象"
    var selectedTab by remember { mutableStateOf(BindingTab.APPLY) }
    var currentStaff by remember { mutableStateOf<AppUser?>(null) }

    LaunchedEffect(Unit) {
        currentStaff = ServiceLocator.staffUserStore.getCurrentStaffUser()
    }

    val staffPhone = currentStaff?.phone ?: ""

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg)
    ) {
        // ===== 顶部栏 =====
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
                Text("银龄心语", color = TextDark, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("绑定管理", color = TextGray, fontSize = 10.sp)
            }
        }

        // ===== Tab 栏 =====
        TabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = Color.Transparent,
            contentColor = MintGreen,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier
                        .tabIndicatorOffset(tabPositions[selectedTab.ordinal])
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)),
                    height = 3.dp,
                    color = MintGreen
                )
            },
            divider = { HorizontalDivider(color = DividerColor, thickness = 0.5.dp) }
        ) {
            BindingTab.entries.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    selectedContentColor = MintGreen,
                    unselectedContentColor = TextGray
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 10.dp)
                    ) {
                        Icon(tab.icon, null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(tab.label, fontSize = 12.sp, fontWeight = if (selectedTab == tab) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ===== Tab 内容 =====
        when (selectedTab) {
            BindingTab.APPLY -> ApplyTabContent(role = role, onSubmitted = { selectedTab = BindingTab.REQUESTS })
            BindingTab.REQUESTS -> {
                if (staffPhone.isNotBlank()) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        MyRequestsPanel(staffPhone)
                    }
                }
            }
            BindingTab.BOUND -> {
                if (staffPhone.isNotBlank()) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        BoundUsersPanel(staffPhone, elderWord)
                    }
                }
            }
        }
    }
}

/**
 * 绑定申请 Tab 内容：选择老人 → 查看信息 → 填写说明 → 提交。
 * 提交成功后自动切换到「我的申请」Tab。
 */
@Composable
private fun ApplyTabContent(role: UserRole, onSubmitted: () -> Unit) {
    val elderWord = if (role == UserRole.HOSPITAL) "长者" else "服务对象"
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
            var available = ServiceLocator.bindingRepository.getAvailableElderly(staff)
            // 社区网格员只能申请绑定自己负责楼栋的老人；医院前台走医院-社区绑定，不受楼栋限制
            if (staff.role == UserRole.COMMUNITY && staff.areaBuildings.isNotEmpty()) {
                available = available.filter { it.buildingNo in staff.areaBuildings }
            }
            elderlyList = available
        }
    }

    val selectedProfile = elderlyList.firstOrNull { it.userId == selectedId }

    if (currentStaff == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("未登录，无法发起申请", color = Error, style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 选择老人
        item {
            Column {
                Text("选择老人", color = TextDark, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text("可申请绑定的${elderWord}列表", color = TextGray, fontSize = 11.sp)
            }
        }
        if (elderlyList.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Inbox, null, tint = TextHint, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("暂无可申请绑定的$elderWord", color = TextHint, fontSize = 13.sp)
                        }
                    }
                }
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
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(CircleShape).background(MintGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(profile.name.take(1), color = MintGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(profile.name, fontWeight = FontWeight.SemiBold, color = TextDark, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("${profile.age}岁 · ${profile.gender.label}", color = TextGray, fontSize = 12.sp)
                    }
                    if (selected) {
                        Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(MintGreen), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // 老人信息
        val p = selectedProfile
        if (p != null) {
            item { Spacer(modifier = Modifier.height(4.dp)) }
            item {
                Text("老人信息", color = TextDark, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
            item {
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = CardWhite), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        InfoRow("姓名", p.name)
                        InfoRow("性别", p.gender.label)
                        InfoRow("年龄", p.age)
                        InfoRow("联系电话", p.phone.ifBlank { "未填写" })
                        InfoRow("身高", p.height.ifBlank { "未填写" })
                        InfoRow("体重", p.weight.ifBlank { "未填写" })
                    }
                }
            }
            item {
                Text("设备信息", color = TextDark, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
            item {
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = CardWhite), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        InfoRow("设备SN", p.deviceSn.ifBlank { "未绑定" })
                        InfoRow("绑定状态", if (p.deviceBound) "已绑定" else "未绑定")
                    }
                }
            }
        }

        // 申请说明
        item { Spacer(modifier = Modifier.height(4.dp)) }
        item {
            Column {
                Text("申请说明", color = TextDark, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text("可选，填写申请理由", color = TextGray, fontSize = 11.sp)
            }
        }
        item {
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("申请说明（可选）", color = TextGray) },
                placeholder = { Text("请简要说明申请绑定的原因...", color = TextHint) },
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
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
            item { Text(error!!, color = Error, style = MaterialTheme.typography.bodyMedium) }
        }

        // 提交按钮
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
                            onSubmitted()
                        } else {
                            error = err
                            submitting = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
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
