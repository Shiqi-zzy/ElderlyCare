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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.elderlycare.app.R
import com.elderlycare.app.data.message.MessageEntity
import com.elderlycare.app.ui.shared.formatTimestamp
import com.elderlycare.app.ui.theme.Primary
import com.elderlycare.app.ui.theme.Surface as SurfaceColor
import com.elderlycare.app.ui.theme.TextHint
import com.elderlycare.app.ui.theme.TextPrimary
import com.elderlycare.app.ui.theme.TextSecondary

/** 会话头像色板（设备蓝 / 我方绿 / 他人橙，Material 风格） */
private val AvatarDevice = Color(0xFF4086E8)
private val AvatarMe = Color(0xFF42BD67)
private val AvatarOther = Color(0xFFFF9F38)
private val BadgeRed = Color(0xFFF24848)

/**
 * 消息中心（会话列表模式，对标萤石对话会话列表；家属端底部「消息」Tab）。
 *
 * 会话按发送方在内存聚合（MessageCenterViewModel.groupConversations，不新增数据表）：
 * 设备消息统一归「RK3(SN)」会话，App 发送归「我」，其他人员按名称。
 * 顶部固定标题「消息中心」，右上【全部已读】全局清零 +【去留言】；
 * 点击会话条目进入对应聊天对话页（ChatConversationScreen）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageCenterScreen(
    onOpenConversation: (String) -> Unit,
    onOpenVideo: (Long) -> Unit,
    onOpenLeave: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: MessageCenterViewModel = viewModel()

    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.consumeToast()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("消息中心", fontWeight = FontWeight.SemiBold) },
                actions = {
                    TextButton(onClick = { viewModel.markAllRead() }) {
                        Text("全部已读", color = Primary)
                    }
                    TextButton(onClick = onOpenLeave) {
                        Text("去留言", color = Primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceColor)
            )
        }
    ) { paddingValues ->
        if (conversations.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(R.drawable.ic_empty_state),
                        contentDescription = null,
                        modifier = Modifier.size(96.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "暂无会话消息",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextHint
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(conversations, key = { "conv_${it.key}" }) { conversation ->
                    ConversationRow(
                        conversation = conversation,
                        onClick = { onOpenConversation(conversation.key) }
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

/** 会话条目：左侧头像 + 中间标题/最新预览摘要 + 右侧时间/未读红点角标 */
@Composable
private fun ConversationRow(
    conversation: MessageCenterViewModel.Conversation,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧头像：设备会话用设备图标，人员会话用用户头像（首字圆形色块）
            Surface(
                shape = CircleShape,
                color = when {
                    conversation.isDevice -> AvatarDevice
                    conversation.title == "我" -> AvatarMe
                    else -> AvatarOther
                },
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (conversation.isDevice) {
                        Icon(Icons.Filled.Videocam, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    } else if (conversation.title == "我") {
                        Icon(Icons.Filled.Person, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    } else {
                        Text(
                            conversation.title.take(1),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            // 中间：会话标题 + 最新一条消息预览摘要（单行省略）
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    conversation.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    previewText(conversation.latest),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))

            // 右侧：上方最新消息时间，下方未读红点角标（未读数 > 0 显示）
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatTimestamp(conversation.latest.createTime, "MM-dd HH:mm"),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextHint
                )
                if (conversation.unread > 0) {
                    Spacer(Modifier.height(5.dp))
                    Surface(shape = CircleShape, color = BadgeRed) {
                        Text(
                            if (conversation.unread > 99) "99+" else "${conversation.unread}",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/** 最新消息预览摘要（会话列表第二行） */
private fun previewText(message: MessageEntity): String = when (message.msgType) {
    MessageEntity.MSG_TYPE_RECORD -> "语音留言 ${message.duration}s"
    MessageEntity.MSG_TYPE_DEVICE -> if (message.content.isNotBlank()) message.content
    else "视频留言 ${message.duration}s"
    MessageEntity.MSG_TYPE_ALERT -> message.content.ifBlank { "报警消息" }
    MessageEntity.MSG_TYPE_ADVICE -> message.content.ifBlank { "健康建议" }
    else -> message.content.ifBlank { "留言" }
}
