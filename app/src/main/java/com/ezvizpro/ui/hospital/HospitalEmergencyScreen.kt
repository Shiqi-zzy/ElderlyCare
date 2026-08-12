package com.ezvizpro.ui.hospital

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private val DarkBg = Color(0xFF0F172A)
private val CardBg = Color(0xFF1E293B)
private val TextPrimary = Color(0xFFE2E8F0)
private val TextSecondary = Color(0xFF94A3B8)
private val Accent = Color(0xFF3B82F6)
private val HospitalRed = Color(0xFFE11D48)
private val Success = Color(0xFF22C55E)
private val Warning = Color(0xFFF59E0B)

/**
 * 医院急救临时监控权限页面
 *
 * 流程：选择老人 → 发起急救权限请求 → 家属审批 → 获取有时效的直播流地址
 * App 获取 stream URL 后跳转 LivePreviewScreen 播放
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HospitalEmergencyScreen(
    token: String,
    onPlayLive: (deviceSerial: String, channelNo: Int) -> Unit = { _, _ -> },
    onBack: () -> Unit,
    viewModel: HospitalViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showRequestDialog by remember { mutableStateOf(false) }
    var selectedElderlyId by remember { mutableStateOf("") }
    var selectedElderlyName by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(token) { viewModel.setToken(token) }

    LaunchedEffect(state.toastMessage) {
        state.toastMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearToast() }
    }

    Scaffold(
        containerColor = DarkBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("🚨 急救临时权限", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 说明卡片
            Card(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = HospitalRed.copy(alpha = 0.1f))
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Warning,
                        null,
                        tint = HospitalRed,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "紧急情况临时监控",
                            color = HospitalRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            "仅限急救场景使用，申请通过后获得24小时临时监控权限\n过期自动失效，全程审计记录",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // 老人列表
            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = HospitalRed)
                }
            } else if (state.elderlyList.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📭", fontSize = 48.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "暂无绑定老人",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "需先由家属授权建立诊疗绑定关系",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                Text(
                    "选择需急救的老人",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                LazyColumn(
                    Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.elderlyList) { elderly ->
                        Card(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = CardBg)
                        ) {
                            Row(
                                Modifier.padding(14.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "👴 ${elderly.name}",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        "${elderly.gender.ifEmpty { "—" }} · ${elderly.birthDate.ifEmpty { "—" }}",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        "护理等级: ${elderly.careLevel}",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                                Button(
                                    onClick = {
                                        selectedElderlyId = elderly.id
                                        selectedElderlyName = elderly.name
                                        showRequestDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = HospitalRed),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("发起急救请求", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 急救请求对话框
    if (showRequestDialog) {
        AlertDialog(
            onDismissRequest = { showRequestDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = HospitalRed)
                    Spacer(Modifier.width(8.dp))
                    Text("急救监控权限申请")
                }
            },
            text = {
                Column {
                    Text(
                        "老人: $selectedElderlyName",
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "此操作将通知家属，请求24小时临时监控权限。仅在紧急救治情况下使用。",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("急救原因（必填）") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, null, tint = Accent, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "审批通过后将获得24小时有效直播流地址",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // 阶段3: 发送急救权限请求到后端API
                        viewModel.requestEmergencyAccess(selectedElderlyId, reason)
                        showRequestDialog = false
                        reason = ""
                    },
                    enabled = reason.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = HospitalRed)
                ) {
                    Text("确认发起", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRequestDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}
