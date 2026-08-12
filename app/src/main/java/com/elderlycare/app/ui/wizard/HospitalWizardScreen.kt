package com.elderlycare.app.ui.wizard

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elderlycare.app.ui.components.StepIndicator
import com.elderlycare.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HospitalWizardScreen(onWizardComplete: () -> Unit, onExit: () -> Unit = {}) {
    var currentStep by remember { mutableIntStateOf(1) }
    val scrollState = rememberScrollState()
    val totalSteps = 4

    LaunchedEffect(currentStep) { scrollState.animateScrollTo(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("医院端 - 医疗资质认证", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    if (currentStep > 1) {
                        IconButton(onClick = { currentStep-- }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "上一步")
                        }
                    }
                },
                actions = { IconButton(onClick = onExit) { Icon(Icons.Filled.Close, "退出认证") } },
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
                        onClick = { if (currentStep < totalSteps) currentStep++ else onWizardComplete() },
                        shape = RoundedCornerShape(24.dp), colors = ButtonDefaults.buttonColors(containerColor = Error), modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (currentStep < totalSteps) "下一步" else "进入医院端")
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            StepIndicator(
                currentStep = currentStep, totalSteps = totalSteps,
                stepLabels = listOf("1", "2", "3", "4"),
                stepTexts = listOf("医疗资质", "绑定患者", "隐私审查", "卫健委审核")
            )
            HorizontalDivider(color = CardBorder, thickness = 0.5.dp)
            Column(modifier = Modifier.weight(1f).verticalScroll(scrollState).padding(16.dp)) {
                AnimatedContent(targetState = currentStep, transitionSpec = { fadeIn() + slideInHorizontally { it / 4 } togetherWith fadeOut() + slideOutHorizontally { -it / 4 } }, label = "hospitalWizard") { step ->
                    when (step) {
                        1 -> HospitalStepMedicalQual()
                        2 -> HospitalStepBinding()
                        3 -> HospitalStepPrivacy()
                        4 -> HospitalStepReview()
                    }
                }
            }
        }
    }
}

@Composable
fun HospitalStepMedicalQual() {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("步骤 1：医疗资质上传", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("仅支持 JPG/PNG/PDF，后端计算文件 MD5 防篡改", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(12.dp))
            val docs = listOf("医师执业资格证", "科室在岗证明", "急救协作授权函")
            docs.forEach { doc ->
                Surface(shape = RoundedCornerShape(12.dp), color = SurfaceVariant, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(doc, modifier = Modifier.weight(1f))
                        OutlinedButton(onClick = {}, shape = RoundedCornerShape(16.dp)) { Text("上传") }
                    }
                }
            }
        }
    }
}

@Composable
fun HospitalStepBinding() {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("步骤 2：绑定患者老人", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            val patients = listOf("张爷爷 (72岁 · 高血压/高血脂)", "李奶奶 (68岁 · 糖尿病)", "王爷爷 (75岁 · 冠心病)")
            patients.forEach { name ->
                Surface(shape = RoundedCornerShape(12.dp), color = SurfaceVariant, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(name, modifier = Modifier.weight(1f))
                        FilterChip(selected = true, onClick = {}, label = { Text("绑定") }, shape = RoundedCornerShape(16.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Surface(shape = RoundedCornerShape(8.dp), color = Error.copy(alpha = 0.08f), modifier = Modifier.fillMaxWidth()) {
                Text("医疗人员仅可查看健康档案，日常不可查看监控视频（急救场景除外）", modifier = Modifier.padding(8.dp), color = Error, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun HospitalStepPrivacy() {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("步骤 3：隐私与数据安全审查", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            val items = listOf("医疗数据与安防数据物理隔离", "医护只能读取健康病历，日常监控画面完全隐藏", "医生执业资质线上备案、到期年审冻结业务权限", "全操作审计日志留存，用于核查溯源")
            items.forEach { item ->
                Row(modifier = Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(3.dp), color = StatusGreen, modifier = Modifier.size(6.dp)) {}
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(item, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun HospitalStepReview() {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("步骤 4：卫健委审核", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            Surface(shape = RoundedCornerShape(12.dp), color = StatusGreen.copy(alpha = 0.1f), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("机器初审 (OCR)", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.weight(1f))
                    Text("已通过", color = StatusGreen)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Surface(shape = RoundedCornerShape(12.dp), color = StatusYellow.copy(alpha = 0.1f), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("卫健委人工复审", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.weight(1f))
                    Text("审核中...", color = StatusYellow)
                }
            }
        }
    }
}
