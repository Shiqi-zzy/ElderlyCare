package com.elderlycare.app.ui.message

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elderlycare.app.R
import com.elderlycare.app.data.binding.BindingRepository
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.message.MessageEntity
import com.elderlycare.app.ui.shared.formatTimestamp
import com.elderlycare.app.ui.theme.Primary
import com.elderlycare.app.ui.theme.StatusGreen
import com.elderlycare.app.ui.theme.StatusRed
import com.elderlycare.app.ui.theme.Surface as SurfaceColor
import com.elderlycare.app.ui.theme.TextHint
import com.elderlycare.app.ui.theme.TextPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** 我方气泡底色（与主题主色一致）；对方气泡白色 */
private val BubbleMine = Color(0xFF4086E8)

/** 顶部横幅极浅蓝渐变（与其他页面统一） */
private val BannerBlueStart = Color(0xFFEAF2FF)
private val BannerBlueEnd = Color(0xFFF5F9FF)
private val BannerText = Color(0xFF1A2332)

/**
 * 聊天对话页：某发送方会话的全部历史消息（气泡对话 + 时间戳）。
 *
 * - 会话键 [conversationKey]（内存分组键）：设备消息「RK3(SN)」、我方「我」、他人按名称；
 * - 气泡区分：App 发送（文字/录音）在右（主色底白字），对方消息在左（白底深字）；
 * - 消息类型标签：留言（绿）/ 报警（红 + 警示标记）/ 健康建议（主题色）；
 * - 设备视频留言渲染缩略图 + 时长角标，点击跳视频播放页；
 * - 右上角【全部已读】仅把当前会话内全部消息置为 isRead=true（不影响其他会话）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    conversationKey: String,
    onBack: () -> Unit,
    onOpenVideo: (Long) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = ServiceLocator.messageRepository

    // 授权链路当前设备（响应式；无设备则空列表）
    var boundDevice by remember { mutableStateOf<BindingRepository.AccessibleDevice?>(null) }
    LaunchedEffect(Unit) {
        ServiceLocator.bindingRepository.observeCurrentUserDevice().collect { boundDevice = it }
    }
    val deviceSn = boundDevice?.deviceSn

    // 当前会话消息（时间升序；LazyColumn reverseLayout 让最新消息显示在底部）
    val allMessages by remember(deviceSn) {
        deviceSn?.let { repository.observeAllMessages(it) }
            ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val messages = allMessages
        .filter { MessageCenterViewModel.conversationKey(it) == conversationKey }
        .sortedBy { it.createTime }

    val listState = rememberLazyListState()

    Scaffold(
        containerColor = Color(0xFFF5F7FA)
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // 极浅蓝渐变顶部横幅
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(BannerBlueStart, BannerBlueEnd)
                        )
                    )
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = BannerText)
                    }
                    Text(
                        conversationKey,
                        fontWeight = FontWeight.SemiBold,
                        color = BannerText,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = {
                            val unreadIds = messages.filter { !it.isRead }.map { it.id }
                            scope.launch(Dispatchers.IO) {
                                repository.markMessagesRead(unreadIds)
                            }
                            Toast.makeText(context, "已全部标记已读", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("全部已读", color = Primary)
                    }
                }
            }

            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            painter = painterResource(R.drawable.ic_empty_state),
                            contentDescription = null,
                            modifier = Modifier.size(96.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("暂无消息", style = MaterialTheme.typography.bodyMedium, color = TextHint)
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    reverseLayout = true,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages, key = { "chat_${it.id}" }) { message ->
                        ChatBubble(
                            message = message,
                            onOpenVideo = { onOpenVideo(message.id) }
                        )
                    }
                }
            }
        }
    }
}

/** 单条聊天气泡：我方右对齐（主色底白字），对方左对齐（白底深字），附时间戳与类型标签 */
@Composable
private fun ChatBubble(message: MessageEntity, onOpenVideo: () -> Unit) {
    val isMine = message.msgType == MessageEntity.MSG_TYPE_TEXT ||
        message.msgType == MessageEntity.MSG_TYPE_RECORD

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MessageTypeTag(message.msgType)
            Spacer(Modifier.width(6.dp))
            Text(
                formatTimestamp(message.createTime, "MM-dd HH:mm"),
                style = MaterialTheme.typography.labelSmall,
                color = TextHint
            )
        }
        Spacer(Modifier.height(4.dp))
        // 气泡体
        Card(
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isMine) 14.dp else 4.dp,
                bottomEnd = if (isMine) 4.dp else 14.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isMine) BubbleMine else SurfaceColor
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            MessageContent(message = message, isMine = isMine, onOpenVideo = onOpenVideo)
        }
    }
}

/** 消息类型标签：留言（绿）/ 报警（红 + 警示图标）/ 健康建议（主题色）；报警带特殊标记 */
@Composable
private fun MessageTypeTag(msgType: Int) {
    val (label, color) = when (msgType) {
        MessageEntity.MSG_TYPE_ALERT -> "报警" to StatusRed
        MessageEntity.MSG_TYPE_ADVICE -> "健康建议" to Primary
        else -> "留言" to StatusGreen
    }
    Surface(shape = RoundedCornerShape(6.dp), color = color.copy(alpha = 0.12f)) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (msgType == MessageEntity.MSG_TYPE_ALERT) {
                Icon(
                    Icons.Filled.Warning,
                    null,
                    tint = color,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(Modifier.width(2.dp))
            }
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
        }
    }
}

/** 气泡内容：文字 / 录音时长 / 设备视频缩略图（点击播放）/ 报警文案 */
@Composable
private fun MessageContent(message: MessageEntity, isMine: Boolean, onOpenVideo: () -> Unit) {
    val textColor = if (isMine) Color.White else TextPrimary
    when (message.msgType) {
        MessageEntity.MSG_TYPE_RECORD -> {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Mic, null, tint = textColor, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("语音 ${message.duration}s", style = MaterialTheme.typography.bodyMedium, color = textColor)
            }
        }
        MessageEntity.MSG_TYPE_DEVICE -> {
            // 设备视频留言：缩略图 + 时长角标，点击跳视频播放页
            Box(modifier = Modifier.padding(4.dp).clip(RoundedCornerShape(10.dp)).clickable(onClick = onOpenVideo)) {
                VideoThumbnail(
                    thumbUrl = message.thumbUrl,
                    localVideoPath = message.localVideoPath,
                    modifier = Modifier
                        .widthIn(max = 260.dp)
                        .height(140.dp)
                )
                Surface(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.55f)
                ) {
                    Text(
                        "${message.duration}s",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
        }
        else -> {
            // 文字留言 / 报警 / 健康建议：文本内容
            Text(
                message.content.ifBlank {
                    when (message.msgType) {
                        MessageEntity.MSG_TYPE_ALERT -> "设备告警"
                        MessageEntity.MSG_TYPE_ADVICE -> "健康建议"
                        else -> "留言"
                    }
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
        }
    }
}
