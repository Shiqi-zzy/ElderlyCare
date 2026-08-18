package com.elderlycare.app.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elderlycare.app.ui.theme.*

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Primary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Logo 区
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = OnPrimary,
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("EC", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = OnPrimary)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "萤石养老看护",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = OnPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "家属端 · 远程看护助手",
                fontSize = 14.sp,
                color = OnPrimary.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(48.dp))
            // Mock 登录按钮
            Button(
                onClick = onLoginSuccess,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OnPrimary,
                    contentColor = Primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "登录萤石账号（演示）",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "点击即可进入（演示阶段无需真实账号）",
                fontSize = 12.sp,
                color = OnPrimary.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}
