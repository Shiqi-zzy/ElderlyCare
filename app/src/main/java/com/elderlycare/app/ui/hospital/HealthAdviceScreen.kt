package com.elderlycare.app.ui.hospital

import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elderlycare.app.R
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.hospital.HealthAdvice
import com.elderlycare.app.ui.shared.formatTimestamp
import com.elderlycare.app.ui.theme.Primary
import com.elderlycare.app.ui.theme.Surface as SurfaceColor
import com.elderlycare.app.ui.theme.TextHint
import com.elderlycare.app.ui.theme.TextPrimary
import com.elderlycare.app.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 医院端「健康建议录入」（仅前端，本地 Room）。
 *
 * 建议列表（时间 + 内容）+ 底部录入区（多行文本框 + 提交按钮）。
 * 提交：①插入 health_advice 表；②插入 message 表一条 MSG_TYPE_ADVICE 消息
 * （家属端留言页/消息中心渲染独立气泡）。
 * 约束：健康建议**不走萤石设备播报**（不调 clock/语音接口），仅 App 消息模块查看。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthAdviceScreen(
    elderlyId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val adviceDao = remember { ServiceLocator.appDatabase.healthAdviceDao() }

    var elderlyName by remember { mutableStateOf<String?>(null) }
    var staffName by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(elderlyId) {
        elderlyName = ServiceLocator.profileStore.getPrimaryProfile(elderlyId)?.name
        staffName = ServiceLocator.staffUserStore.getCurrentStaffUser()?.name
    }

    val adviceList by remember(elderlyId) {
        adviceDao.observeByElderlyId(elderlyId)
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    var draft by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var savedToast by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(savedToast) {
        savedToast?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            savedToast = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.hospital_advice_title), fontWeight = FontWeight.SemiBold)
                        if (!elderlyName.isNullOrBlank()) {
                            Text(
                                elderlyName.orEmpty(),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextHint
                            )
                        }
                    }
                },
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
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // ===== 建议列表 =====
            if (adviceList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.hospital_advice_empty),
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(adviceList, key = { it.id }) { advice ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = Primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        formatTimestamp(advice.adviceTime),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextHint
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    advice.adviceContent,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }

            // ===== 录入区 =====
            Surface(color = SurfaceColor, shadowElevation = 8.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        label = { Text(stringResource(R.string.hospital_advice_input_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.hospital_advice_input_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextHint
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val content = draft.trim()
                            if (content.isEmpty()) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.hospital_advice_content_required),
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }
                            if (submitting) return@Button
                            submitting = true
                            scope.launch {
                                try {
                                    val deviceSerial = withContext(Dispatchers.IO) {
                                        ServiceLocator.profileStore
                                            .getPrimaryProfile(elderlyId)?.deviceSn.orEmpty()
                                    }
                                    withContext(Dispatchers.IO) {
                                        adviceDao.insert(
                                            HealthAdvice(
                                                elderlyId = elderlyId,
                                                adviceTime = System.currentTimeMillis(),
                                                adviceContent = content
                                            )
                                        )
                                        // 同步落一条家属端消息（MSG_TYPE_ADVICE 独立气泡；
                                        // 不走萤石设备播报）。老人未绑定设备则不推消息。
                                        ServiceLocator.messageRepository.saveHealthAdviceMessage(
                                            deviceSerial = deviceSerial,
                                            senderName = staffName.orEmpty(),
                                            adviceContent = content
                                        )
                                    }
                                    savedToast = context.getString(
                                        if (deviceSerial.isBlank())
                                            R.string.hospital_advice_saved_no_device
                                        else R.string.hospital_advice_saved
                                    )
                                    draft = ""
                                } finally {
                                    submitting = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !submitting,
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Text(
                            stringResource(R.string.hospital_advice_submit),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
