package com.elderlycare.app.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elderlycare.app.data.binding.OrganizationEntity
import com.elderlycare.app.data.binding.SeedData
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.model.AppUser
import com.elderlycare.app.data.model.QualificationStatus
import com.elderlycare.app.data.model.UserRole
import com.elderlycare.app.ui.theme.*
import kotlinx.coroutines.launch

/**
 * 社区/医院工作人员统一认证页（登录 + 注册切换，风格对齐家属端 FamilyLoginScreen）。
 *
 * - 登录：手机号 + 密码，按 [role] 限定身份入口（社区账号不能从医院入口登录）。
 * - 注册：姓名 + 手机号 + 密码 + 确认密码 + 机构选择（该端 Seed 机构），role 由 [role] 自动写入。
 * - 注册成功后自动登录（与家属端「注册并进入」一致，会话保持正确）。
 *
 * 登录/注册均写入 `UserStore`（staff_data DataStore），与家属账号体系完全隔离。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun StaffAuthScreen(
    role: UserRole,
    endName: String,
    accent: Color,
    onLoginSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isRegister by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    // 机构选择（注册用）：读取该端 Room Seed 机构；为空时回退到已知默认机构，保证可选
    val orgOptions = remember { mutableStateListOf<OrganizationEntity>() }
    var selectedOrgId by remember { mutableStateOf<String?>(null) }
    var orgMenuExpanded by remember { mutableStateOf(false) }
    // 机构搜索框文本（可输入，按相似度实时过滤下拉）
    var orgQuery by remember { mutableStateOf("") }
    // 社区网格员负责楼栋多选（固定 1-8 栋，一人可多栋、暂不共管）
    val selectedBuildings = remember { mutableStateListOf<Int>() }
    LaunchedEffect(role) {
        val defaultOrg = OrganizationEntity(
            id = if (role == UserRole.COMMUNITY) SeedData.COMMUNITY_ORG_ID else SeedData.HOSPITAL_ORG_ID,
            name = if (role == UserRole.COMMUNITY) "幸福社区照护驿站" else "新华社区医院",
            type = role.name,
            createdAt = 0L
        )
        val dbOrgs = ServiceLocator.bindingDao.getAllOrganizations().filter { it.type == role.name }
        val all = if (dbOrgs.any { it.id == defaultOrg.id }) dbOrgs else listOf(defaultOrg) + dbOrgs
        orgOptions.clear()
        orgOptions.addAll(all)
        if (selectedOrgId == null || all.none { it.id == selectedOrgId }) {
            selectedOrgId = all.first().id
            orgQuery = all.first().name
        }
    }
    // 输入框文本实时模糊匹配（空串显示全部；名称包含关键字即命中，忽略大小写）
    val filteredOrgs = orgOptions.filter {
        orgQuery.isBlank() || it.name.contains(orgQuery.trim(), ignoreCase = true)
    }

    fun submit() {
        if (loading) return
        val p = phone.trim()
        val pwd = password.trim()
        val nm = name.trim()
        error = when {
            p.isBlank() -> "请输入手机号"
            pwd.isBlank() -> "请输入密码"
            isRegister && nm.isBlank() -> "请输入姓名"
            isRegister && pwd != confirmPassword.trim() -> "两次输入的密码不一致"
            isRegister && selectedOrgId.isNullOrBlank() -> "请选择所属机构"
            isRegister && role == UserRole.COMMUNITY && selectedBuildings.isEmpty() -> "请至少选择一栋负责楼栋"
            else -> null
        }
        if (error != null) return

        scope.launch {
            loading = true
            error = null
            val store = ServiceLocator.staffUserStore
            if (isRegister) {
                val ok = store.register(
                    AppUser(
                        phone = p,
                        name = nm,
                        password = pwd,
                        role = role,
                        organizationId = selectedOrgId,
                        // 新注册默认工作资格「审核中」，需审核通过后才能使用业务功能
                        qualification = QualificationStatus.PENDING.name,
                        areaBuildings = if (role == UserRole.COMMUNITY) selectedBuildings.sorted().map { it.toString() } else emptyList(),
                        createdAt = System.currentTimeMillis()
                    )
                )
                if (!ok) { error = "该手机号已注册"; loading = false; return@launch }
                // 注册成功后自动登录（与家属端「注册并进入」一致）
                store.setCurrentStaff(p)
            } else {
                // 分步校验，给出明确错误
                val existing = store.getStaffByPhone(p)
                when {
                    existing == null -> { error = "手机号不存在"; loading = false; return@launch }
                    existing.password != pwd -> { error = "密码错误"; loading = false; return@launch }
                    existing.role != role -> { error = "账号角色不匹配，请从正确的身份入口登录"; loading = false; return@launch }
                }
                store.setCurrentStaff(p)
            }
            loading = false
            onLoginSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isRegister) "${endName}注册" else "${endName}登录", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            Text("欢迎使用银龄心语", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isRegister) "首次使用请注册${endName}账号" else "登录后自动恢复您的授权看护数据",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(28.dp))

            if (isRegister) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("姓名") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("手机号") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )
            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("密码") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            Spacer(modifier = Modifier.height(14.dp))

            if (isRegister) {
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("确认密码") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                Spacer(modifier = Modifier.height(14.dp))

                // 机构选择
                ExposedDropdownMenuBox(
                    expanded = orgMenuExpanded,
                    onExpandedChange = { orgMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = orgQuery,
                        onValueChange = { orgQuery = it; selectedOrgId = null; orgMenuExpanded = true },
                        // 可直接输入机构名称进行模糊搜索
                        singleLine = true,
                        label = { Text("所属机构（可输入关键字搜索）") },
                        trailingIcon = { Icon(Icons.Filled.ArrowDropDown, null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = orgMenuExpanded,
                        onDismissRequest = { orgMenuExpanded = false }
                    ) {
                        filteredOrgs.forEach { org ->
                            DropdownMenuItem(
                                text = { Text(org.name) },
                                onClick = {
                                    selectedOrgId = org.id
                                    orgQuery = org.name
                                    orgMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                if (role == UserRole.COMMUNITY) {
                    Text("负责楼栋（固定 1-8 栋，可多选；同一栋暂不共管）", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..8).forEach { b ->
                            FilterChip(
                                selected = selectedBuildings.contains(b),
                                onClick = {
                                    if (selectedBuildings.contains(b)) selectedBuildings.remove(b) else selectedBuildings.add(b)
                                },
                                label = { Text("${b}栋") }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            if (error != null) {
                Text(error!!, color = Error, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(12.dp))
            }

            Button(
                onClick = { submit() },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = OnPrimary, strokeWidth = 2.dp)
                } else {
                    Text(if (isRegister) "注册并进入" else "登录", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = { isRegister = !isRegister; error = null },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(if (isRegister) "已有账号？去登录" else "没有账号？去注册", color = accent)
            }
        }
    }
}
