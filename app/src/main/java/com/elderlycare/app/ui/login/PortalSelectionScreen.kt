package com.elderlycare.app.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.ui.theme.Primary
import com.elderlycare.app.ui.theme.TextPrimary
import com.elderlycare.app.ui.theme.TextSecondary

private val DeepBlue = Color(0xFF1A3A5C)
private val BorderGray = Color(0xFFE1E6EC)
private val TextMain = Color(0xFF1A3A5C)
private val TextDark = Color(0xFF2A3D52)
private val TextSub = Color(0xFF6B7280)
private val TextLight = Color(0xFF8A94A0)
private val ChevronGray = Color(0xFFB9C2CC)


@Composable
fun PortalSelectionScreen(
    onFamilyLogin: () -> Unit,
    onCommunityLogin: () -> Unit,
    onHospitalLogin: () -> Unit
) {
    var showRoleDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // ===== 顶部深蓝标题栏 =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DeepBlue)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color.White,
                modifier = Modifier.size(26.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("SV", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DeepBlue)
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text("身份选择", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.weight(1f))
            Text("银龄心语平台", color = Color(0xFF9FB4CC), fontSize = 12.sp)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("请选择登录身份进入相应工作台：", fontSize = 13.sp, color = Color(0xFF41566C))

            Spacer(modifier = Modifier.height(12.dp))

            // ===== 三端入口列表 =====
            PortalEntryCard(
                title = "居家用户",
                desc = "面向居家用户本人及其家属，提供远程看护、健康管理、告警与亲情互动服务。",
                icon = Icons.Filled.Home,
                onClick = { showRoleDialog = true } // 每次进入均弹角色选择（不记住）
            )
            Spacer(modifier = Modifier.height(10.dp))
            PortalEntryCard(
                title = "社区管护",
                desc = "面向社区网格员、驿站与社工，提供日常巡访、照护工单与入户服务管理。",
                icon = Icons.Filled.People,
                onClick = onCommunityLogin
            )
            Spacer(modifier = Modifier.height(10.dp))
            PortalEntryCard(
                title = "医护管理",
                desc = "面向社区医院医护团队，提供健康监测、档案管理与医疗联动处置服务。",
                icon = Icons.Filled.LocalHospital,
                onClick = onHospitalLogin
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // ===== 页脚 =====
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF7F8FA))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Copyright © 2026 银龄照护服务有限公司", fontSize = 10.sp, color = TextSub)
            Spacer(modifier = Modifier.height(3.dp))
            Text("版本 V1.0.0", fontSize = 10.sp, color = TextLight)
        }
    }

    // 居家用户角色选择弹窗（每次进入均弹出，不记住；选择结果供档案 Wizard 角色分流使用）
    if (showRoleDialog) {
        AlertDialog(
            onDismissRequest = { showRoleDialog = false },
            title = { Text("请选择您的使用角色", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "您将以哪种身份使用居家用户功能？",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    RoleOption(
                        title = "使用人本人",
                        desc = "设备为本人使用，可一键跳过档案填写",
                        onClick = {
                            ServiceLocator.settingsStore.setFamilyUserRole("self")
                            showRoleDialog = false
                            onFamilyLogin()
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    RoleOption(
                        title = "家属及居家照料人",
                        desc = "为家人管理档案、授权与查看告警",
                        onClick = {
                            ServiceLocator.settingsStore.setFamilyUserRole("family")
                            showRoleDialog = false
                            onFamilyLogin()
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showRoleDialog = false }) { Text("取消") }
            }
        )
    }
}

/** 角色选择项：圆形单选图标 + 标题 + 说明 */
@Composable
private fun RoleOption(
    title: String,
    desc: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, Primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .border(1.5.dp, Primary, CircleShape)
        ) {
            // 空单选圆
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(desc, color = TextSecondary, fontSize = 12.sp)
        }
    }
}

/** 门户入口卡片（机构入口型列表项） */
@Composable
private fun PortalEntryCard(title: String, desc: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderGray, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = TextMain,
            modifier = Modifier.size(30.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
            Spacer(modifier = Modifier.height(3.dp))
            Text(desc, fontSize = 11.sp, color = TextSub, lineHeight = 16.sp)
        }
        Icon(
            Icons.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = ChevronGray,
            modifier = Modifier.size(18.dp)
        )
    }
}
