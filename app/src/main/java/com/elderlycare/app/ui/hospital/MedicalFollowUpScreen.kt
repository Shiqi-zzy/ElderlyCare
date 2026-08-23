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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elderlycare.app.R
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.hospital.MedicalFollowUpRecord
import com.elderlycare.app.ui.shared.formatTimestamp
import com.elderlycare.app.ui.theme.Primary
import com.elderlycare.app.ui.theme.StatusGreen
import com.elderlycare.app.ui.theme.StatusYellow
import com.elderlycare.app.ui.theme.Surface as SurfaceColor
import com.elderlycare.app.ui.theme.TextHint
import com.elderlycare.app.ui.theme.TextPrimary
import com.elderlycare.app.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 医院端「医疗随访记录」（仅前端，本地 Room）。
 *
 * elderlyId 为空 = 全部随访记录视图（只读，隐藏录入入口）；
 * 非空 = 指定老人（从患者列表「随访」入口进入），支持浮动按钮录入。
 * 录入表单：多行文本 + 状态选择器（待处理/已完成）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalFollowUpScreen(
    elderlyId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val followUpDao = remember { ServiceLocator.appDatabase.medicalFollowUpDao() }

    var elderlyName by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(elderlyId) {
        if (elderlyId.isNotBlank()) {
            elderlyName = ServiceLocator.profileStore.getPrimaryProfile(elderlyId)?.name
        }
    }

    // 空 elderlyId = 全部记录；非空 = 按老人过滤（Room Flow 实时刷新）
    val records by remember(elderlyId) {
        if (elderlyId.isBlank()) followUpDao.observeAll()
        else followUpDao.observeByElderlyId(elderlyId)
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    // ===== 录入表单弹窗状态 =====
    var showDialog by remember { mutableStateOf(false) }
    var draftContent by remember { mutableStateOf("") }
    var draftStatus by remember { mutableStateOf(MedicalFollowUpRecord.STATUS_PENDING) }
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
                        Text(stringResource(R.string.hospital_follow_up_title), fontWeight = FontWeight.SemiBold)
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
        },
        floatingActionButton = {
            // 全部记录视图不提供录入入口（需先定位到具体老人）
            if (elderlyId.isNotBlank()) {
                FloatingActionButton(
                    onClick = {
                        draftContent = ""
                        draftStatus = MedicalFollowUpRecord.STATUS_PENDING
                        showDialog = true
                    },
                    containerColor = Primary,
                    contentColor = androidx.compose.ui.graphics.Color.White
                ) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.hospital_follow_up_add))
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (elderlyId.isBlank()) {
                Surface(
                    shape = RoundedCornerShape(0.dp),
                    color = Primary.copy(alpha = 0.06f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(R.string.hospital_follow_up_all_hint),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = Primary
                    )
                }
            }
            if (records.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.hospital_follow_up_empty),
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(records, key = { it.id }) { record ->
                        FollowUpRecordRow(record)
                    }
                }
            }
        }
    }

    // 录入表单对话框：多行文本 + 状态选择器（待处理/已完成）
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.hospital_follow_up_add), fontWeight = FontWeight.SemiBold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = draftContent,
                        onValueChange = { draftContent = it },
                        label = { Text(stringResource(R.string.hospital_follow_up_content_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        maxLines = 8
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        stringResource(R.string.hospital_follow_up_status),
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = draftStatus == MedicalFollowUpRecord.STATUS_PENDING,
                            onClick = { draftStatus = MedicalFollowUpRecord.STATUS_PENDING },
                            label = { Text(stringResource(R.string.hospital_follow_up_status_pending)) },
                            shape = RoundedCornerShape(16.dp)
                        )
                        FilterChip(
                            selected = draftStatus == MedicalFollowUpRecord.STATUS_DONE,
                            onClick = { draftStatus = MedicalFollowUpRecord.STATUS_DONE },
                            label = { Text(stringResource(R.string.hospital_follow_up_status_done)) },
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val content = draftContent.trim()
                        if (content.isEmpty()) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.hospital_follow_up_content_required),
                                Toast.LENGTH_SHORT
                            ).show()
                            return@TextButton
                        }
                        showDialog = false
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                followUpDao.insert(
                                    MedicalFollowUpRecord(
                                        elderlyId = elderlyId,
                                        followUpTime = System.currentTimeMillis(),
                                        content = content,
                                        status = draftStatus
                                    )
                                )
                            }
                            savedToast = context.getString(R.string.hospital_follow_up_saved)
                        }
                    }
                ) { Text(stringResource(R.string.reminder_save), color = Primary) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.message_text_cancel))
                }
            }
        )
    }
}

/** 单条随访记录：时间 + 状态 chip + 内容 */
@Composable
private fun FollowUpRecordRow(record: MedicalFollowUpRecord) {
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
                    formatTimestamp(record.followUpTime),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextHint
                )
                val isDone = record.status == MedicalFollowUpRecord.STATUS_DONE
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isDone) StatusGreen.copy(alpha = 0.12f) else StatusYellow.copy(alpha = 0.16f)
                ) {
                    Text(
                        record.status,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDone) StatusGreen else StatusYellow
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                record.content,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )
        }
    }
}
