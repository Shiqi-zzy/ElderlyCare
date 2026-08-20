package com.elderlycare.app.ui.message

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.elderlycare.app.R
import com.elderlycare.app.data.ezviz.VoiceCallState
import com.elderlycare.app.data.message.MessageEntity
import com.elderlycare.app.data.reminder.RemindPlanEntity
import com.elderlycare.app.ui.reminder.remindPlanTimeRepeatText
import com.elderlycare.app.ui.theme.Primary
import com.elderlycare.app.ui.theme.StatusGreen
import com.elderlycare.app.ui.theme.StatusRed
import com.elderlycare.app.ui.theme.Surface as SurfaceColor
import com.elderlycare.app.ui.theme.TextHint
import com.elderlycare.app.ui.theme.TextPrimary
import com.elderlycare.app.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 留言页（音频收发模块）。
 *
 * 布局：顶部标题 + 提醒计划入口；中部提示语 + 语音通话状态条 +
 * 混合 feed（留言 + 系统消息 + 提醒计划，空态/列表态）；
 * 底部「文字留言」+「按住留言」（按住录音，上滑取消，松开自动双通道发送）。
 *
 * 留言方向区分（对齐萤石原生 App）：
 * - 设备发来（msgType=3）：EZOpenSDK 微聊公开接口拉取（RK3 按键录音），
 *   展示「设备留言」标签 + 发送时间 + 未读蓝点，点播放标记已读，长按删除（本地+云端）；
 * - 手机发出（文字/录音留言）：本地记录，展示发送状态（绿勾/红叉+失败原因）与通路标签。
 *   录音留言双通道发送：①EZOpenSDK 语音通话（实时对讲）②云广播 REST（录音文件下发）。
 *
 * 列表项：发送人/时间、文字内容（文字留言）、播放按钮 + 时长 + 发送状态图标 + 通路标签；
 * 点击播放并标记已读，长按删除（本地记录 + 音频文件 + 云端留言）。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MessageScreen(onBack: () -> Unit, onNavigateToRemindPlan: () -> Unit) {
    val context = LocalContext.current
    val viewModel: MessageViewModel = viewModel()

    val feed by viewModel.feed.collectAsStateWithLifecycle()
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val recordCancelled by viewModel.recordCancelled.collectAsStateWithLifecycle()
    val recordElapsed by viewModel.recordElapsed.collectAsStateWithLifecycle()
    val recordAmplitude by viewModel.recordAmplitude.collectAsStateWithLifecycle()
    val playingId by viewModel.playingId.collectAsStateWithLifecycle()
    val playbackProgress by viewModel.playbackProgress.collectAsStateWithLifecycle()
    val voiceCallState by viewModel.voiceCallState.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    var showTextDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<MessageEntity?>(null) }

    // 时间格式化（列表统一 MM-dd HH:mm）
    val timeFormat = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }

    // Toast 展示
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.consumeToast()
        }
    }

    // 录音权限
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.startRecording()
        else viewModel.toast(R.string.message_permission_denied)
    }

    val onPressStart: () -> Unit = {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) viewModel.startRecording()
        else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    // 上滑取消阈值（80dp）
    val cancelThresholdPx = with(LocalDensity.current) { 80.dp.toPx() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.message_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.message_back)
                        )
                    }
                },
                actions = {
                    // 提醒计划：设备本地闹铃计划列表
                    TextButton(onClick = onNavigateToRemindPlan) {
                        Icon(Icons.AutoMirrored.Filled.EventNote, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.message_reminder_plan))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceColor)
            )
        },
        bottomBar = {
            MessageBottomBar(
                onTextClick = { showTextDialog = true },
                onPressStart = onPressStart,
                onPressRelease = { viewModel.finishRecording() },
                onSlideUp = { viewModel.cancelRecording() },
                cancelThresholdPx = cancelThresholdPx
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 提示语
                Text(
                    stringResource(R.string.message_hint_text),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                // 语音通话状态条（通路① 通话中/呼叫中显示）
                if (voiceCallState is VoiceCallState.Calling || voiceCallState is VoiceCallState.Connected) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = Primary.copy(alpha = 0.08f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.message_voice_call_active),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Primary
                            )
                            TextButton(onClick = { viewModel.endVoiceCall() }) {
                                Text(stringResource(R.string.message_end_call), color = StatusRed)
                            }
                        }
                    }
                }

                // 混合 feed（留言 + 系统消息 + 提醒计划）/ 空态
                if (feed.isEmpty()) {
                    EmptyState(modifier = Modifier.weight(1f))
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // key 加类型前缀：两表 Long 自增 id 会撞
                        items(
                            feed,
                            key = {
                                when (it) {
                                    is MessageFeedItem.Msg -> "msg_${it.message.id}"
                                    is MessageFeedItem.Plan -> "plan_${it.plan.id}"
                                }
                            }
                        ) { item ->
                            when (item) {
                                is MessageFeedItem.Msg -> {
                                    val message = item.message
                                    if (message.msgType == MessageEntity.MSG_TYPE_SYSTEM) {
                                        // 系统消息（提醒计划播报完成）：静态灰字，无蓝点/播放/状态/通路
                                        SystemMessageCard(
                                            message = message,
                                            timeText = timeFormat.format(Date(message.createTime))
                                        )
                                    } else {
                                        MessageRow(
                                            message = message,
                                            isPlaying = playingId == message.id,
                                            progress = if (playingId == message.id) playbackProgress else 0f,
                                            timeText = timeFormat.format(Date(message.createTime)),
                                            onClick = { viewModel.togglePlay(message) },
                                            onLongClick = { deleteTarget = message }
                                        )
                                    }
                                }
                                is MessageFeedItem.Plan -> RemindPlanRow(plan = item.plan)
                            }
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                    }
                }
            }

            // 录音遮罩
            RecordOverlay(
                isRecording = isRecording,
                cancelled = recordCancelled,
                amplitude = recordAmplitude,
                elapsedSec = recordElapsed
            )
        }
    }

    // 文字留言弹窗
    if (showTextDialog) {
        TextMessageDialog(
            onDismiss = { showTextDialog = false },
            onConfirm = { text ->
                showTextDialog = false
                viewModel.sendText(text)
            }
        )
    }

    // 删除确认
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.message_delete_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(target)
                    deleteTarget = null
                }) { Text(stringResource(R.string.message_delete), color = StatusRed) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(R.string.message_text_cancel))
                }
            }
        )
    }
}

