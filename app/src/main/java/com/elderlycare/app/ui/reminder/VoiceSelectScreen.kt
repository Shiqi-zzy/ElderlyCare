package com.elderlycare.app.ui.reminder

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elderlycare.app.R
import com.elderlycare.app.data.reminder.PreviewVoices
import com.elderlycare.app.ui.theme.DividerColor
import com.elderlycare.app.ui.theme.Primary
import com.elderlycare.app.ui.theme.Surface as SurfaceColor
import com.elderlycare.app.ui.theme.TextPrimary
import com.elderlycare.app.ui.theme.TextSecondary

/**
 * 选择声音页（手机试听音色单选）。
 *
 * 选中项 Check 勾选；点击后通过 previousBackStackEntry.savedStateHandle
 * 写回 "voice_key" 并 popBackStack（跨 back stack entry 回传标准模式，
 * 表单页 LaunchedEffect 收集 getStateFlow 回填）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSelectScreen(
    currentVoiceKey: String,
    onVoiceSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.reminder_voice_select), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.message_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceColor)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Text(
                stringResource(R.string.reminder_voice_hint),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            PreviewVoices.ALL.forEach { voice ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onVoiceSelected(voice.key) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        voice.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (voice.key == currentVoiceKey) Primary else TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    if (voice.key == currentVoiceKey) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}
