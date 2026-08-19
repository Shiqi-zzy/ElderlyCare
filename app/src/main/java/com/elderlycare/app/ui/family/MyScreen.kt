package com.elderlycare.app.ui.family

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elderlycare.app.data.binding.BindingRepository
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.model.FamilyUser
import com.elderlycare.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyScreen(
    onNavigateToProfile: () -> Unit,
    onNavigateToBindingRequest: () -> Unit,
    onNavigateToAuthorizationMgmt: () -> Unit,
    onLogout: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var user by remember { mutableStateOf<FamilyUser?>(null) }
    LaunchedEffect(Unit) {
        user = ServiceLocator.userStore.getCurrentUser()
    }
    val currentUser = user
    // 已绑定设备（响应式）：读 BindingRepository 授权链路（档案 deviceSn），不读 DeviceBindingStore 缓存
    var boundDevice by remember { mutableStateOf<BindingRepository.AccessibleDevice?>(null) }
    LaunchedEffect(Unit) {
        ServiceLocator.bindingRepository.observeCurrentUserDevice().collect { boundDevice = it }
    }

    var showInfoDialog by remember { mutableStateOf(false) }
    var showDeviceDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 头部：头像 + 姓名 + 手机号
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = Primary.copy(alpha = 0.1f), modifier = Modifier.size(64.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                (currentUser?.name ?: "家").take(1),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(currentUser?.name ?: "未登录", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(currentUser?.phone ?: "", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }
            }

            // 账号与个人信息
            SectionCard {
                MenuItemRow(icon = Icons.Filled.Person, label = "个人信息") { showInfoDialog = true }
            }

            // 老人管理
            SectionCard {
                MenuItemRow(icon = Icons.Filled.Folder, label = "老人档案", onClick = onNavigateToProfile)
                MenuItemRow(icon = Icons.Filled.Devices, label = "设备管理") { showDeviceDialog = true }
            }

            // 其他
            SectionCard {
                MenuItemRow(icon = Icons.Filled.Link, label = "绑定申请", onClick = onNavigateToBindingRequest)
                MenuItemRow(icon = Icons.Filled.Shield, label = "授权管理", onClick = onNavigateToAuthorizationMgmt)
                MenuItemRow(icon = Icons.Filled.Info, label = "关于我们") { showAboutDialog = true }
            }

            // 退出登录
            Button(
                onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Error)
            ) {
                Text("退出登录", color = OnPrimary, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (showInfoDialog) {
        EditInfoDialog(
            user = currentUser,
            onDismiss = { showInfoDialog = false },
            onSave = { name, contact ->
                scope.launch {
                    currentUser?.let {
                        ServiceLocator.userStore.updateUser(it.copy(name = name, contact = contact))
                        user = ServiceLocator.userStore.getCurrentUser()
                    }
                }
                showInfoDialog = false
            }
        )
    }

    if (showDeviceDialog) {
        AlertDialog(
            onDismissRequest = { showDeviceDialog = false },
            title = { Text("设备管理") },
            text = {
                Text(boundDevice?.let { "已绑定设备：${it.deviceSn}" } ?: "尚未绑定 RK3 设备")
            },
            confirmButton = { TextButton(onClick = { showDeviceDialog = false }) { Text("知道了") } }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("关于我们") },
            text = { Text("萤石养老看护 · ElderlyCare\n面向家属的远程看护助手") },
            confirmButton = { TextButton(onClick = { showAboutDialog = false }) { Text("知道了") } }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("退出登录") },
            text = { Text("确定退出登录吗？\n（用户与老人档案数据会保留）") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    scope.launch {
                        ServiceLocator.userStore.clearCurrentUser()
                        // 清跨账号兼容缓存：登出后 deviceBindingStore 残留会使下一位登录家属
                        // 首页/设备弹窗显示上一账号设备、留言发送到上一账号设备（权限闸门不受影响）。
                        ServiceLocator.deviceBindingStore.clear()
                        onLogout()
                    }
                }) { Text("退出登录", color = Error) }
            },
            dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), content = content)
    }
}

@Composable
private fun MenuItemRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Primary, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Icon(Icons.Filled.KeyboardArrowRight, null, tint = TextHint)
    }
}

@Composable
private fun EditInfoDialog(user: FamilyUser?, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by remember { mutableStateOf(user?.name ?: "") }
    var contact by remember { mutableStateOf(user?.contact ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("个人信息") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("姓名") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = contact, onValueChange = { contact = it }, label = { Text("联系方式") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name, contact) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
