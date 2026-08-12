package com.ezvizpro.ui.reminder

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ezvizpro.core.local.LifeReminder
import com.ezvizpro.core.local.ReminderType
import com.ezvizpro.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifeReminderScreen(
    onBackClick: () -> Unit,
    viewModel: LifeReminderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("生活提醒") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, "添加提醒")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        if (uiState.reminders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Notifications,
                        null,
                        modifier = Modifier.size(64.dp),
                        tint = Gray600.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("暂无提醒", color = Gray600)
                    Text("点击右上角 + 添加提醒", style = MaterialTheme.typography.bodySmall, color = Gray600)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.reminders, key = { it.id }) { reminder ->
                    ReminderCard(
                        reminder = reminder,
                        onToggle = { viewModel.toggleReminder(reminder) },
                        onDelete = { viewModel.deleteReminder(reminder.id) }
                    )
                }
            }
        }
    }

    // 添加提醒弹窗
    if (showAddDialog) {
        AddReminderDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { title, type ->
                viewModel.addReminder(title, type)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun ReminderCard(
    reminder: LifeReminder,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (reminder.enabled)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 类型图标
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(10.dp),
                color = when (reminder.type) {
                    ReminderType.MEDICINE -> Red500.copy(alpha = 0.15f)
                    ReminderType.EXERCISE -> Green500.copy(alpha = 0.15f)
                    ReminderType.WATER -> Blue500.copy(alpha = 0.15f)
                    ReminderType.APPOINTMENT -> Orange500.copy(alpha = 0.15f)
                    ReminderType.OTHER -> Gray600.copy(alpha = 0.15f)
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        when (reminder.type) {
                            ReminderType.MEDICINE -> Icons.Default.Medication
                            ReminderType.EXERCISE -> Icons.AutoMirrored.Filled.DirectionsRun
                            ReminderType.WATER -> Icons.Default.WaterDrop
                            ReminderType.APPOINTMENT -> Icons.Default.Event
                            ReminderType.OTHER -> Icons.Default.Notifications
                        },
                        null,
                        tint = when (reminder.type) {
                            ReminderType.MEDICINE -> Red500
                            ReminderType.EXERCISE -> Green500
                            ReminderType.WATER -> Blue500
                            ReminderType.APPOINTMENT -> Orange500
                            ReminderType.OTHER -> Gray600
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    reminder.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (reminder.enabled) MaterialTheme.colorScheme.onBackground
                    else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = when (reminder.type) {
                            ReminderType.MEDICINE -> Red500.copy(alpha = 0.15f)
                            else -> Blue500.copy(alpha = 0.1f)
                        }
                    ) {
                        Text(
                            reminder.type.label,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = when (reminder.type) {
                                ReminderType.MEDICINE -> Red500
                                else -> Blue500
                            }
                        )
                    }
                    if (reminder.time.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            reminder.time,
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray600
                        )
                    }
                }
            }

            Switch(
                checked = reminder.enabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Blue500,
                    checkedTrackColor = Blue500.copy(alpha = 0.3f)
                )
            )

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Delete,
                    "删除",
                    tint = Gray600,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddReminderDialog(
    onDismiss: () -> Unit,
    onAdd: (String, ReminderType) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(ReminderType.MEDICINE) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加提醒") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("提醒内容") },
                    placeholder = { Text("例如：吃降压药") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("提醒类型", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReminderType.entries.take(4).forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (title.isNotBlank()) onAdd(title.trim(), selectedType) },
                enabled = title.isNotBlank()
            ) { Text("添加") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
