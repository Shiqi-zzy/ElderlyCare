package com.ezvizpro.ui.login

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private val DarkBg = Color(0xFF0F172A)
private val CardBg = Color(0xFF1E293B)

data class PortalOption(
    val key: String,
    val label: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
)

private val portals = listOf(
    PortalOption("family", "家属端", "查看老人数据 · 接收告警\n管理授权 · 隐私控制", Icons.Default.Home, Color(0xFF3B82F6)),
    PortalOption("community", "社区端", "处理工单 · 脱敏老人台账\n设备巡检 · 接受告警推送", Icons.Default.Apartment, Color(0xFFF59E0B)),
    PortalOption("hospital", "医院端", "绑定老人健康档案\n急救临时权限申请", Icons.Default.LocalHospital, Color(0xFFE11D48))
)

/**
 * 门户选择页 — 萤石设备绑定完成后第二步
 *
 * 用户选择 家属/社区/医院 → ViewModel.selectPortal → 后端返回 JWT → 导航进端。
 */
@Composable
fun PortalSelectionScreen(
    onPortalSelected: (role: String, token: String) -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selectedRole by remember { mutableStateOf<String?>(null) }

    val lastRole = remember {
        context.getSharedPreferences("ezvizpro_prefs", Context.MODE_PRIVATE)
            .getString("last_role", null)
    }

    // selectPortal 完成 → 导航
    LaunchedEffect(state.portalSelected) {
        if (state.portalSelected && state.backendToken.isNotEmpty()) {
            val role = state.currentRole ?: return@LaunchedEffect
            viewModel.resetForNavigation()
            onPortalSelected(role, state.backendToken)
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
                .padding(horizontal = 28.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("📱", fontSize = 48.sp)
            Spacer(Modifier.height(8.dp))
            Text("设备已绑定", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE2E8F0))
            Text(
                "请选择要进入的门户",
                fontSize = 14.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )
            if (lastRole != null) {
                Text(
                    "上次使用：${roleLabel(lastRole)}",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            }

            Spacer(Modifier.height(24.dp))

            // 三端卡片
            portals.forEach { portal ->
                val isSelected = selectedRole == portal.key
                val isLast = lastRole == portal.key
                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable { selectedRole = portal.key },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) portal.color.copy(alpha = 0.2f) else CardBg
                    ),
                    border = if (isSelected) CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(portal.color, portal.color))
                    ) else null
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(48.dp)
                                .background(portal.color.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(portal.icon, null, tint = portal.color, modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    portal.label,
                                    color = Color(0xFFE2E8F0),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                if (isLast && !isSelected) {
                                    Spacer(Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = portal.color.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            "上次",
                                            Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            color = portal.color,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                            Text(
                                portal.description,
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedRole = portal.key },
                            colors = RadioButtonDefaults.colors(selectedColor = portal.color)
                        )
                    }
                }
            }

            if (state.error != null) {
                Spacer(Modifier.height(12.dp))
                Text(state.error!!, color = Color(0xFFf87171), fontSize = 13.sp)
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    selectedRole?.let { role ->
                        context.getSharedPreferences("ezvizpro_prefs", Context.MODE_PRIVATE)
                            .edit().putString("last_role", role).apply()
                        viewModel.selectPortal(role)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = selectedRole != null && !state.isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = portals.firstOrNull { it.key == selectedRole }?.color ?: Color(0xFF3B82F6)
                )
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    if (state.isLoading) "加载中…" else "进入${portals.firstOrNull { it.key == selectedRole }?.label ?: ""}",
                    fontSize = 15.sp
                )
            }
        }
    }
}

private fun roleLabel(role: String) = when (role) {
    "family" -> "家属端"
    "community" -> "社区端"
    "hospital" -> "医院端"
    else -> role
}
