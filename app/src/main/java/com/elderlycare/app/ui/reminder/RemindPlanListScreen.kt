package com.elderlycare.app.ui.reminder

import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.elderlycare.app.R
import com.elderlycare.app.data.reminder.RemindPlanEntity
import com.elderlycare.app.data.reminder.RemindTemplate
import com.elderlycare.app.ui.theme.Primary
import com.elderlycare.app.ui.theme.StatusGreen
import com.elderlycare.app.ui.theme.StatusRed
import com.elderlycare.app.ui.theme.StatusYellow
import com.elderlycare.app.ui.theme.Surface as SurfaceColor
import com.elderlycare.app.ui.theme.TextHint
import com.elderlycare.app.ui.theme.TextPrimary
import com.elderlycare.app.ui.theme.TextSecondary

/**
 * 提醒计划列表页。
 *
 * - 留言页右上角入口：showBackButton=true，可添加/删除（新增唯一入口）；
 * - 底部导航日程 Tab（聚合总览）：showBackButton=false、showAddButton=false、showDeleteButton=false，
 *   只读 Room 数据展示提醒计划（日程其中一类），点击条目 → onPlanClick 跳详情。
 *
 * 空态：纸飞机 + 「还没有提醒计划」；底部蓝色【+ 添加计划】仅 showAddButton 时渲染（对齐萤石云视频原型）。
 * 列表：卡片（图标 + 标题 + 内容 + 「HH:mm · 重复标签」+ 「已播报完成」标签 + 删除）。
 * 添加流程：点底部按钮 → 场景模板弹窗 → onNavigateToForm(templateKey) 跳表单页。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindPlanListScreen(
    showBackButton: Boolean,
    onBack: (() -> Unit)? = null,
    onNavigateToForm: (String) -> Unit,
    onPlanClick: ((RemindPlanEntity) -> Unit)? = null,
    showAddButton: Boolean = true,
    showDeleteButton: Boolean = true
) {
    val context = LocalContext.current
    val viewModel: RemindPlanViewModel = viewModel()

    val plans by viewModel.plans.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    var showTemplateSheet by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<RemindPlanEntity?>(null) }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.consumeToast()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.reminder_plan_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = onBack ?: {}) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.message_back)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceColor)
            )
        },
        bottomBar = {
            if (showAddButton) {
                Surface(color = SurfaceColor) {
                    Button(
                        onClick = { showTemplateSheet = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.reminder_add_plan))
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (plans.isEmpty()) {
                RemindPlanEmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(plans, key = { it.id }) { plan ->
                        RemindPlanCard(
                            plan = plan,
                            onClick = onPlanClick?.let { click -> { click(plan) } },
                            showDeleteButton = showDeleteButton,
                            onDeleteClick = { deleteTarget = plan },
                            onConfirmAgree = if (plan.confirmStatus == RemindPlanEntity.CONFIRM_PENDING) {
                                { viewModel.confirmPlan(plan, true) }
                            } else null,
                            onConfirmReject = if (plan.confirmStatus == RemindPlanEntity.CONFIRM_PENDING) {
                                { viewModel.confirmPlan(plan, false) }
                            } else null
                        )
                    }
                }
            }
        }
    }

    // 场景模板弹窗
    if (showTemplateSheet) {
        RemindTemplateSheet(
            onDismiss = { showTemplateSheet = false },
            onTemplateSelected = { template ->
                showTemplateSheet = false
                onNavigateToForm(template.key)
            }
        )
    }

    // 删除确认（仿留言列表删除交互）
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.reminder_delete_confirm)) },
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

/** 空态：纸飞机 + 文案（添加按钮在底部常驻） */
@Composable
private fun RemindPlanEmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.AutoMirrored.Filled.Send,
            contentDescription = null,
            tint = TextHint,
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.reminder_plan_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = TextHint
        )
    }
}

