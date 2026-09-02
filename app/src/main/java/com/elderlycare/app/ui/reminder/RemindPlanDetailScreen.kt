package com.elderlycare.app.ui.reminder

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.elderlycare.app.R
import com.elderlycare.app.data.reminder.RemindPlanEntity
import com.elderlycare.app.ui.theme.DividerColor
import com.elderlycare.app.ui.theme.Primary
import com.elderlycare.app.ui.theme.StatusGreen
import com.elderlycare.app.ui.theme.Surface as SurfaceColor
import com.elderlycare.app.ui.theme.TextHint
import com.elderlycare.app.ui.theme.TextPrimary
import com.elderlycare.app.ui.theme.TextSecondary
import kotlinx.coroutines.flow.first

/**
 * 提醒计划详情页（日程 Tab / 提醒计划列表点击条目进入；只读，无编辑/删除）。
 *
 * 打开时保护（防脏数据/设备侧已删的幽灵条目，只执行一次）：
 * 1. 本地 Room 查不到该计划（或 clockId 为空脏行）→ 删本地脏行 + toast「该提醒计划已不存在」+ 关闭页面；
 * 2. 本地查得到 → 用 clockId 调 clock/list 核对设备侧是否仍存在：
 *    - 设备侧已删 → 同步删除 Room 对应记录 + toast + 关闭页面；
 *    - 核对失败（网络/未登录）→ 保留页面展示 Room 数据。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindPlanDetailScreen(
    planId: Long,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: RemindPlanViewModel = viewModel()
    val plan by viewModel.observePlan(planId).collectAsStateWithLifecycle(initialValue = null)
    val planGoneText = stringResource(R.string.reminder_plan_gone)

    // 打开时校验（只跑一次；删除本地记录在关闭页面前 await 完成）
    LaunchedEffect(planId) {
        val current = viewModel.observePlan(planId).first()
        if (current == null || current.clockId.isBlank()) {
            if (current != null) viewModel.deleteLocal(current)
            Toast.makeText(context, planGoneText, Toast.LENGTH_SHORT).show()
            onBack()
            return@LaunchedEffect
        }
        // 设备核对（clock/list 优先）：返回该 clock 已删除 → 删 Room 记录 + 关闭
        when (viewModel.verifyClockExists(current.clockId)) {
            false -> {
                viewModel.deleteLocal(current)
                Toast.makeText(context, planGoneText, Toast.LENGTH_SHORT).show()
                onBack()
            }
            else -> Unit // true=设备存在；null=核对失败 → 保留页面展示 Room 数据
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.reminder_plan_detail),
                        fontWeight = FontWeight.SemiBold
                    )
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
        val current = plan
        if (current == null) {
            // 首帧前 / 已被同步删除的兜底（正常由上方保护逻辑先 toast 关闭页面）
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) { }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                PlanDetailCard(current)
            }
        }
    }
}

/** 详情卡片：图标 + 标题 + 状态标签 + 留言内容 + 分隔线 + 时间重复 */
@Composable
private fun PlanDetailCard(plan: RemindPlanEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题 + 播报状态标签
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.AutoMirrored.Filled.EventNote,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    plan.tag,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(Modifier.weight(1f))
                if (plan.executed == RemindPlanEntity.EXECUTED_YES) {
                    Surface(shape = RoundedCornerShape(6.dp), color = StatusGreen.copy(alpha = 0.12f)) {
                        Text(
                            stringResource(R.string.reminder_executed),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusGreen
                        )
                    }
                } else {
                    Surface(shape = RoundedCornerShape(6.dp), color = TextHint.copy(alpha = 0.10f)) {
                        Text(
                            stringResource(R.string.reminder_not_executed),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // 留言内容（设备播报文案）
            if (plan.content.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    plan.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = DividerColor)
            Spacer(Modifier.height(12.dp))

            // 开始时间 + 重复周期
            Text(
                stringResource(R.string.reminder_start_time),
                style = MaterialTheme.typography.labelMedium,
                color = TextHint
            )
            Spacer(Modifier.height(4.dp))
            Text(
                remindPlanTimeRepeatText(plan),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}
