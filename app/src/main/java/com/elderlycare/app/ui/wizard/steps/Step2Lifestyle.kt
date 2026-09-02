package com.elderlycare.app.ui.wizard.steps

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.elderlycare.app.data.model.*
import com.elderlycare.app.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Step2Lifestyle(profile: ElderlyProfile, onUpdate: (ElderlyProfile) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // === 体育锻炼 ===
        SectionTitle("体育锻炼")
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("锻炼频率", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ExerciseFrequency.entries.forEach { freq ->
                        FilterChip(
                            selected = profile.exerciseFrequency == freq,
                            onClick = { onUpdate(profile.copy(exerciseFrequency = freq)) },
                            label = { Text(freq.label, style = MaterialTheme.typography.labelSmall, maxLines = 1) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                AnimatedVisibility(visible = profile.exerciseFrequency != ExerciseFrequency.NONE) {
                    Column {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = profile.exerciseDuration,
                                onValueChange = { onUpdate(profile.copy(exerciseDuration = it)) },
                                label = { Text("单次时长(分钟)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = profile.exerciseYears,
                                onValueChange = { onUpdate(profile.copy(exerciseYears = it)) },
                                label = { Text("坚持锻炼(年)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = profile.exerciseTypes.joinToString("、"),
                            onValueChange = { },
                            label = { Text("锻炼方式（可从兴趣爱好联动）") },
                            placeholder = { Text("如：广场舞、散步、太极") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // === 饮食习惯 ===
        SectionTitle("饮食习惯")
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("饮食类型", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DietType.entries.forEach { diet ->
                        FilterChip(
                            selected = profile.dietType == diet,
                            onClick = { onUpdate(profile.copy(dietType = diet)) },
                            label = { Text(diet.label) },
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("饮食偏好（多选）", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    dietPreferenceOptions.forEach { pref ->
                        val isSelected = profile.dietPreferences.contains(pref)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                val newList = if (isSelected)
                                    profile.dietPreferences - pref
                                else
                                    profile.dietPreferences + pref
                                onUpdate(profile.copy(dietPreferences = newList))
                            },
                            label = { Text(pref, style = MaterialTheme.typography.labelMedium) },
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // === 吸烟情况 ===
        SectionTitle("吸烟情况")
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("吸烟状态", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    SmokingStatus.entries.forEach { status ->
                        FilterChip(
                            selected = profile.smokingStatus == status,
                            onClick = { onUpdate(profile.copy(smokingStatus = status)) },
                            label = { Text(status.label, style = MaterialTheme.typography.labelMedium) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                AnimatedVisibility(
                    visible = profile.smokingStatus != SmokingStatus.NEVER
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = profile.dailyCigarettes,
                                onValueChange = { onUpdate(profile.copy(dailyCigarettes = it)) },
                                label = { Text("日均吸烟(支)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = profile.smokingStartAge,
                                onValueChange = { onUpdate(profile.copy(smokingStartAge = it)) },
                                label = { Text("开始吸烟年龄") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = profile.smokingStatus == SmokingStatus.QUIT) {
                    Column {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = profile.quitSmokingAge,
                            onValueChange = { onUpdate(profile.copy(quitSmokingAge = it)) },
                            label = { Text("戒烟年龄") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(0.5f),
                            singleLine = true
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // === 饮酒情况 ===
        SectionTitle("饮酒情况")
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("饮酒频率", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    DrinkingFrequency.entries.forEach { freq ->
                        FilterChip(
                            selected = profile.drinkingFrequency == freq,
                            onClick = { onUpdate(profile.copy(drinkingFrequency = freq)) },
                            label = { Text(freq.label, style = MaterialTheme.typography.labelMedium) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                AnimatedVisibility(visible = profile.drinkingFrequency != DrinkingFrequency.NEVER) {
                    Column {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = profile.dailyAlcoholAmount,
                                onValueChange = { onUpdate(profile.copy(dailyAlcoholAmount = it)) },
                                label = { Text("日均饮酒量(两)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = profile.drinkingStartAge,
                                onValueChange = { onUpdate(profile.copy(drinkingStartAge = it)) },
                                label = { Text("开始饮酒年龄") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        // 是否戒酒
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("是否戒酒", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                            Spacer(modifier = Modifier.width(12.dp))
                            FilterChip(
                                selected = profile.hasQuitAlcohol,
                                onClick = { onUpdate(profile.copy(hasQuitAlcohol = !profile.hasQuitAlcohol)) },
                                label = { Text("已戒酒") },
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                        AnimatedVisibility(visible = profile.hasQuitAlcohol) {
                            OutlinedTextField(
                                value = profile.quitAlcoholAge,
                                onValueChange = { onUpdate(profile.copy(quitAlcoholAge = it)) },
                                label = { Text("戒酒年龄") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .fillMaxWidth(0.5f)
                                    .padding(top = 8.dp),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("近一年是否醉酒", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                            Spacer(modifier = Modifier.width(12.dp))
                            FilterChip(
                                selected = profile.drunkInPastYear,
                                onClick = { onUpdate(profile.copy(drunkInPastYear = !profile.drunkInPastYear)) },
                                label = { Text("是") },
                                shape = RoundedCornerShape(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text("饮酒种类（多选）", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        val alcoholOptions = listOf("白酒", "啤酒", "红酒", "黄酒", "其他")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            alcoholOptions.forEach { type ->
                                FilterChip(
                                    selected = profile.alcoholTypes.contains(type),
                                    onClick = {
                                        val newTypes = if (profile.alcoholTypes.contains(type))
                                            profile.alcoholTypes - type
                                        else
                                            profile.alcoholTypes + type
                                        onUpdate(profile.copy(alcoholTypes = newTypes))
                                    },
                                    label = { Text(type, style = MaterialTheme.typography.labelMedium) },
                                    shape = RoundedCornerShape(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // === 职业接触史 ===
        SectionTitle("职业病有害接触史（可选填）")
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
            Column(modifier = Modifier.padding(16.dp)) {
                exposureTypeOptions.forEach { type ->
                    val exposure = profile.occupationalExposures.find { it.type == type }
                    val hasContact = exposure?.hasContact ?: false

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(type, style = MaterialTheme.typography.bodyLarge)
                        FilterChip(
                            selected = hasContact,
                            onClick = {
                                val newExposures = profile.occupationalExposures.toMutableList()
                                val idx = newExposures.indexOfFirst { it.type == type }
                                if (idx >= 0) {
                                    newExposures[idx] = newExposures[idx].copy(hasContact = !hasContact)
                                } else {
                                    newExposures.add(OccupationalExposure(type = type, hasContact = true))
                                }
                                onUpdate(profile.copy(occupationalExposures = newExposures))
                            },
                            label = { Text(if (hasContact) "有接触" else "无接触") },
                            shape = RoundedCornerShape(16.dp)
                        )
                    }

                    AnimatedVisibility(visible = hasContact) {
                        Row(
                            modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = exposure?.workYears ?: "",
                                onValueChange = { v ->
                                    val newExposures = profile.occupationalExposures.toMutableList()
                                    val idx = newExposures.indexOfFirst { it.type == type }
                                    if (idx >= 0) newExposures[idx] = newExposures[idx].copy(workYears = v)
                                    onUpdate(profile.copy(occupationalExposures = newExposures))
                                },
                                label = { Text("从业年限") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("防护措施:", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                Spacer(modifier = Modifier.width(4.dp))
                                FilterChip(
                                    selected = exposure?.hasProtection ?: false,
                                    onClick = {
                                        val newExposures = profile.occupationalExposures.toMutableList()
                                        val idx = newExposures.indexOfFirst { it.type == type }
                                        if (idx >= 0) newExposures[idx] = newExposures[idx].copy(hasProtection = !(exposure?.hasProtection ?: false))
                                        onUpdate(profile.copy(occupationalExposures = newExposures))
                                    },
                                    label = { Text(if (exposure?.hasProtection == true) "有" else "无") },
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
