package com.elderlycare.app.ui.home

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
import com.elderlycare.app.data.model.*
import com.elderlycare.app.ui.components.CollapsibleCard
import com.elderlycare.app.ui.components.InfoRow
import com.elderlycare.app.ui.components.StatusBadge
import com.elderlycare.app.ui.theme.*
import com.elderlycare.app.util.BMICalculator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDetailScreen(
    onNavigateBack: () -> Unit
) {
    // Mock 档案数据
    val profile = remember {
        ElderlyProfile(
            name = "张爷爷", gender = Gender.MALE, age = "72", examDate = "2024-01-15",
            archiveNumber = "RK3-2024-0001", height = "170", weight = "68", waist = "85",
            temperature = "36.5", pulseRate = "72", respiration = "18",
            bloodPressureHigh = "128", bloodPressureLow = "85",
            phone = "139xxxx1234", emergencyContactName = "张小明", emergencyContactPhone = "138xxxx5678",
            exerciseFrequency = ExerciseFrequency.EVERYDAY, exerciseDuration = "30", exerciseYears = "5",
            exerciseTypes = listOf("广场舞", "散步"),
            dietType = DietType.BALANCED,
            dietPreferences = listOf("嗜盐", "嗜糖", "三餐规律"),
            smokingStatus = SmokingStatus.QUIT, dailyCigarettes = "10", smokingStartAge = "25", quitSmokingAge = "65",
            drinkingFrequency = DrinkingFrequency.OCCASIONALLY, dailyAlcoholAmount = "2", drinkingStartAge = "30",
            alcoholTypes = listOf("啤酒", "红酒"),
            currentSymptoms = listOf("头痛", "乏力"),
            chronicDiseases = listOf("高血压", "高血脂"),
            allergyHistory = "青霉素过敏",
            mentalHealthHistory = "轻度焦虑",
            cognitiveDeclineRecord = "偶尔健忘",
            healthSelfAssessment = HealthSelfAssessment.BASICALLY_SATISFIED,
            selfCareLevel = SelfCareLevel.MILD_DEPENDENCE,
            cognitiveScreening = ScreeningResult.NEGATIVE, cognitiveScore = "26",
            depressionScreening = ScreeningResult.NEGATIVE, depressionScore = "8",
            physicalExam = PhysicalExam(
                consciousness = "正常", skin = "正常", sclera = "正常", lymphNodes = "正常",
                leftVision = "0.8", rightVision = "0.7", correctedVision = "1.0", fundus = "正常",
                hearing = "听得见",
                lipStatus = "正常", missingTeeth = "2", decayedTeeth = "1", dentures = "1", pharynx = "无充血",
                chestShape = "正常", breathSounds = "清", rales = "无",
                heartRate = "72", heartRhythm = "齐", heartMurmur = "无",
                abdominalTenderness = "无", abdominalMass = "无", liverEnlargement = "无",
                motorFunction = "可独立完成动作"
            ),
            hobbies = listOf("戏曲", "广场舞", "养花"),
            deviceSn = "RK3-2024-A1B2C3", deviceBound = true,
            privacyConsentGiven = true
        )
    }

    val h = profile.height.toFloatOrNull() ?: 0f
    val w = profile.weight.toFloatOrNull() ?: 0f
    val bmi = BMICalculator.calculate(w, h)
    val bmiCategory = BMICalculator.getBMICategory(bmi)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("档案详情", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // 档案头部
            ProfileHeader(profile = profile, bmi = bmi, bmiCategory = bmiCategory)

            Spacer(modifier = Modifier.height(8.dp))

            // 6 个折叠卡片
            CollapsibleCard(title = "【基础身份与身体测量】") {
                InfoRow("姓名", profile.name)
                InfoRow("性别", profile.gender.label)
                InfoRow("年龄", "${profile.age}岁")
                InfoRow("体检日期", profile.examDate)
                InfoRow("建档编号", profile.archiveNumber)
                InfoRow("身高", "${profile.height} cm")
                InfoRow("体重", "${profile.weight} kg")
                InfoRow("腰围", "${profile.waist} cm")
                InfoRow("BMI", "$bmi (${bmiCategory.label})")
                InfoRow("体温", "${profile.temperature}℃")
                InfoRow("脉率", "${profile.pulseRate} 次/分")
                InfoRow("呼吸", "${profile.respiration} 次/分")
                InfoRow("血压", "${profile.bloodPressureHigh}/${profile.bloodPressureLow} mmHg")
                InfoRow("本人手机", profile.phone)
                InfoRow("紧急联系人", profile.emergencyContactName, showDivider = false)
                InfoRow("紧急电话", profile.emergencyContactPhone, showDivider = false)
            }

            CollapsibleCard(title = "【生活习惯与作息】") {
                InfoRow("锻炼频率", profile.exerciseFrequency.label)
                if (profile.exerciseFrequency != ExerciseFrequency.NONE) {
                    InfoRow("单次时长", "${profile.exerciseDuration} 分钟")
                    InfoRow("坚持锻炼", "${profile.exerciseYears} 年")
                    InfoRow("锻炼方式", profile.exerciseTypes.joinToString("、"))
                }
                InfoRow("饮食类型", profile.dietType.label)
                InfoRow("饮食偏好", profile.dietPreferences.ifEmpty { listOf("无") }.joinToString("、"))
                InfoRow("吸烟", profile.smokingStatus.label)
                if (profile.smokingStatus != SmokingStatus.NEVER) {
                    InfoRow("日均吸烟", "${profile.dailyCigarettes} 支")
                    InfoRow("开始年龄", "${profile.smokingStartAge} 岁")
                }
                if (profile.smokingStatus == SmokingStatus.QUIT) {
                    InfoRow("戒烟年龄", "${profile.quitSmokingAge} 岁")
                }
                InfoRow("饮酒频率", profile.drinkingFrequency.label)
                if (profile.drinkingFrequency != DrinkingFrequency.NEVER) {
                    InfoRow("日均饮酒", "${profile.dailyAlcoholAmount} 两")
                    InfoRow("饮酒种类", profile.alcoholTypes.joinToString("、"), showDivider = false)
                }
            }

            CollapsibleCard(title = "【既往症状与疾病史】") {
                InfoRow("躯体症状（一月内）", profile.currentSymptoms.ifEmpty { listOf("无") }.joinToString("、"))
                InfoRow("慢性疾病", profile.chronicDiseases.ifEmpty { listOf("无") }.joinToString("、"))
                InfoRow("过敏史", profile.allergyHistory.ifEmpty { "无" })
                InfoRow("精神心理疾病史", profile.mentalHealthHistory.ifEmpty { "无" })
                InfoRow("轻度认知障碍MCI记录", profile.cognitiveDeclineRecord.ifEmpty { "无" }, showDivider = false)
            }

            CollapsibleCard(title = "【身心认知抑郁评估】") {
                InfoRow("健康自评", profile.healthSelfAssessment?.label ?: "未填写")
                InfoRow("自理能力", "${profile.selfCareLevel?.label} (${profile.selfCareLevel?.score})")
                InfoRow("认知筛查", profile.cognitiveScreening?.label ?: "")
                InfoRow("认知总分", profile.cognitiveScore.ifEmpty { "未填写" })
                InfoRow("抑郁筛查", profile.depressionScreening?.label ?: "")
                InfoRow("抑郁总分", profile.depressionScore.ifEmpty { "未填写" }, showDivider = false)
            }

            CollapsibleCard(title = "【全身体检脏器记录】") {
                val exam = profile.physicalExam
                InfoRow("意识", exam.consciousness)
                InfoRow("皮肤", exam.skin)
                InfoRow("巩膜", exam.sclera)
                InfoRow("淋巴结", exam.lymphNodes)
                InfoRow("左眼视力", exam.leftVision)
                InfoRow("右眼视力", exam.rightVision)
                InfoRow("听力", exam.hearing)
                InfoRow("口唇", exam.lipStatus)
                InfoRow("缺齿/龋齿/义齿", "${exam.missingTeeth}/${exam.decayedTeeth}/${exam.dentures}")
                InfoRow("心率/心律", "${exam.heartRate} / ${exam.heartRhythm}")
                InfoRow("运动功能", exam.motorFunction, showDivider = false)
            }

            CollapsibleCard(title = "【兴趣爱好 & 设备绑定】") {
                InfoRow("兴趣爱好", profile.hobbies.joinToString("、"))
                if (profile.deviceBound) {
                    InfoRow("绑定设备", profile.deviceSn)
                    InfoRow("设备状态", "在线", showDivider = false)
                } else {
                    InfoRow("绑定设备", "未绑定", showDivider = false)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ProfileHeader(
    profile: ElderlyProfile,
    bmi: Float,
    bmiCategory: BMICalculator.BMICategory
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Surface
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = Primary.copy(alpha = 0.1f),
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("EL", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = Primary)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(profile.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.width(8.dp))
                if (profile.deviceBound) {
                    StatusBadge(text = "RK3 已绑定", color = StatusGreen)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "${profile.gender.label} · ${profile.age}岁 · ${profile.examDate}建档",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            if (profile.deviceBound) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "设备: ${profile.deviceSn}",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextHint
                )
            }
        }
    }
}
