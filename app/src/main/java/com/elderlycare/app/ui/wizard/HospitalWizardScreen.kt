package com.elderlycare.app.ui.wizard

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.elderlycare.app.ui.components.StepIndicator
import com.elderlycare.app.ui.theme.*

data class HospitalCertInfo(
    val name: String = "",
    val idCard: String = "",
    val licenseNo: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HospitalWizardScreen(onWizardComplete: () -> Unit, onExit: () -> Unit = {}) {
    var currentStep by remember { mutableIntStateOf(1) }
    var showNotice by remember { mutableStateOf(false) }
    var certInfo by remember { mutableStateOf(HospitalCertInfo()) }
    val scrollState = rememberScrollState()
    val totalSteps = 3

    LaunchedEffect(currentStep) { scrollState.animateScrollTo(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("医护管理 - 医疗资质认证", fontWeight = FontWeight.SemiBold) },
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
                        onClick = { if (currentStep < totalSteps) currentStep++ else showNotice = true },
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
                stepLabels = listOf("1", "2", "3"),
                stepTexts = listOf("医疗资质认证", "卫健委审核", "绑定患者")
            )
            HorizontalDivider(color = CardBorder, thickness = 0.5.dp)
            Column(modifier = Modifier.weight(1f).verticalScroll(scrollState).padding(16.dp)) {
                AnimatedContent(targetState = currentStep, transitionSpec = { fadeIn() + slideInHorizontally { it / 4 } togetherWith fadeOut() + slideOutHorizontally { -it / 4 } }, label = "hospitalWizard") { step ->
                    when (step) {
                        1 -> HospitalStepMedicalQual(certInfo) { certInfo = it }
                        2 -> HospitalStepReview()
                        3 -> HospitalStepBinding()
                    }
                }
            }
        }
    }

    if (showNotice) {
        ConsentNotice(
            onAgree = { showNotice = false; onWizardComplete() },
            onDismiss = { showNotice = false }
        )
    }
}

@Composable
fun HospitalStepMedicalQual(certInfo: HospitalCertInfo, onCertChange: (HospitalCertInfo) -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("步骤 1：医疗资质认证", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("请填写医护人员个人信息并上传执业资质", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = certInfo.name,
                onValueChange = { onCertChange(certInfo.copy(name = it)) },
                label = { Text("姓名") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = certInfo.idCard,
                onValueChange = { onCertChange(certInfo.copy(idCard = it)) },
                label = { Text("身份证号") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = certInfo.licenseNo,
                onValueChange = { onCertChange(certInfo.copy(licenseNo = it)) },
                label = { Text("执业证号") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("资质材料上传（仅支持 JPG/PNG/PDF，后端计算文件 MD5 防篡改）", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
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
            Text("步骤 3：绑定患者", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            val patients = listOf("张** (72岁 · 高血压/高血脂)", "李** (68岁 · 糖尿病)", "王** (75岁 · 冠心病)")
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
fun HospitalStepReview() {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("步骤 2：卫健委审核", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
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