/** 单条提醒计划卡片（列表页与日程页共用；onClick 非空时整卡可点击跳详情；只读场景不显示删除） */
@Composable
internal fun RemindPlanCard(
    plan: RemindPlanEntity,
    onClick: (() -> Unit)? = null,
    showDeleteButton: Boolean = true,
    onDeleteClick: () -> Unit,
    onConfirmAgree: (() -> Unit)? = null,
    onConfirmReject: (() -> Unit)? = null
) {
    Card(
        modifier = if (onClick != null) Modifier.fillMaxWidth().clickable(onClick = onClick)
        else Modifier.fillMaxWidth(),
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
                // 留言内容
                if (plan.content.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        plan.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                // 时间 · 重复标签
                Spacer(Modifier.height(5.dp))
                Text(
                    remindPlanTimeRepeatText(plan),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextHint
                )
                // 复诊双重确认状态（v6）：待确认 → 同意/拒绝按钮；已同意/已拒绝 → 只读徽标
                if (plan.confirmStatus != RemindPlanEntity.CONFIRM_NONE) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val (label, color) = when (plan.confirmStatus) {
                            RemindPlanEntity.CONFIRM_PENDING ->
                                R.string.reminder_confirm_pending to StatusYellow
                            RemindPlanEntity.CONFIRM_AGREED ->
                                R.string.reminder_confirm_agreed to StatusGreen
                            else -> R.string.reminder_confirm_rejected to StatusRed
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = color.copy(alpha = 0.12f)
                        ) {
                            Text(
                                stringResource(label),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = color
                            )
                        }
                        // 待确认且提供回调（列表页）才渲染操作按钮；日程页只读只显徽标
                        if (plan.confirmStatus == RemindPlanEntity.CONFIRM_PENDING &&
                            (onConfirmAgree != null || onConfirmReject != null)) {
                            Spacer(Modifier.width(8.dp))
                            onConfirmAgree?.let {
                                TextButton(
                                    onClick = it,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Text(
                                        stringResource(R.string.reminder_confirm_agree),
                                        color = Primary
                                    )
                                }
                            }
                            onConfirmReject?.let {
                                TextButton(
                                    onClick = it,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Text(
                                        stringResource(R.string.reminder_confirm_reject),
                                        color = StatusRed
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (showDeleteButton) {
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Filled.DeleteOutline,
                        contentDescription = stringResource(R.string.message_delete),
                        tint = TextHint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

/** 添加提醒场景模板弹窗（2 列网格） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemindTemplateSheet(
    onDismiss: () -> Unit,
    onTemplateSelected: (RemindTemplate) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                stringResource(R.string.reminder_template_title),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(16.dp))

            RemindTemplate.entries.chunked(2).forEach { rowTemplates ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowTemplates.forEach { template ->
                        RemindTemplateItem(
                            template = template,
                            onClick = { onTemplateSelected(template) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // 奇数个补占位，保持网格对齐
                    if (rowTemplates.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

/** 模板格子：图标 + 名称 */
@Composable
private fun RemindTemplateItem(
    template: RemindTemplate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                templateIcon(template),
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(template.label, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
        }
    }
}

/** 模板图标（material-icons-extended） */
private fun templateIcon(template: RemindTemplate): ImageVector = when (template) {
    RemindTemplate.ALARM -> Icons.Filled.Alarm
    RemindTemplate.MEDICINE -> Icons.Filled.Medication
    RemindTemplate.BLOOD_PRESSURE -> Icons.Filled.MonitorHeart
    RemindTemplate.BILL -> Icons.Filled.Payments
    RemindTemplate.HOME_CARE -> Icons.Filled.Home
    RemindTemplate.DATE -> Icons.Filled.Favorite
    RemindTemplate.EXERCISE -> Icons.Filled.FitnessCenter
    RemindTemplate.WORDS -> Icons.AutoMirrored.Filled.MenuBook
    RemindTemplate.ONLINE_COURSE -> Icons.Filled.School
    RemindTemplate.CUSTOM -> Icons.Filled.EditNote
}
