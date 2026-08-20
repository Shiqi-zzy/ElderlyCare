package com.elderlycare.app.ui.wizard.steps

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elderlycare.app.data.model.ElderlyProfile
import com.elderlycare.app.ui.theme.*

@Composable
fun Step5PhysicalExam(profile: ElderlyProfile, onUpdate: (ElderlyProfile) -> Unit) {
    val exam = profile.physicalExam

    Column(modifier = Modifier.fillMaxWidth()) {
        // === 基础体表 ===
        SectionTitle("基础体表")
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
            Column(modifier = Modifier.padding(16.dp)) {
                ExamItem(
                    title = "意识状态",
                    status = exam.consciousness,
                    onStatusChange = { onUpdate(profile.copy(physicalExam = exam.copy(consciousness = it))) },
                    abnormalNote = exam.consciousnessAbnormal,
                    onAbnormalChange = { onUpdate(profile.copy(physicalExam = exam.copy(consciousnessAbnormal = it))) }
                )
                HorizontalDivider(color = CardBorder, modifier = Modifier.padding(vertical = 8.dp))
                ExamItem(
                    title = "皮肤",
                    status = exam.skin,
                    onStatusChange = { onUpdate(profile.copy(physicalExam = exam.copy(skin = it))) },
                    abnormalNote = exam.skinAbnormal,
                    onAbnormalChange = { onUpdate(profile.copy(physicalExam = exam.copy(skinAbnormal = it))) }
                )
                HorizontalDivider(color = CardBorder, modifier = Modifier.padding(vertical = 8.dp))
                ExamItem(
                    title = "巩膜",
                    status = exam.sclera,
                    onStatusChange = { onUpdate(profile.copy(physicalExam = exam.copy(sclera = it))) },
                    abnormalNote = exam.scleraAbnormal,
                    onAbnormalChange = { onUpdate(profile.copy(physicalExam = exam.copy(scleraAbnormal = it))) }
                )
                HorizontalDivider(color = CardBorder, modifier = Modifier.padding(vertical = 8.dp))
                ExamItem(
                    title = "淋巴结",
                    status = exam.lymphNodes,
                    onStatusChange = { onUpdate(profile.copy(physicalExam = exam.copy(lymphNodes = it))) },
                    abnormalNote = exam.lymphNodesAbnormal,
                    onAbnormalChange = { onUpdate(profile.copy(physicalExam = exam.copy(lymphNodesAbnormal = it))) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // === 五官 ===
        SectionTitle("五官检查")
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
            Column(modifier = Modifier.padding(16.dp)) {
                // 眼部
                Text("眼部", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = exam.leftVision,
                        onValueChange = { onUpdate(profile.copy(physicalExam = exam.copy(leftVision = it))) },
                        label = { Text("左眼视力") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = exam.rightVision,
                        onValueChange = { onUpdate(profile.copy(physicalExam = exam.copy(rightVision = it))) },
                        label = { Text("右眼视力") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = exam.correctedVision,
                    onValueChange = { onUpdate(profile.copy(physicalExam = exam.copy(correctedVision = it))) },
                    label = { Text("矫正视力") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("眼底", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Spacer(modifier = Modifier.width(12.dp))
                    FilterChip(
                        selected = exam.fundus == "正常",
                        onClick = { onUpdate(profile.copy(physicalExam = exam.copy(fundus = if (exam.fundus == "正常") "" else "正常"))) },
                        label = { Text("正常") },
                        shape = RoundedCornerShape(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = exam.fundusAbnormal,
                        onValueChange = { onUpdate(profile.copy(physicalExam = exam.copy(fundusAbnormal = it, fundus = "异常"))) },
                        label = { Text("异常记录") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                HorizontalDivider(color = CardBorder, modifier = Modifier.padding(vertical = 12.dp))

                // 耳部
                Text("听力", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val hearingOptions = listOf("听力正常", "轻度听力减退", "中重度听力减退", "听力丧失")
                    hearingOptions.forEach { option ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = exam.hearing == option,
                                onClick = { onUpdate(profile.copy(physicalExam = exam.copy(hearing = option))) },
                                colors = RadioButtonDefaults.colors(selectedColor = Primary)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(option, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                HorizontalDivider(color = CardBorder, modifier = Modifier.padding(vertical = 12.dp))

                // 口腔
                Text("口腔", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = exam.lipStatus,
                    onValueChange = { onUpdate(profile.copy(physicalExam = exam.copy(lipStatus = it))) },
                    label = { Text("口唇状态") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = exam.missingTeeth,
                        onValueChange = { onUpdate(profile.copy(physicalExam = exam.copy(missingTeeth = it))) },
                        label = { Text("缺齿") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = exam.decayedTeeth,
                        onValueChange = { onUpdate(profile.copy(physicalExam = exam.copy(decayedTeeth = it))) },
                        label = { Text("龋齿") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = exam.dentures,
                        onValueChange = { onUpdate(profile.copy(physicalExam = exam.copy(dentures = it))) },
                        label = { Text("义齿") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = exam.pharynx,
                    onValueChange = { onUpdate(profile.copy(physicalExam = exam.copy(pharynx = it))) },
                    label = { Text("咽部（有无充血、增生）") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // === 胸肺心脏 ===
        SectionTitle("胸肺、心脏")
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = exam.chestShape,
                        onValueChange = { onUpdate(profile.copy(physicalExam = exam.copy(chestShape = it))) },
                        label = { Text("胸廓形态") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = exam.breathSounds,
                        onValueChange = { onUpdate(profile.copy(physicalExam = exam.copy(breathSounds = it))) },
                        label = { Text("呼吸音") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = exam.rales,
                    onValueChange = { onUpdate(profile.copy(physicalExam = exam.copy(rales = it))) },
                    label = { Text("啰音") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = exam.heartRate,
                        onValueChange = { onUpdate(profile.copy(physicalExam = exam.copy(heartRate = it))) },
                        label = { Text("心率") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = exam.heartRhythm,
                        onValueChange = { onUpdate(profile.copy(physicalExam = exam.copy(heartRhythm = it))) },
                        label = { Text("心律") },
                        placeholder = { Text("齐/不齐") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = exam.heartMurmur,
                    onValueChange = { onUpdate(profile.copy(physicalExam = exam.copy(heartMurmur = it))) },
                    label = { Text("心脏杂音记录") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // === 腹部 + 运动 ===
        SectionTitle("腹部 & 运动功能")
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = exam.abdominalTenderness,
                        onValueChange = { onUpdate(profile.copy(physicalExam = exam.copy(abdominalTenderness = it))) },
                        label = { Text("腹部压痛") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = exam.abdominalMass,
                        onValueChange = { onUpdate(profile.copy(physicalExam = exam.copy(abdominalMass = it))) },
                        label = { Text("腹部包块") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = exam.liverEnlargement,
                        onValueChange = { onUpdate(profile.copy(physicalExam = exam.copy(liverEnlargement = it))) },
                        label = { Text("肝脏肿大") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                HorizontalDivider(color = CardBorder, modifier = Modifier.padding(vertical = 12.dp))

                Text("运动功能", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilterChip(
                        selected = exam.motorFunction == "可独立完成动作",
                        onClick = {
                            onUpdate(profile.copy(physicalExam = exam.copy(
                                motorFunction = if (exam.motorFunction == "可独立完成动作") "" else "可独立完成动作"
                            )))
                        },
                        label = { Text("可独立完成动作") },
                        shape = RoundedCornerShape(16.dp)
                    )
                    FilterChip(
                        selected = exam.motorFunction == "无法独立完成行动",
                        onClick = {
                            onUpdate(profile.copy(physicalExam = exam.copy(
                                motorFunction = if (exam.motorFunction == "无法独立完成行动") "" else "无法独立完成行动"
                            )))
                        },
                        label = { Text("无法独立完成行动") },
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ExamItem(
    title: String,
    status: String,
    onStatusChange: (String) -> Unit,
    abnormalNote: String,
    onAbnormalChange: (String) -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(60.dp), color = TextSecondary)
            Spacer(modifier = Modifier.width(8.dp))
            FilterChip(
                selected = status == "正常",
                onClick = { onStatusChange(if (status == "正常") "" else "正常") },
                label = { Text("正常") },
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilterChip(
                selected = status == "异常",
                onClick = { onStatusChange(if (status == "异常") "" else "异常") },
                label = { Text("异常") },
                shape = RoundedCornerShape(16.dp)
            )
        }
        if (status == "异常") {
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = abnormalNote,
                onValueChange = onAbnormalChange,
                label = { Text("异常描述") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
