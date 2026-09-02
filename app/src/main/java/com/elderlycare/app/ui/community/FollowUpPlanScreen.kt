package com.elderlycare.app.ui.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elderlycare.app.data.binding.BindingRepository
import com.elderlycare.app.data.community.CommunityFollowUpRecord
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.model.AppUser
import com.elderlycare.app.ui.components.StatusBadge
import com.elderlycare.app.ui.shared.formatTimestamp
import com.elderlycare.app.ui.theme.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/** 随访计划页色值（薄荷绿主题） */
private val PageBg = Color(0xFFF5FAF7)
private val MintGreen = Color(0xFF4CAF8A)
private val CardWhite = Color.White
private val TextDark = Color(0xFF1A2E25)
private val TextGray = Color(0xFF6B7C74)
private val TextHint = Color(0xFF9AA8A2)

/**
 * 社区端「随访计划」页面：选择老人添加随访 + 随访列表 + 完成标记。
 * 完成随访后自动写入服务记录，并生成/完成待办事项。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowUpPlanScreen(onNavigateBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var staff by remember { mutableStateOf<AppUser?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { staff = ServiceLocator.staffUserStore.getCurrentStaffUser() }

    val followUps by remember(staff?.phone) {
        if (staff != null) ServiceLocator.communityRepository.observeFollowUps(staff!!.phone)
        else flowOf(emptyList())
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    val pending = followUps.filter { it.status == CommunityFollowUpRecord.STATUS_PENDING }
    val done = followUps.filter { it.status == CommunityFollowUpRecord.STATUS_DONE }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("随访计划", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Filled.Add, "添加随访")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(PageBg)
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 待处理随访
            item { Text("待处理 (${pending.size})", color = TextDark, fontWeight = FontWeight.SemiBold, fontSize = 16.sp) }
            if (pending.isEmpty()) {
                item { Text("暂无待处理随访", color = TextHint, fontSize = 13.sp) }
            }
            items(pending, key = { it.id }) { item ->
                FollowUpCard(
                    record = item,
                    onComplete = {
                        scope.launch {
                            staff?.let { s ->
                                ServiceLocator.communityRepository.completeFollowUp(
                                    id = item.id,
                                    staffId = s.phone,
                                    elderlyId = item.elderlyId,
                                    elderlyName = item.elderlyName,
                                    followUpType = item.followUpType,
                                    content = item.content
                                )
                            }
                        }
                    }
                )
            }

            // 已完成随访
            if (done.isNotEmpty()) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                item { Text("已完成 (${done.size})", color = TextGray, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }
                items(done, key = { it.id }) { item ->
                    FollowUpCard(record = item, onComplete = null)
                }
            }
        }
    }

    if (showAddDialog) {
        AddFollowUpDialog(
            staff = staff,
            onDismiss = { showAddDialog = false },
            onConfirm = { elderlyId, elderlyName, type, time, content ->
                scope.launch {
                    staff?.let { s ->
                        ServiceLocator.communityRepository.createFollowUp(
                            staffId = s.phone,
                            elderlyId = elderlyId,
                            elderlyName = elderlyName,
                            followUpType = type,
                            scheduledTime = time,
                            content = content
                        )
                    }
                    showAddDialog = false
                }
            }
        )
    }
}

/** 随访卡片 */
@Composable
private fun FollowUpCard(record: CommunityFollowUpRecord, onComplete: (() -> Unit)?) {
    val isDone = record.status == CommunityFollowUpRecord.STATUS_DONE
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(9.dp)).background(MintGreen.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.FactCheck, null, tint = MintGreen, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("${record.followUpType} - ${record.elderlyName}", color = TextDark, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("计划时间：${formatTimestamp(record.scheduledTime)}", color = TextGray, fontSize = 12.sp)
                }
                StatusBadge(text = record.status, color = if (isDone) StatusGreen else StatusYellow)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("随访内容：${record.content}", color = TextGray, fontSize = 12.sp)
            if (isDone && record.completedAt != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("完成时间：${formatTimestamp(record.completedAt)}", color = TextHint, fontSize = 11.sp)
            }
            if (onComplete != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onComplete,
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MintGreen, contentColor = Color.White)
                ) {
                    Icon(Icons.Filled.Check, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("标记完成", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

/** 添加随访对话框 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFollowUpDialog(
    staff: AppUser?,
    onDismiss: () -> Unit,
    onConfirm: (elderlyId: String, elderlyName: String, type: String, time: Long, content: String) -> Unit
) {
    var elderly by remember { mutableStateOf<BindingRepository.AccessibleElderlyUi?>(null) }
    var type by remember { mutableStateOf("上门随访") }
    var content by remember { mutableStateOf("") }
    var showElderlyPicker by remember { mutableStateOf(false) }

    val elderlyList by remember(staff?.phone) {
        if (staff != null) ServiceLocator.bindingRepository.observeAccessibleElderly(staff!!)
        else flowOf(emptyList())
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    val types = listOf("上门随访", "电话随访", "健康随访")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加随访计划", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 选择老人
                OutlinedButton(
                    onClick = { showElderlyPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(elderly?.profile?.name ?: "选择老人", color = if (elderly == null) TextHint else TextDark)
                }

                // 随访类型
                Text("随访类型", color = TextGray, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    types.forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t, fontSize = 12.sp) }
                        )
                    }
                }

                // 随访内容
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("随访内容") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    elderly?.let { e ->
                        if (content.isNotBlank()) {
                            onConfirm(e.elderlyId, e.profile.name, type, System.currentTimeMillis(), content)
                        }
                    }
                },
                enabled = elderly != null && content.isNotBlank()
            ) { Text("确认") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )

    if (showElderlyPicker) {
        AlertDialog(
            onDismissRequest = { showElderlyPicker = false },
            title = { Text("选择老人") },
            text = {
                LazyColumn {
                    items(elderlyList) { e ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { elderly = e; showElderlyPicker = false }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(e.profile.name, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${e.profile.age}岁·${e.profile.gender.label}", color = TextHint, fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showElderlyPicker = false }) { Text("关闭") } }
        )
    }
}
