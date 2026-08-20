package com.elderlycare.app.ui.wizard.steps

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elderlycare.app.data.model.ElderlyProfile
import com.elderlycare.app.data.model.presetHobbies
import com.elderlycare.app.ui.components.TagChip
import com.elderlycare.app.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Step6Hobbies(profile: ElderlyProfile, onUpdate: (ElderlyProfile) -> Unit) {
    var customHobbyText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionTitle("兴趣爱好")

        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("选择已有标签（可多选）", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetHobbies.forEach { hobby ->
                        val isSelected = profile.hobbies.contains(hobby)
                        TagChip(
                            text = hobby,
                            selected = isSelected,
                            onClick = {
                                val newList = if (isSelected)
                                    profile.hobbies - hobby
                                else
                                    profile.hobbies + hobby
                                onUpdate(profile.copy(hobbies = newList))
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("自定义爱好", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customHobbyText,
                        onValueChange = { customHobbyText = it },
                        label = { Text("输入新爱好") },
                        placeholder = { Text("如：太极、钓鱼、摄影...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            if (customHobbyText.isNotBlank()) {
                                onUpdate(profile.copy(hobbies = profile.hobbies + customHobbyText.trim()))
                                customHobbyText = ""
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("添加")
                    }
                }

                // 已选标签展示区
                val allHobbies = profile.hobbies
                if (allHobbies.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("已选标签（点击 x 删除）", style = MaterialTheme.typography.labelMedium, color = TextHint)
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        allHobbies.forEach { hobby ->
                            TagChip(
                                text = hobby,
                                selected = true,
                                onClick = { },
                                showClose = true,
                                onClose = {
                                    onUpdate(profile.copy(hobbies = profile.hobbies - hobby))
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
