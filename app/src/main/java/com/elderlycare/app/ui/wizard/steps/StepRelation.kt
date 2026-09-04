package com.elderlycare.app.ui.wizard.steps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elderlycare.app.data.model.ElderlyProfile
import com.elderlycare.app.ui.theme.*

/**
 * 家属及居家照料人登记时的「与照看对象关系」步骤（档案录入第 2 步）。
 *
 * 展示「我是（照看对象姓名）的」，选项：子女 / 亲戚 / 其他（其他可填写自定义关系）。
 * 选择结果存入 [ElderlyProfile.relationToElder]。
 */
@Composable
fun StepRelation(profile: ElderlyProfile, onUpdate: (ElderlyProfile) -> Unit) {
    val elderName = profile.name.ifBlank { "照看对象" }
    var custom by remember { mutableStateOf("") }

    // 当前选择（用于高亮）
    val relationOptions = listOf("子女", "亲戚", "其他")
    val current = profile.relationToElder

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionTitle("与照看对象关系")
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "我是${elderName}的：",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "请选择您与所登记照看对象的关系，用于建立照看对象与照料人的绑定记录。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                relationOptions.forEach { option ->
                    val selected = when (option) {
                        "其他" -> current.startsWith("其他")
                        else -> current == option
                    }
                    OutlinedButton(
                        onClick = {
                            if (option == "其他") {
                                onUpdate(profile.copy(relationToElder = "其他-"))
                            } else {
                                onUpdate(profile.copy(relationToElder = option))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = if (selected) BorderStroke(1.dp, Primary) else BorderStroke(1.dp, Color(0xFFE0E0E0)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selected) Primary.copy(alpha = 0.1f) else Color.Transparent
                        )
                    ) {
                        Text(
                            option,
                            modifier = Modifier.weight(1f),
                            color = if (selected) Primary else TextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        if (selected) {
                            Text("✓", color = Primary, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // 其他时自定义输入
                if (current.startsWith("其他")) {
                    OutlinedTextField(
                        value = if (current.length > 3) current.substring(3) else "",
                        onValueChange = { onUpdate(profile.copy(relationToElder = "其他-$it")) },
                        label = { Text("请注明具体关系（如：邻居 / 朋友）") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "已选择：我是${elderName}的${current.removePrefix("其他-").ifBlank { "其他" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Primary
                    )
                } else if (current.isNotBlank()) {
                    Text(
                        "已选择：我是${elderName}的$current",
                        style = MaterialTheme.typography.bodySmall,
                        color = Primary
                    )
                }
            }
        }
    }
}
