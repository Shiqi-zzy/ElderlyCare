package com.elderlycare.app.ui.wizard

import androidx.compose.animation.*
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
import com.elderlycare.app.data.model.ElderlyProfile
import com.elderlycare.app.ui.components.StepIndicator
import com.elderlycare.app.ui.theme.*
import com.elderlycare.app.ui.wizard.steps.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WizardScreen(
    onWizardComplete: () -> Unit,
    onBackToLogin: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(1) }
    var profile by remember { mutableStateOf(ElderlyProfile()) }
    val scrollState = rememberScrollState()

    // 切换步骤时自动滚动到顶部
    LaunchedEffect(currentStep) {
        scrollState.animateScrollTo(0)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "档案录入",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    if (currentStep > 1) {
                        IconButton(onClick = { currentStep-- }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "上一步"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Surface
                )
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                color = Surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 上一步按钮
                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Text("上一步")
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // 下一步/完成按钮
                    Button(
                        onClick = {
                            if (currentStep < 6) {
                                currentStep++
                            } else {
                                onWizardComplete()
                            }
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Primary
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (currentStep < 7) "下一步" else "完成"
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 步进进度条
            StepIndicator(currentStep = currentStep)

            HorizontalDivider(color = CardBorder, thickness = 0.5.dp)

            // 当前步骤的内容
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(16.dp)
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        fadeIn() + slideInHorizontally { it / 4 } togetherWith
                                fadeOut() + slideOutHorizontally { -it / 4 }
                    },
                    label = "wizardStep"
                ) { step ->
                    when (step) {
                        1 -> Step1BasicInfo(profile) { profile = it }
                        2 -> Step2Lifestyle(profile) { profile = it }
                        3 -> Step3MedicalHistory(profile) { profile = it }
                        4 -> Step5PhysicalExam(profile) { profile = it }
                        5 -> Step6Hobbies(profile) { profile = it }
                        6 -> Step7DeviceBinding(profile) { profile = it }
                    }
                }
            }
        }
    }
}
