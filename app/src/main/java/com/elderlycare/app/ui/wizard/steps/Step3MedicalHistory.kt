package com.elderlycare.app.ui.wizard.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elderlycare.app.data.model.ElderlyProfile
import com.elderlycare.app.data.model.chronicDiseaseOptions
import com.elderlycare.app.data.model.symptomOptions
import com.elderlycare.app.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Step3MedicalHistory(profile: ElderlyProfile, onUpdate: (ElderlyProfile) -> Unit) {
    var showPrivacyDialog by remember { mutableStateOf(!profile.privacyConsentGiven) }

    // 隐私授权弹窗
    if (showPrivacyDialog) {
        PrivacyConsentDialog(
            onAgree = {
                onUpdate(profile.copy(privacyConsentGiven = true))
                showPrivacyDialog = false
            },
            onReject = {
                onUpdate(profile.copy(privacyConsentGiven = false))
                showPrivacyDialog = false
            }
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // 隐私未授权时显示锁定提示
        if (!profile.privacyConsentGiven) {
            SectionTitle("当下躯体症状（一月内）")
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = StatusYellow.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "敏感信息已锁定，仅开放基础姓名与设备录入",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        } else {
            // === 当下躯体症状（一月内） ===
            SectionTitle("当下躯体症状（一月内）")

            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val hasNoSymptom = profile.currentSymptoms.contains("无症状")

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        symptomOptions.forEach { symptom ->
                            val isSelected = profile.currentSymptoms.contains(symptom)
                            val isDisabled = (hasNoSymptom && symptom != "无症状") ||
                                    (!hasNoSymptom && symptom == "无症状" && profile.currentSymptoms.isNotEmpty())

                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (symptom == "无症状") {
                                        onUpdate(profile.copy(currentSymptoms = if (isSelected) emptyList() else listOf("无症状")))
                                    } else {
                                        val newList = if (isSelected)
                                            profile.currentSymptoms - symptom
                                        else
                                            profile.currentSymptoms.filter { it != "无症状" } + symptom
                                        onUpdate(profile.copy(currentSymptoms = newList))
                                    }
                                },
                                label = { Text(symptom, style = MaterialTheme.typography.labelMedium) },
                                shape = RoundedCornerShape(16.dp),
                                enabled = !isDisabled
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // === 慢性基础疾病 ===
            SectionTitle("慢性基础疾病（多选）")
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        chronicDiseaseOptions.forEach { disease ->
                            val selected = profile.chronicDiseases.contains(disease)
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    val newList = if (selected)
                                        profile.chronicDiseases - disease
                                    else
                                        profile.chronicDiseases + disease
                                    onUpdate(profile.copy(chronicDiseases = newList))
                                },
                                label = { Text(disease, style = MaterialTheme.typography.labelMedium) },
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // === 专项病史 ===
            SectionTitle("专项病史录入")
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = profile.allergyHistory,
                        onValueChange = { onUpdate(profile.copy(allergyHistory = it)) },
                        label = { Text("过敏史（药物/食物过敏）") },
                        placeholder = { Text("无则填「无」") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 3
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = profile.mentalHealthHistory,
                        onValueChange = { onUpdate(profile.copy(mentalHealthHistory = it)) },
                        label = { Text("精神心理疾病史") },
                        placeholder = { Text("如：焦虑、抑郁、躁郁等") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 3
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = profile.cognitiveDeclineRecord,
                        onValueChange = { onUpdate(profile.copy(cognitiveDeclineRecord = it)) },
                        label = { Text("轻度认知障碍MCI记录") },
                        placeholder = { Text("如：健忘、昼夜颠倒、重复问话等表现") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 3
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // === 病历图片上传 ===
            SectionTitle("病历附件上传（最多6张）")
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "支持拍照/相册上传病历、体检报告图片",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        "上传前请注意：病历图片含敏感个人信息，仅加密存储于本地与自研后端",
                        style = MaterialTheme.typography.labelSmall,
                        color = StatusYellow,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 已上传图片预览（Mock）
                        profile.medicalImages.forEachIndexed { index, _ ->
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceVariant)
                                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("IMG", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                                }
                                // 删除按钮
                                IconButton(
                                    onClick = {
                                        onUpdate(profile.copy(
                                            medicalImages = profile.medicalImages.toMutableList().also { it.removeAt(index) }
                                        ))
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(22.dp)
                                        .offset(x = 4.dp, y = (-4).dp)
                                        .clip(CircleShape)
                                        .background(Error)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "删除",
                                        tint = OnPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        // 添加按钮
                        if (profile.medicalImages.size < 6) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.5.dp, Primary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .clickable {
                                        // Mock: 添加一张占位图片
                                        onUpdate(profile.copy(
                                            medicalImages = profile.medicalImages + "image_${profile.medicalImages.size + 1}"
                                        ))
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.PhotoCamera,
                                        contentDescription = "拍照",
                                        tint = Primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "添加",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Primary
                                    )
                                }
                            }
                        }
                    }

                    if (profile.medicalImages.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "${profile.medicalImages.size}/6 张",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextHint
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * 隐私与健康数据授权弹窗。
 *
 * 普通模式（向导 Step3）：同意/拒绝选择，未决定前不允许点外部关闭；
 * 只读模式（我的页「查看隐私同意协议」）：仅展示协议文案与当前授权状态，
 * 「知道了」关闭，不修改任何数据。
 */
@Composable
fun PrivacyConsentDialog(
    onAgree: () -> Unit,
    onReject: () -> Unit,
    readOnly: Boolean = false,
    consentGiven: Boolean = false,
    onDismiss: () -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = { if (readOnly) onDismiss() },
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                "隐私与健康数据授权",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    "本 APP 将采集慢病、情绪、病历等健康信息，仅用于生成情绪倾向健康报告、推送正念干预日程；数据加密存储，不会对外出售、共享，您可随时删除全部档案及图片。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    if (readOnly)
                        "当前授权状态：${if (consentGiven) "已同意采集" else "未同意（仅填写基础姓名设备信息）"}"
                    else
                        "是否同意采集？",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        confirmButton = {
            if (readOnly) {
                TextButton(onClick = onDismiss) { Text("知道了") }
            } else {
                Button(
                    onClick = onAgree,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text("同意采集")
                }
            }
        },
        dismissButton = {
            if (!readOnly) {
                OutlinedButton(
                    onClick = onReject,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("拒绝，仅填写基础姓名设备信息")
                }
            }
        }
    )
}
