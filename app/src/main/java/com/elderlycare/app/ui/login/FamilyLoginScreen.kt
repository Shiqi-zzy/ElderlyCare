package com.elderlycare.app.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.model.FamilyUser
import com.elderlycare.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyLoginScreen(
    onNavigateToWizard: () -> Unit,
    onNavigateToMain: () -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isRegister by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    fun submit() {
        if (loading) return
        val p = phone.trim()
        val pwd = password.trim()
        error = when {
            p.isBlank() -> "请输入手机号"
            pwd.isBlank() -> "请输入密码"
            isRegister && name.trim().isBlank() -> "请输入姓名"
            else -> null
        }
        if (error != null) return

        scope.launch {
            loading = true
            error = null
            val userStore = ServiceLocator.userStore
            val profileStore = ServiceLocator.profileStore
            if (isRegister) {
                val ok = userStore.register(FamilyUser(phone = p, name = name.trim(), password = pwd))
                if (!ok) { error = "该手机号已注册"; loading = false; return@launch }
            } else {
                val user = userStore.login(p, pwd)
                if (user == null) { error = "手机号或密码错误"; loading = false; return@launch }
            }
            userStore.setCurrentUser(p)
            val hasProfile = profileStore.getPrimaryProfile(p) != null
            loading = false
            if (hasProfile) onNavigateToMain() else onNavigateToWizard()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isRegister) "家属注册" else "家属登录", fontWeight = FontWeight.SemiBold) },
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
            Text("欢迎使用萤石养老看护", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isRegister) "首次使用请注册家属账号" else "登录后自动恢复您的看护数据",
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
            Spacer(modifier = Modifier.height(20.dp))

            if (error != null) {
                Text(error!!, color = Error, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(12.dp))
            }

            Button(
                onClick = { submit() },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
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
                Text(if (isRegister) "已有账号？去登录" else "没有账号？去注册", color = Primary)
            }
        }
    }
}
