package com.elderlycare.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elderlycare.app.ui.theme.TextSecondary

@Composable
fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
    labelWeight: Float = 0.35f
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.fillMaxWidth(labelWeight)
            )
            Text(
                text = value.ifEmpty { "未填写" },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (value.isNotEmpty()) FontWeight.Medium else FontWeight.Normal,
                color = if (value.isNotEmpty())
                    MaterialTheme.colorScheme.onSurface
                else
                    TextSecondary,
                modifier = Modifier.fillMaxWidth(1f - labelWeight)
            )
        }
        if (showDivider) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        }
    }
}
