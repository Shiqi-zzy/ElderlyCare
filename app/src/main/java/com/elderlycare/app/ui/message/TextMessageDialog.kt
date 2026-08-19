package com.elderlycare.app.ui.message

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.elderlycare.app.R
import com.elderlycare.app.ui.theme.Primary
import com.elderlycare.app.ui.theme.TextHint

/**
 * 文字留言弹窗。
 * 80 字上限 + 实时计数（0/80）；确认后由 TTS 自动转为语音下发（提示文案见 message_text_ai_tag）。
 */
@Composable
fun TextMessageDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.message_text_entry)) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(MAX_TEXT_LEN) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.message_text_input_hint)) },
                    maxLines = 4
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // AI 转语音提示标签
                    Surface(shape = RoundedCornerShape(6.dp), color = Primary.copy(alpha = 0.08f)) {
                        Text(
                            stringResource(R.string.message_text_ai_tag),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Primary
                        )
                    }
                    Text(
                        stringResource(R.string.message_text_count_format, text.length),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextHint
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = text.isNotBlank(),
                onClick = { onConfirm(text) }
            ) { Text(stringResource(R.string.message_text_done)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.message_text_cancel)) }
        }
    )
}

private const val MAX_TEXT_LEN = 80