/** 底部操作栏：文字留言 + 按住留言（按住录音，上滑取消，松开自动双通道发送） */
@Composable
private fun MessageBottomBar(
    onTextClick: () -> Unit,
    onPressStart: () -> Unit,
    onPressRelease: () -> Unit,
    onSlideUp: () -> Unit,
    cancelThresholdPx: Float
) {
    Surface(color = SurfaceColor) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onTextClick,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.message_text_entry))
            }

            Button(
                // 按住录音：按下开始、松开发送、上滑取消（手势被 pointerInput 消费，onClick 不会触发）
                onClick = {},
                modifier = Modifier
                    .weight(1f)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            onPressStart()
                            var cancelled = false
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id }
                                if (change == null || !change.pressed) break
                                change.consume()
                                if (!cancelled &&
                                    change.position.y - down.position.y < -cancelThresholdPx
                                ) {
                                    cancelled = true
                                    onSlideUp()
                                }
                            }
                            if (!cancelled) onPressRelease()
                        }
                    }
            ) {
                Icon(Icons.Filled.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.message_hold_to_talk))
            }
        }
    }
}

/** 空态 */
@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.MailOutline,
            contentDescription = null,
            tint = TextHint,
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.message_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = TextHint
        )
    }
}

/** 单条留言 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageRow(
    message: MessageEntity,
    isPlaying: Boolean,
    progress: Float,
    timeText: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp)) {
            // 未读蓝点
            if (!message.isRead) {
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp, end = 8.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Primary)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                // 发送人 + 时间
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        message.senderName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    // 设备发来的留言：方向标签（对齐原生「设备」标识）
                    if (message.msgType == MessageEntity.MSG_TYPE_DEVICE) {
                        Spacer(Modifier.width(6.dp))
                        Surface(shape = RoundedCornerShape(6.dp), color = TextHint.copy(alpha = 0.10f)) {
                            Text(
                                stringResource(R.string.message_type_device),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                    Text(timeText, style = MaterialTheme.typography.labelSmall, color = TextHint)
                }

                // 文字留言展示内容
                if (message.msgType == MessageEntity.MSG_TYPE_TEXT && message.content.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                Spacer(Modifier.height(6.dp))

                // 播放 + 时长 + 状态 + 通路
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClick, modifier = Modifier.size(30.dp)) {
                        Icon(
                            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = stringResource(
                                if (isPlaying) R.string.message_pause else R.string.message_play
                            ),
                            tint = Primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        stringResource(R.string.message_record_duration_format, message.duration),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Spacer(Modifier.width(10.dp))

                    // 发送状态图标（仅 App 发送的留言）
                    if (message.msgType != MessageEntity.MSG_TYPE_DEVICE) {
                        when (message.sendStatus) {
                            MessageEntity.SEND_STATUS_SENDING -> CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = Primary
                            )
                            MessageEntity.SEND_STATUS_SUCCESS -> Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = stringResource(R.string.message_send_success),
                                tint = StatusGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            else -> Icon(
                                Icons.Filled.ErrorOutline,
                                contentDescription = stringResource(R.string.message_send_failed),
                                tint = StatusRed,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        // 通路标签
                        val channelRes = when (message.sendChannel) {
                            MessageEntity.CHANNEL_TALK -> R.string.message_channel_talk
                            MessageEntity.CHANNEL_BROADCAST -> R.string.message_channel_broadcast
                            else -> R.string.message_channel_both
                        }
                        Surface(shape = RoundedCornerShape(6.dp), color = Primary.copy(alpha = 0.08f)) {
                            Text(
                                stringResource(channelRes),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Primary
                            )
                        }
                    }
                }

                // 失败原因
                if (message.sendStatus == MessageEntity.SEND_STATUS_FAILED && message.failReason.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        message.failReason,
                        style = MaterialTheme.typography.labelSmall,
                        color = StatusRed
                    )
                }

                // 播放进度
                if (isPlaying) {
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(3.dp).clip(CircleShape),
                        color = Primary,
                        trackColor = Primary.copy(alpha = 0.12f)
                    )
                }
            }
        }
    }
}

/** 系统消息（提醒计划播报完成等）：静态灰字卡片，无蓝点/播放/状态图标/通路标签 */
@Composable
private fun SystemMessageCard(message: MessageEntity, timeText: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    message.senderName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                Text(timeText, style = MaterialTheme.typography.labelSmall, color = TextHint)
            }
            if (message.content.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

/** 提醒计划条目（feed 内静态展示：无播放、无长按） */
@Composable
private fun RemindPlanRow(plan: RemindPlanEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.AutoMirrored.Filled.EventNote,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                // 标题 + 已播报完成标签
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        plan.tag,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    if (plan.executed == RemindPlanEntity.EXECUTED_YES) {
                        Spacer(Modifier.width(8.dp))
                        Surface(shape = RoundedCornerShape(6.dp), color = StatusGreen.copy(alpha = 0.12f)) {
                            Text(
                                stringResource(R.string.reminder_executed),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusGreen
                            )
                        }
                    }
                }
                if (plan.content.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        plan.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    remindPlanTimeRepeatText(plan),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextHint
                )
            }
        }
    }
}
