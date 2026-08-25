package com.elderlycare.app.ui.wizard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.model.ElderlyProfile
import com.elderlycare.app.ui.components.StepIndicator
import com.elderlycare.app.ui.theme.*
import com.elderlycare.app.ui.wizard.steps.*
import kotlinx.coroutines.launch

/**
 * 家属端「编辑家人档案」。
 *
 * 复用 Wizard 步骤 Step1/2/3/5/6（**跳过 Step7 设备绑定** —— 设备信息不可通过普通档案编辑修改，
 * 只能走现有绑定流程；copy 保留原 deviceSn/deviceValidateCode/deviceBound 不变）。
 * 顶部「设备」卡只读展示当前绑定设备（有 SN 时点击跳实时预览，不可改绑）。
 *
 * 路由不携带 elderlyId 参数 → 只能编辑「当前登录家属」的档案，无法深链编辑他人；
 * 保存时再次以当前用户身份校验（登出/切换账号则放弃保存）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyProfileEditScreen(
    onNavigateBack: () -> Unit,
    onOpenDevice: (String) -> Unit = {}
) {
    var currentStep by remember { mutableIntStateOf(1) }
    var profile by remember { mutableStateOf<ElderlyProfile?>(null) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // 加载当前登录家属的真实档案（无档案则空档案兜底，保存时按当前身份 upsert）
    LaunchedEffect(Unit) {
        val uid = ServiceLocator.userStore.getCurrentUserId() ?: ""
        profile = ServiceLocator.profileStore.getPrimaryProfile(uid) ?: ElderlyProfile()
    }

    LaunchedEffect(currentStep) { scrollState.animateScrollTo(0) }

    val p = profile ?: ElderlyProfile()

    fun save() {
        if (saving) return
        scope.launch {
            saving = true
            // 保存时再次当前用户身份校验：登出/切换账号则放弃保存，防止写入他人档案
            val uid = ServiceLocator.userStore.getCurrentUserId() ?: run {
                saving = false
                return@launch
            }
            ServiceLocator.profileStore.saveProfile(p.copy(userId = uid))
            // 云同步：fire-and-forget 上传自家后端（失败仅日志，不阻断保存返回）
            ServiceLocator.healthProfileCloudRepository.upload(p.copy(userId = uid))
            saving = false
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("编辑家人档案", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    if (currentStep > 1) {
                        IconButton(onClick = { currentStep-- }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "上一步")
                        }
                    } else {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp, color = Surface) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (currentStep > 1) {
                        OutlinedButton(onClick = { currentStep-- }, shape = RoundedCornerShape(24.dp), modifier = Modifier.weight(1f).padding(end = 8.dp)) { Text("上一步") }
                    } else { Spacer(modifier = Modifier.weight(1f)) }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = { if (currentStep < 5) currentStep++ else save() },
                        enabled = currentStep < 5 || !saving,
                        shape = RoundedCornerShape(24.dp), colors = ButtonDefaults.buttonColors(containerColor = Primary), modifier = Modifier.weight(1f)
                    ) {
                        if (saving && currentStep >= 5) {
                            // 保存中：白色 spinner 替代箭头图标
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = androidx.compose.ui.graphics.Color.White
                            )
                        } else {
                            Icon(Icons.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (currentStep < 5) "下一步" else if (saving) "保存中…" else "保存")
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            StepIndicator(
                currentStep = currentStep,
                totalSteps = 5,
                stepLabels = listOf("1", "2", "3", "4", "5"),
                stepTexts = listOf("基础信息", "生活习惯", "疾病史", "体检记录", "兴趣爱好")
            )
            HorizontalDivider(color = CardBorder, thickness = 0.5.dp)
            if (profile == null) {
                // 档案尚未加载完成，不渲染表单（防止误编辑覆盖真实档案）
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Column(modifier = Modifier.weight(1f).verticalScroll(scrollState).padding(16.dp)) {
                    // 设备卡：只读展示当前绑定设备（有 SN 时点击跳实时预览，不可改绑）
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (p.deviceSn.isNotBlank())
                                    Modifier.clickable { onOpenDevice(p.deviceSn) }
                                else
                                    Modifier
                            )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("设备", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                if (p.deviceSn.isNotBlank()) p.deviceSn else "未绑定设备",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (p.deviceSn.isNotBlank()) TextPrimary else TextHint
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                if (p.deviceBound) "已绑定（点击查看实时视频）" else "未绑定",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (p.deviceBound) Primary else TextHint
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    AnimatedContent(targetState = currentStep, transitionSpec = { fadeIn() + slideInHorizontally { it / 4 } togetherWith fadeOut() + slideOutHorizontally { -it / 4 } }, label = "familyEdit") { step ->
                        when (step) {
                            1 -> Step1BasicInfo(p) { profile = it }
                            2 -> Step2Lifestyle(p) { profile = it }
                            3 -> Step3MedicalHistory(p) { profile = it }
                            4 -> Step5PhysicalExam(p) { profile = it }
                            5 -> Step6Hobbies(p) { profile = it }
                        }
                    }
                }
            }
        }
    }
}
