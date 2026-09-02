package com.elderlycare.app.ui.wizard

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elderlycare.app.R
import com.elderlycare.app.data.ezviz.EzvizAgentMemoryUtil
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.model.ElderlyProfile
import com.elderlycare.app.ui.components.StepIndicator
import com.elderlycare.app.ui.theme.*
import com.elderlycare.app.ui.wizard.steps.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyWizardScreen(onWizardComplete: () -> Unit, onExit: () -> Unit = {}) {
    var currentStep by remember { mutableIntStateOf(1) }
    var profile by remember { mutableStateOf(ElderlyProfile()) }
    var showNotice by remember { mutableStateOf(false) }
    // 第 6 步设备验证码是否已同步后端（device_auth）；已绑定设备且未同步时禁用【完成】
    var backendSynced by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(currentStep) { scrollState.animateScrollTo(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("档案录入", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    if (currentStep > 1) {
                        IconButton(onClick = { currentStep-- }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "上一步")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onExit) { Icon(Icons.Filled.Close, "退出录入") }
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
                        onClick = { if (currentStep < 6) currentStep++ else showNotice = true },
                        shape = RoundedCornerShape(24.dp), colors = ButtonDefaults.buttonColors(containerColor = Primary), modifier = Modifier.weight(1f),
                        // 保留「可不绑定设备完成向导」自由度；已绑定设备则必须验证码已同步后端
                        enabled = currentStep < 6 || !profile.deviceBound || backendSynced
                    ) {
                        Icon(Icons.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (currentStep < 6) "下一步" else "完成")
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            StepIndicator(currentStep = currentStep)
            HorizontalDivider(color = CardBorder, thickness = 0.5.dp)
            Column(modifier = Modifier.weight(1f).verticalScroll(scrollState).padding(16.dp)) {
                AnimatedContent(targetState = currentStep, transitionSpec = { fadeIn() + slideInHorizontally { it / 4 } togetherWith fadeOut() + slideOutHorizontally { -it / 4 } }, label = "familyWizard") { step ->
                    when (step) {
                        1 -> Step1BasicInfo(profile) { profile = it }
                        2 -> Step2Lifestyle(profile) { profile = it }
                        3 -> Step3MedicalHistory(profile) { profile = it }
                        4 -> Step5PhysicalExam(profile) { profile = it }
                        5 -> Step6Hobbies(profile) { profile = it }
                        6 -> Step7DeviceBinding(profile, { profile = it }, backendSynced) { backendSynced = it }
                    }
                }
            }
        }
    }

    if (showNotice) {
        ConsentNotice(
            onAgree = {
                showNotice = false
                // 最终提交：先本地持久化；本地保存成功之后才执行智能体长期记忆上报
                // （合并多端后 userId 绑定当前登录家属，档案按 userId upsert）
                scope.launch {
                    val profileWithUser = profile.copy(
                        userId = ServiceLocator.userStore.getCurrentUserId().orEmpty()
                    )
                    val saved = ServiceLocator.profileStore.save(profileWithUser)
                    if (saved) {
                        Toast.makeText(context, R.string.profile_save_success, Toast.LENGTH_SHORT).show()
                        // 异步上报：失败仅记日志，绝不阻断提交、不弹窗（表单已提示保存成功）
                        EzvizAgentMemoryUtil.reportElderlyProfile(profileWithUser)
                        // 云同步：fire-and-forget 上传自家后端（失败仅日志，不阻断提交）
                        ServiceLocator.healthProfileCloudRepository.upload(profileWithUser)
                        onWizardComplete()
                    } else {
                        // 本地保存失败：不上报、不进入首页，提示家属重试
                        Toast.makeText(context, R.string.profile_save_failed, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = { showNotice = false }
        )
    }
}
