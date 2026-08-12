package com.ezvizpro.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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

/**
 * 萤石设备绑定登录页
 *
 * 自动用 AppKey/AppSecret 获取 accessToken → 后端 sync 设备标识。
 * 成功 → 导航到门户选择。失败 → 显示重试。
 */
@Composable
fun LoginScreen(
    onEzvizReady: (clientId: String, ezvizToken: String, currentRole: String?, backendToken: String) -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // 萤石登录完成 → 导航到门户选择
    LaunchedEffect(state.ezvizReady) {
        if (state.ezvizReady) {
            viewModel.resetForNavigation()
            onEzvizReady(state.clientId, state.ezvizToken, state.currentRole, state.backendToken)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1a1a2e), Color(0xFF16213e), Color(0xFF0f3460))
                )
            )
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🏠", fontSize = 56.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "智慧养老平台",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFe2e8f0)
            )
            Text(
                text = "萤视Pro · 基于萤石 RK3",
                fontSize = 14.sp,
                color = Color(0xFF94a3b8)
            )

            Spacer(Modifier.height(40.dp))

            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    color = Color(0xFF3B82F6),
                    strokeWidth = 3.dp
                )
                Spacer(Modifier.height(16.dp))
                Text("正在连接萤石平台…", color = Color(0xFF94a3b8), fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    "登录即完成设备绑定，无需手机验证码",
                    color = Color(0xFF64748B),
                    fontSize = 12.sp
                )
            }

            if (state.error != null) {
                Spacer(Modifier.height(16.dp))
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0x33EF4444))
                ) {
                    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "⚠️ ${state.error}",
                            color = Color(0xFFf87171),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.retry() },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                        ) { Text("重试") }
                    }
                }
            }
        }
    }
}
