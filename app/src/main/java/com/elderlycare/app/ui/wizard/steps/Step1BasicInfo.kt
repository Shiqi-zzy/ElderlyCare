package com.elderlycare.app.ui.wizard.steps

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.elderlycare.app.data.model.ElderlyProfile
import com.elderlycare.app.data.model.Gender
import com.elderlycare.app.ui.theme.*
import com.elderlycare.app.util.BMICalculator

@Composable
fun Step1BasicInfo(profile: ElderlyProfile, onUpdate: (ElderlyProfile) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionTitle("基础标识")
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // 姓名（必填）
                OutlinedTextField(
                    value = profile.name,
                    onValueChange = { onUpdate(profile.copy(name = it)) },
                    label = { Text("名字（必填）") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 性别
                    Column(modifier = Modifier.weight(1f)) {
                        Text("性别", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Gender.entries.forEach { gender ->
                                FilterChip(
                                    selected = profile.gender == gender,
                                    onClick = { onUpdate(profile.copy(gender = gender)) },
                                    label = { Text(gender.label) },
                                    shape = RoundedCornerShape(20.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = profile.age,
                        onValueChange = { onUpdate(profile.copy(age = it)) },
                        label = { Text("年龄") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = profile.examDate,
                        onValueChange = { onUpdate(profile.copy(examDate = it)) },
                        label = { Text("体检/建档日期") },
                        placeholder = { Text("2024-01-15") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = profile.archiveNumber,
                    onValueChange = { onUpdate(profile.copy(archiveNumber = it)) },
                    label = { Text("建档编号") },
                    placeholder = { Text("自动生成，可修改") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        SectionTitle("人体测量数据")

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // 身高 体重 腰围
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = profile.height,
                        onValueChange = { onUpdate(profile.copy(height = it)) },
                        label = { Text("身高(cm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = profile.weight,
                        onValueChange = { onUpdate(profile.copy(weight = it)) },
                        label = { Text("体重(kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // BMI 自动计算展示
                val h = profile.height.toFloatOrNull() ?: 0f
                val w = profile.weight.toFloatOrNull() ?: 0f
                val bmi = BMICalculator.calculate(w, h)
                val bmiCategory = BMICalculator.getBMICategory(bmi)

                if (bmi > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = androidx.compose.ui.graphics.Color(bmiCategory.color).copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "BMI 体质指数",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Row {
                                Text(
                                    "$bmi",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = androidx.compose.ui.graphics.Color(bmiCategory.color)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = androidx.compose.ui.graphics.Color(bmiCategory.color)
                                ) {
                                    Text(
                                        bmiCategory.label,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        color = OnPrimary,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("生命体征", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                Spacer(modifier = Modifier.height(8.dp))

                // 生命体征 4 格
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = profile.temperature,
                        onValueChange = { onUpdate(profile.copy(temperature = it)) },
                        label = { Text("体温") },
                        placeholder = { Text("℃") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = profile.pulseRate,
                        onValueChange = { onUpdate(profile.copy(pulseRate = it)) },
                        label = { Text("脉率") },
                        placeholder = { Text("次/分") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = profile.respiration,
                        onValueChange = { onUpdate(profile.copy(respiration = it)) },
                        label = { Text("呼吸") },
                        placeholder = { Text("次/分") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = profile.bloodPressureHigh,
                        onValueChange = { onUpdate(profile.copy(bloodPressureHigh = it)) },
                        label = { Text("血压-高压") },
                        placeholder = { Text("mmHg") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Text(
                        text = "/",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextHint,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    OutlinedTextField(
                        value = profile.bloodPressureLow,
                        onValueChange = { onUpdate(profile.copy(bloodPressureLow = it)) },
                        label = { Text("血压-低压") },
                        placeholder = { Text("mmHg") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        SectionTitle("紧急联系信息")

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = profile.phone,
                    onValueChange = { onUpdate(profile.copy(phone = it)) },
                    label = { Text("本人手机号") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = profile.emergencyContactName,
                    onValueChange = { onUpdate(profile.copy(emergencyContactName = it)) },
                    label = { Text("紧急联系人姓名") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = profile.emergencyContactPhone,
                    onValueChange = { onUpdate(profile.copy(emergencyContactPhone = it)) },
                    label = { Text("紧急联系电话") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
    )
}
