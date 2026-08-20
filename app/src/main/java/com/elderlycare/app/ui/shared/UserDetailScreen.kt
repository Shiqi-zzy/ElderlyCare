package com.elderlycare.app.ui.shared

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.model.AppUser
import com.elderlycare.app.data.model.ElderlyProfile
import com.elderlycare.app.ui.components.InfoRow
import com.elderlycare.app.ui.components.StatusBadge
import com.elderlycare.app.ui.theme.*

/**
 * 共享「用户详情」屏（第四阶段权限化）。
 * 按 elderlyId 通过 [BindingRepository.getAccessibleElderlyById] 读取档案；
 * 当前工作人员无 ACTIVE 绑定 → 显示「无权限访问」，禁止 UI 层直接 getAllProfiles 绕过。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetailScreen(elderlyId: String, onNavigateBack: () -> Unit) {
    var staff by remember { mutableStateOf<AppUser?>(null) }
    var profile by remember { mutableStateOf<ElderlyProfile?>(null) }
    var denied by remember { mutableStateOf(false) }

    LaunchedEffect(elderlyId) {
        val s = ServiceLocator.staffUserStore.getCurrentStaffUser()
        staff = s
        if (s == null) return@LaunchedEffect
        val p = ServiceLocator.bindingRepository.getAccessibleElderlyById(s, elderlyId)
        if (p == null) {
            denied = true
            profile = null
        } else {
            denied = false
            profile = p
        }
    }

    val currentProfile = profile

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("用户详情", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        }
    ) { paddingValues ->
        when {
            staff == null -> CenterHint("未登录", paddingValues)
            denied -> CenterHint("无权限访问该老人", paddingValues)
            currentProfile == null -> CenterHint("加载中…", paddingValues)
            else -> DetailContent(currentProfile, paddingValues)
        }
    }
}

@Composable
private fun CenterHint(text: String, paddingValues: androidx.compose.foundation.layout.PaddingValues) {
    Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
        Text(text, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun DetailContent(profile: ElderlyProfile, paddingValues: androidx.compose.foundation.layout.PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 用户头部
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Primary.copy(alpha = 0.1f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            profile.name.take(1).ifBlank { "用" },
                            fontWeight = FontWeight.Bold,
                            color = Primary,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(profile.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.width(8.dp))
                        StatusBadge(text = profile.healthCategory().label, color = profile.healthCategory().color())
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("${profile.age}岁 · ${profile.gender.label}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
        }

        // 基本信息
        DetailCard("基本信息") {
            InfoRow("姓名", profile.name)
            InfoRow("性别", profile.gender.label)
            InfoRow("年龄", profile.age)
            InfoRow("身高", profile.height)
            InfoRow("体重", profile.weight)
            InfoRow("联系电话", profile.phone)
            InfoRow("紧急联系人", listOf(profile.emergencyContactName, profile.emergencyContactPhone).filter { it.isNotBlank() }.joinToString(" · "))
        }

        // 健康档案（展示模型已有字段，不虚构）
        DetailCard("健康档案") {
            InfoRow("慢病", profile.chronicDiseases.joinToString("、").ifEmpty { "无" })
            InfoRow("过敏史", profile.allergyHistory.ifEmpty { "无" })
            InfoRow("当前症状", profile.currentSymptoms.joinToString("、").ifEmpty { "无" })
            if (profile.bloodPressureHigh.isNotBlank() && profile.bloodPressureLow.isNotBlank()) {
                InfoRow("血压", "${profile.bloodPressureHigh}/${profile.bloodPressureLow} mmHg")
            }
            InfoRow("自理能力", profile.selfCareLevel?.label ?: "-")
            InfoRow("认知筛查", "${profile.cognitiveScreening?.label ?: "-"}" +
                (if (profile.cognitiveScore.isNotBlank()) "（${profile.cognitiveScore}分）" else ""))
            InfoRow("抑郁筛查", "${profile.depressionScreening?.label ?: "-"}" +
                (if (profile.depressionScore.isNotBlank()) "（${profile.depressionScore}分）" else ""))
        }

        // 设备状态（仅档案 deviceSn/deviceBound 判断，不读全局设备/告警）
        DetailCard("设备状态") {
            InfoRow("设备SN", profile.deviceSn.ifEmpty { "未绑定设备" })
            InfoRow("绑定状态", if (profile.deviceBound) "已绑定" else "未绑定")
        }
    }
}

@Composable
private fun DetailCard(title: String, content: @Composable () -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}
