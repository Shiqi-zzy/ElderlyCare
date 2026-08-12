package com.ezvizpro.ui.verify

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
private val Success = Color(0xFF22C55E)
private val Warning = Color(0xFFF59E0B)

/**
 * 资质验证页面 — 社区端/医院端的二次认证门控
 *
 * 未提交 → 显示上传表单
 * pending → 显示等待审核
 * approved → 回调 onVerified
 * rejected → 显示驳回原因 + 重新提交
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerificationScreen(
    role: String,  // "community" or "hospital"
    token: String,
    onVerified: () -> Unit,
    onBack: () -> Unit,
    viewModel: VerificationViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(token) { viewModel.initialize(token) }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (role == "community") "🏘 社区资质认证" else "🏥 医院资质认证",
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF1a1a2e), Color(0xFF16213e), Color(0xFF0f3460))
                    )
                )
        ) {
            when (state.status) {
                "none", "rejected" -> VerificationForm(
                    role = role,
                    state = state,
                    onSubmit = { instName, docUrls ->
                        viewModel.submitVerification(instName, role, docUrls)
                    }
                )
                "pending" -> PendingReview(state)
                "approved" -> {
                    LaunchedEffect(Unit) { onVerified() }
                }
                "loading" -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Accent)
                            Spacer(Modifier.height(16.dp))
                            Text("检查认证状态…", color = TextSecondary)
                        }
                    }
                }
            }

            if (state.error != null) {
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) { Text("关闭") }
                    }
                ) {
                    Text(state.error!!)
                }
            }
        }
    }
}

@Composable
private fun VerificationForm(
    role: String,
    state: VerificationUiState,
    onSubmit: (String, String) -> Unit
) {
    var institutionName by remember { mutableStateOf("") }
    var documentDesc by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))

        Icon(
            if (role == "community") Icons.Default.Apartment else Icons.Default.LocalHospital,
            null,
            modifier = Modifier.size(64.dp),
            tint = if (role == "community") Color(0xFFF59E0B) else Color(0xFFE11D48)
        )

        Spacer(Modifier.height(16.dp))
        Text(
            if (role == "community") "社区工作人员认证" else "医护人员认证",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            "请提交您的资质证明文件\n审核通过后方可使用平台功能",
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        if (state.status == "rejected") {
            Spacer(Modifier.height(16.dp))
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x33EF4444))
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Error, null, tint = Color(0xFFEF4444))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        state.reviewNote ?: "审核未通过，请重新提交",
                        color = Color(0xFFf87171),
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // 机构名称
        OutlinedTextField(
            value = institutionName,
            onValueChange = { institutionName = it },
            label = { Text(if (role == "community") "社区/机构名称" else "医院名称") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = darkFieldColors(),
            leadingIcon = {
                Icon(
                    if (role == "community") Icons.Default.Apartment else Icons.Default.LocalHospital,
                    null,
                    tint = TextSecondary
                )
            }
        )

        Spacer(Modifier.height(12.dp))

        // 资质描述（P2: 替换为文件上传）
        OutlinedTextField(
            value = documentDesc,
            onValueChange = { documentDesc = it },
            label = {
                Text(
                    if (role == "community") "工作证/授权函（P2支持拍照上传）"
                    else "医师执业证/科室证明（P2支持拍照上传）"
                )
            },
            modifier = Modifier.fillMaxWidth().height(100.dp),
            maxLines = 3,
            shape = RoundedCornerShape(10.dp),
            colors = darkFieldColors(),
            leadingIcon = {
                Icon(Icons.Default.Description, null, tint = TextSecondary)
            }
        )

        Spacer(Modifier.height(8.dp))
        Text(
            "※ 阶段1: 填写后直接提交，管理员手动审核\n※ 阶段2: 支持OCR自动初审 + 文件上传",
            color = TextSecondary,
            fontSize = 11.sp,
            lineHeight = 15.sp
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                val docUrls = """["${documentDesc}"]"""
                onSubmit(institutionName, docUrls)
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = institutionName.isNotBlank() && !state.isSubmitting,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Accent)
        ) {
            if (state.isSubmitting) {
                CircularProgressIndicator(
                    Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
            }
            Text("提交审核", fontSize = 16.sp)
        }
    }
}

@Composable
private fun PendingReview(state: VerificationUiState) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.HourglassTop,
                null,
                modifier = Modifier.size(72.dp),
                tint = Warning
            )
            Spacer(Modifier.height(20.dp))
            Text(
                "资质审核中",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                "您的资质证明已提交，管理员正在审核中\n请耐心等待，审核结果将尽快通知您",
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(top = 12.dp)
            )
            Spacer(Modifier.height(32.dp))
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row {
                        Text("提交时间：", color = TextSecondary, fontSize = 13.sp)
                        Text(
                            state.submittedAt?.take(16)?.replace("T", " ") ?: "—",
                            color = TextPrimary,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Row {
                        Text("审核状态：", color = TextSecondary, fontSize = 13.sp)
                        Text("等待管理员审核", color = Warning, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
fun darkFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color(0xFFe2e8f0),
    unfocusedTextColor = Color(0xFFe2e8f0),
    focusedBorderColor = Color(0xFF3b82f6),
    unfocusedBorderColor = Color(0xFF475569),
    focusedLabelColor = Color(0xFF60a5fa),
    unfocusedLabelColor = Color(0xFF94a3b8),
    cursorColor = Color(0xFF60a5fa)
)
