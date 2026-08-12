package com.ezvizpro.ui.alarm

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ezvizpro.domain.model.AlarmMessage
import com.ezvizpro.domain.model.AlarmType
import com.ezvizpro.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    onAlarmClick: (AlarmMessage) -> Unit,
    viewModel: MessagesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedMessage by remember { mutableStateOf<AlarmMessage?>(null) }
    val pullRefreshState = rememberPullToRefreshState()

    // 同步刷新状态
    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.refresh()
            pullRefreshState.endRefresh()
        }
    }
    LaunchedEffect(uiState.isRefreshing) {
        if (!uiState.isRefreshing && pullRefreshState.isRefreshing) {
            pullRefreshState.endRefresh()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("报警消息")
                    if (uiState.unreadCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.error
                        ) {
                            Text(
                                "${uiState.unreadCount}",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onError
                            )
                        }
                    }
                }
            },
            actions = {
                IconButton(onClick = { viewModel.refresh() }) {
                    Icon(Icons.Default.Refresh, "刷新")
                }
            }
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(pullRefreshState.nestedScrollConnection)
        ) {
            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.error != null && uiState.messages.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.loadMessages() }) {
                                Text("重试")
                            }
                        }
                    }
                }
                uiState.messages.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Notifications,
                                null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("暂无报警消息", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        val groups = listOf("今天", "昨天", "更早")
                        groups.forEach { group ->
                            val groupMessages = uiState.groupedMessages[group] ?: emptyList()
                            if (groupMessages.isNotEmpty()) {
                                item {
                                    Text(
                                        text = group,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Gray600,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                                items(
                                    items = groupMessages,
                                    key = { it.alarmId }
                                ) { message ->
                                    AlarmItem(
                                        message = message,
                                        onClick = {
                                            selectedMessage = message
                                            viewModel.markAsRead(message.alarmId)
                                            onAlarmClick(message)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            PullToRefreshContainer(
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }

    selectedMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { selectedMessage = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Warning,
                        null,
                        tint = when (AlarmType.fromCode(message.alarmType)) {
                            AlarmType.MOTION_DETECT -> Orange500
                            AlarmType.HUMAN_DETECT -> Red500
                            else -> Red500
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(message.alarmName)
                }
            },
            text = {
                Column {
                    if (message.alarmPicUrl != null) {
                        AsyncImage(
                            model = message.alarmPicUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.FillWidth
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    InfoRow("设备", message.deviceName ?: message.deviceSerial)
                    InfoRow("时间", message.alarmTime)
                    InfoRow("类型", AlarmType.fromCode(message.alarmType).label)
                    if (message.alarmVideoUrl != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("有相关视频可查看", style = MaterialTheme.typography.bodySmall, color = Blue500)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedMessage = null }) { Text("关闭") }
            }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text("$label:  ", style = MaterialTheme.typography.bodyMedium, color = Gray600, modifier = Modifier.width(56.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun AlarmItem(message: AlarmMessage, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = if (!message.isRead) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
        else MaterialTheme.colorScheme.surface
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(8.dp),
                color = when (AlarmType.fromCode(message.alarmType)) {
                    AlarmType.MOTION_DETECT -> Orange100
                    AlarmType.HUMAN_DETECT -> Red100
                    else -> Gray100
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        when (AlarmType.fromCode(message.alarmType)) {
                            AlarmType.MOTION_DETECT -> Icons.Default.Warning
                            AlarmType.HUMAN_DETECT -> Icons.Default.Person
                            else -> Icons.Default.Warning
                        },
                        contentDescription = null,
                        tint = when (AlarmType.fromCode(message.alarmType)) {
                            AlarmType.MOTION_DETECT -> Orange500
                            AlarmType.HUMAN_DETECT -> Red500
                            else -> Gray600
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = message.alarmName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (!message.isRead) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = message.formattedTime.ifBlank { message.alarmTime.takeLast(8) },
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray600
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message.deviceName ?: message.deviceSerial,
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray600,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (message.alarmPicUrl != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    AsyncImage(
                        model = message.alarmPicUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(6.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                if (!message.isRead) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Red500))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("未读", style = MaterialTheme.typography.labelSmall, color = Red500)
                    }
                }
            }
        }
    }
}
