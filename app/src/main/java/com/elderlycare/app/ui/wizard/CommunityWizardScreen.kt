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

data class CertInfo(
    val name: String = "",
    val idCard: String = "",
    val phone: String = "",
    val org: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityWizardScreen(onWizardComplete: () -> Unit, onExit: () -> Unit = {}) {
    var currentStep by remember { mutableIntStateOf(1) }
    var showNotice by remember { mutableStateOf(false) }
    var certInfo by remember { mutableStateOf(CertInfo()) }
    val scrollState = rememberScrollState()
    val totalSteps = 4

    LaunchedEffect(currentStep) { scrollState.animateScrollTo(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("社区管护 - 身份认证", fontWeight = FontWeight.SemiBold) },
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
                        shape = RoundedCornerShape(24.dp), colors = ButtonDefaults.buttonColors(containerColor = Secondary), modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (currentStep < totalSteps) "下一步" else "进入社区端")
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            StepIndicator(
                currentStep = currentStep, totalSteps = totalSteps,
                stepLabels = listOf("1", "2", "3", "4"),
                stepTexts = listOf("资质认证", "自动处理", "双重审核", "绑定用户")
            )
            HorizontalDivider(color = CardBorder, thickness = 0.5.dp)
            Column(modifier = Modifier.weight(1f).verticalScroll(scrollState).padding(16.dp)) {
                AnimatedContent(targetState = currentStep, transitionSpec = { fadeIn() + slideInHorizontally { it / 4 } togetherWith fadeOut() + slideOutHorizontally { -it / 4 } }, label = "communityWizard") { step ->
                    when (step) {
                        1 -> CommunityStepQual(certInfo) { certInfo = it }
                        2 -> CommunityStepProcessing()
                        3 -> CommunityStepReview()
                        4 -> CommunityStepBinding()
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
fun CommunityStepQual(certInfo: CertInfo, onCertChange: (CertInfo) -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("步骤 1：资质认证", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("请填写工作人员个人信息并上传资质证明", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
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
                value = certInfo.phone,
                onValueChange = { onCertChange(certInfo.copy(phone = it)) },
                label = { Text("手机号") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = certInfo.org,
                onValueChange = { onCertChange(certInfo.copy(org = it)) },
                label = { Text("所属机构") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("资质材料上传（仅支持 JPG/PNG/PDF）", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            val docs = listOf("网格员工作证", "社区照护服务授权函", "在职证明")
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
fun CommunityStepProcessing() {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("步骤 2：自动处理（隐私模糊 + 审计日志）", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(12.dp))
            Text("正在进行隐私脱敏处理...", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("身份证号中间8位掩码、手机号中间4位隐藏、详细住址截断", color = TextHint, style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text("上传行为已写入操作审计日志，日志只增不改", color = TextHint, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun CommunityStepReview() {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("步骤 3：双重认证审核", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
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
                    Text("人工终审", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.weight(1f))
                    Text("审核中...", color = StatusYellow)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("审核通过后授予时效型权限（默认1年），到期自动失效。用户可单方面撤销授权。", color = TextHint, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun CommunityStepBinding() {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("步骤 4：绑定辖区用户", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("选择辖区内用户档案，发起机构-用户数据授权绑定申请", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(12.dp))
            val elders = listOf("张** (3号楼 · 72岁)", "李** (5号楼 · 68岁)", "王** (1号楼 · 75岁)")
            elders.forEach { name ->
                Surface(shape = RoundedCornerShape(12.dp), color = SurfaceVariant, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(name, modifier = Modifier.weight(1f))
                        FilterChip(selected = true, onClick = {}, label = { Text("选择") }, shape = RoundedCornerShape(16.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("一位工作人员可绑定多位用户", color = TextHint, style = MaterialTheme.typography.labelMedium)
        }
    }
}
