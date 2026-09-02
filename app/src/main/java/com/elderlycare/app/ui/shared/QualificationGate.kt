package com.elderlycare.app.ui.shared

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.model.AppUser
import com.elderlycare.app.data.model.QualificationStatus
import com.elderlycare.app.ui.theme.*

/**
 * 工作资格闸门（社区/医院业务 tab 守卫）。
 *
 * 自动解析当前登录 staff 的 qualification：
 * - APPROVED（或旧账号 null，视为已通过）→ 放行 content()；
 * - PENDING / REJECTED → 显示审核中/未通过占位，不渲染业务内容。
 *
 * 注册新账号默认 PENDING；资格页提供演示审核按钮修改状态，保存后由闸门实时拦截/放行。
 */
@Composable
fun QualificationGate(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var staff by remember { mutableStateOf<AppUser?>(null) }
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        staff = ServiceLocator.staffUserStore.getCurrentStaffUser()
        loaded = true
    }

    when {
        !loaded -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        staff == null -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("未登录，无法访问业务功能", color = TextSecondary)
        }
        else -> {
            val status = runCatching {
                QualificationStatus.valueOf(staff?.qualification ?: QualificationStatus.APPROVED.name)
            }.getOrDefault(QualificationStatus.APPROVED)
            if (status == QualificationStatus.APPROVED) {
                content()
            } else {
                Column(
                    modifier = modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Filled.VerifiedUser,
                        null,
                        modifier = Modifier.size(72.dp),
                        tint = if (status == QualificationStatus.REJECTED) StatusRed else StatusYellow
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        if (status == QualificationStatus.REJECTED) "工作资格未通过" else "工作资格审核中",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (status == QualificationStatus.REJECTED) {
                            "您的申请已被驳回，请联系管理员后重新提交"
                        } else {
                            "您的注册已完成，审核通过后即可使用业务功能"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}
