package com.elderlycare.app.ui.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.elderlycare.app.R
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.local.SettingsStore
import com.elderlycare.app.ui.theme.*
import kotlinx.coroutines.delay

/**
 * 建议 Tab 下方的 AI 智能体配置表单。
 *
 * 仅本地保存参数（SettingsStore），**不发起任何请求**——建议的真实生成由 RK3 设备端
 * AI 智能体完成；本页「生成家属建议」按钮只刷新 /api/suggestions/latest 拉取结果。
 */
@Composable
fun AiAgentConfigForm() {
    val store = ServiceLocator.settingsStore
    val initial = remember { store.getAiAgentConfig() }
    var modelName by remember { mutableStateOf(initial.modelName.ifBlank { "rk3-emotion-agent" }) }
    var temperature by remember { mutableStateOf(initial.temperature.ifBlank { "0.7" }) }
    var maxTokens by remember { mutableStateOf(initial.maxTokens.ifBlank { "1024" }) }
    var systemPrompt by remember { mutableStateOf(initial.systemPrompt) }
    var showSaved by remember { mutableStateOf(false) }

    // 「配置已保存」提示 2 秒后自动消失
    LaunchedEffect(showSaved) {
        if (showSaved) {
            delay(2000)
            showSaved = false
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                stringResource(R.string.report_suggestion_ai_config_title),
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium
            )
            OutlinedTextField(
                value = modelName,
                onValueChange = { modelName = it },
                label = { Text(stringResource(R.string.report_suggestion_ai_model)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = temperature,
                onValueChange = { temperature = it },
                label = { Text(stringResource(R.string.report_suggestion_ai_temperature)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = maxTokens,
                onValueChange = { maxTokens = it },
                label = { Text(stringResource(R.string.report_suggestion_ai_max_tokens)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = systemPrompt,
                onValueChange = { systemPrompt = it },
                label = { Text(stringResource(R.string.report_suggestion_ai_prompt)) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                stringResource(R.string.report_suggestion_ai_config_local_only),
                style = MaterialTheme.typography.bodySmall,
                color = TextHint
            )
            Button(
                onClick = {
                    store.setAiAgentConfig(
                        SettingsStore.AiAgentConfig(
                            modelName = modelName.trim(),
                            temperature = temperature.trim(),
                            maxTokens = maxTokens.trim(),
                            systemPrompt = systemPrompt
                        )
                    )
                    showSaved = true
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text(stringResource(R.string.report_suggestion_ai_config_save), modifier = Modifier.padding(vertical = 4.dp))
            }
            if (showSaved) {
                Text(
                    stringResource(R.string.report_suggestion_ai_config_saved),
                    color = Primary,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}
