package com.elderlycare.app.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.People
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
fun PortalSelectionScreen(
    onFamilyLogin: () -> Unit,
    onCommunityLogin: () -> Unit,
    onHospitalLogin: () -> Unit
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
            // Logo
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = OnPrimary,
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("EC", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Primary)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "萤石养老看护",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = OnPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "请选择您的身份入口",
                fontSize = 15.sp,
                color = OnPrimary.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(36.dp))

            // 家属端卡片
            PortalCard(
                icon = { Icon(Icons.Filled.FamilyRestroom, null, tint = Primary, modifier = Modifier.size(32.dp)) },
                title = "家属端",
                subtitle = "子女亲属 · 远程看护助手",
                accentColor = Primary,
                onClick = onFamilyLogin
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 社区端卡片
            PortalCard(
                icon = { Icon(Icons.Filled.People, null, tint = Secondary, modifier = Modifier.size(32.dp)) },
                title = "社区端",
                subtitle = "网格员 · 养老驿站 · 社工",
                accentColor = Secondary,
                onClick = onCommunityLogin
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 医院端卡片
            PortalCard(
                icon = { Icon(Icons.Filled.LocalHospital, null, tint = Error, modifier = Modifier.size(32.dp)) },
                title = "医院端",
                subtitle = "社区医院 · 康复科 · 老年科",
                accentColor = Error,
                onClick = onHospitalLogin
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "演示阶段无需真实账号注册",
                fontSize = 14.sp,
                color = OnPrimary.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PortalCard(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    accentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = OnPrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = accentColor.copy(alpha = 0.1f),
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) { icon() }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
            Icon(
                Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = TextHint
            )
        }
    }
}
