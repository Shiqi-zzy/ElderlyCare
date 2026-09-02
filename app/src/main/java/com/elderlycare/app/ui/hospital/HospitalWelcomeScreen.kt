package com.elderlycare.app.ui.hospital

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elderlycare.app.R

/** 医院端欢迎页色值（深青绿色主题） */
private val TealDark = Color(0xFF2A9D8F)
private val TealLight = Color(0xFF52B7A8)
private val White = Color.White

/**
 * 医院端欢迎页（深青绿色主题，参考社区端欢迎页结构）。
 * 点击「进入工作台」进入医院端急救大屏。
 */
@Composable
fun HospitalWelcomeScreen(onEnter: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(TealDark, TealLight))
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // ===== Logo =====
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.LocalHospital, null, tint = White, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text("ElderlyCare", color = White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ===== 标题 =====
            Text(
                "智慧医疗 · 守护健康",
                color = White,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "专业医护团队，为老人提供全方位健康守护",
                color = White.copy(alpha = 0.85f),
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ===== 主视觉插画 =====
            Image(
                painter = painterResource(id = R.drawable.hospital_welcome_illustration),
                contentDescription = "医疗守护插画",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ===== 轮播指示点 =====
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(White))
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(White.copy(alpha = 0.4f)))
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(White.copy(alpha = 0.4f)))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ===== 进入工作台按钮 =====
            Button(
                onClick = onEnter,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(27.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = White,
                    contentColor = TealDark
                )
            ) {
                Text("进入工作台", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
