package com.ezvizpro.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onReady: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(800)
        onReady()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1a1a2e), Color(0xFF16213e), Color(0xFF0f3460))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🏠", fontSize = 64.sp)
            Spacer(Modifier.height(16.dp))
            Text("智慧养老平台", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFFe2e8f0))
            Text("萤视Pro · RK3", fontSize = 14.sp, color = Color(0xFF94a3b8))
            Spacer(Modifier.height(32.dp))
            Text("正在启动…", fontSize = 13.sp, color = Color(0xFF64748b))
        }
    }
}
