package com.elderlycare.app.ui.reminder

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.elderlycare.app.R
import com.elderlycare.app.data.reminder.PlanDraft
import com.elderlycare.app.data.reminder.PreviewVoices
import com.elderlycare.app.data.reminder.RemindPlanEntity
import com.elderlycare.app.data.reminder.RemindTemplate
import com.elderlycare.app.ui.theme.Primary
import com.elderlycare.app.ui.theme.Surface as SurfaceColor
import com.elderlycare.app.ui.theme.TextHint
import com.elderlycare.app.ui.theme.TextPrimary
import com.elderlycare.app.ui.theme.TextSecondary
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

/**
 * 新建提醒表单页（按场景模板预填）。
 *
 * ①标题（≤50 字）②留言内容（≤20 字含标点，前端硬拦截；麦克风语音输入回填截 20 字，
 * 不支持语音识别时按钮置灰）③选择声音（仅作用于手机试听——设备播报固定音色，保存不传音色）
 * ④开始时间（TimePicker）+ 重复周期（单次[DatePicker]/每日/每周[星期多选]）
 * ⑤底部【手机试听】(edge-tts) +【保存】(萤石 life/remind/clock 下发)。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RemindPlanFormScreen(
    templateKey: String,
    voiceKeyFlow: Flow<String>,
    onBack: () -> Unit,
    onNavigateToVoiceSelect: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: RemindPlanViewModel = viewModel()
    val template = remember(templateKey) { RemindTemplate.fromKey(templateKey) }

    // ===== 表单状态 =====
    var tag by remember { mutableStateOf(template.defaultTag) }
    var content by remember { mutableStateOf(template.defaultContent) }
    var timeHour by remember { mutableIntStateOf(8) }
    var timeMin by remember { mutableIntStateOf(0) }
    var repeatType by remember { mutableIntStateOf(RemindPlanEntity.REPEAT_DAILY) }
    var selectedWeekdays by remember { mutableStateOf(setOf<Int>()) }
    var singleDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val selectedVoice by viewModel.selectedVoice.collectAsStateWithLifecycle()
    val previewState by viewModel.previewState.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    // 音色回传（VoiceSelect 页通过 previousBackStackEntry.savedStateHandle 写回 "voice_key"）
    LaunchedEffect(Unit) {
        voiceKeyFlow.collect { viewModel.setSelectedVoice(it) }
    }

    // 语音输入（系统识别 Intent；设备不支持 → ActivityNotFoundException → 按钮置灰）
    var voiceAvailable by remember { mutableStateOf(true) }
    val voicePrompt = stringResource(R.string.reminder_voice_input_hint)
    val voiceInputLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                .orEmpty()
            // 识别结果回填，最多保留 20 字（与输入框上限一致，不做提交截断）
            content = spoken.take(20)
        }
    }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.consumeToast()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.reminder_new_plan), fontWeight = FontWeight.SemiBold) },
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
        bottomBar = {
            Surface(color = SurfaceColor) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 手机试听（文本+音色 → 后端 edge-tts；仅试听，不下发设备）
                    OutlinedButton(
                        onClick = { viewModel.togglePreview(content.trim(), selectedVoice) },
                        modifier = Modifier.weight(1f),
                        enabled = previewState != PreviewState.Loading && content.isNotBlank()
                    ) {
                        when (previewState) {
                            PreviewState.Loading -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Primary
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.reminder_preview))
                            }
                            PreviewState.Playing -> {
                                Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.reminder_stop_preview))
                            }
                            else -> {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.reminder_preview))
                            }
                        }
                    }
                    // 保存（萤石 life/remind/clock 下发；校验不通过留在本页 toast）
                    Button(
                        onClick = {
                            val trimmedTag = tag.trim()
                            val trimmedContent = content.trim()
                            if (trimmedTag.isBlank()) {
                                viewModel.toast(R.string.reminder_title_required)
                                return@Button
                            }
                            if (trimmedContent.isBlank()) {
                                viewModel.toast(R.string.reminder_content_required)
                                return@Button
                            }
                            if (repeatType == RemindPlanEntity.REPEAT_WEEKLY && selectedWeekdays.isEmpty()) {
                                viewModel.toast(R.string.reminder_weekday_required)
                                return@Button
                            }

                            // 单次：日期不可早于今天；选今天但时间已过 → 顺延一天（weekdays 按最终日期推算）
                            val cal = Calendar.getInstance().apply { timeInMillis = singleDateMillis }
                            if (repeatType == RemindPlanEntity.REPEAT_ONCE) {
                                val now = Calendar.getInstance()
                                val todayStart = now.clone() as Calendar
                                todayStart.set(Calendar.HOUR_OF_DAY, 0)
                                todayStart.set(Calendar.MINUTE, 0)
                                todayStart.set(Calendar.SECOND, 0)
                                todayStart.set(Calendar.MILLISECOND, 0)
                                if (cal.timeInMillis < todayStart.timeInMillis) {
                                    cal.timeInMillis = todayStart.timeInMillis
                                }
                                val isToday = cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                                    cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
                                if (isToday &&
                                    timeHour * 60 + timeMin <=
                                    now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
                                ) {
                                    cal.add(Calendar.DAY_OF_YEAR, 1)
                                }
                            }
                            val weekdays = when (repeatType) {
                                // 萤石 0=周日…6=周六；Calendar.DAY_OF_WEEK 1=周日…7=周六 → (dow+6)%7
                                RemindPlanEntity.REPEAT_ONCE -> listOf((cal.get(Calendar.DAY_OF_WEEK) + 6) % 7)
                                RemindPlanEntity.REPEAT_DAILY -> (0..6).toList()
                                else -> selectedWeekdays.sorted()
                            }
                            val draft = PlanDraft(
                                tag = trimmedTag,
                                content = trimmedContent,
                                timeHour = timeHour,
                                timeMin = timeMin,
                                repeatType = repeatType,
                                weekdays = weekdays,
                                year = if (repeatType == RemindPlanEntity.REPEAT_ONCE) cal.get(Calendar.YEAR) else 0,
                                month = if (repeatType == RemindPlanEntity.REPEAT_ONCE) cal.get(Calendar.MONTH) + 1 else 0,
                                day = if (repeatType == RemindPlanEntity.REPEAT_ONCE) cal.get(Calendar.DAY_OF_MONTH) else 0
                            )
                            viewModel.save(draft) { onBack() }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isSaving,
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Text(stringResource(R.string.reminder_save))
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // ①标题
            OutlinedTextField(
                value = tag,
                onValueChange = { if (it.length <= 50) tag = it },
                label = { Text(stringResource(R.string.reminder_title_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            // ②留言内容（20 字硬上限 + 计数 + 麦克风语音输入）
            OutlinedTextField(
                value = content,
                onValueChange = { if (it.length <= 20) content = it },
                label = { Text(stringResource(R.string.reminder_content_label)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 4,
                trailingIcon = {
                    IconButton(
                        onClick = {
                            try {
                                voiceInputLauncher.launch(
                                    Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(
                                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                        )
                                        putExtra(
                                            RecognizerIntent.EXTRA_PROMPT,
                                            voicePrompt
                                        )
                                    }
                                )
                            } catch (e: ActivityNotFoundException) {
                                // 设备不支持语音识别 → 按钮置灰禁用
                                voiceAvailable = false
                            }
                        },
                        enabled = voiceAvailable && content.length < 20
                    ) {
                        Icon(
                            Icons.Filled.Mic,
                            contentDescription = stringResource(R.string.reminder_voice_input_hint),
                            tint = if (voiceAvailable) Primary else TextHint
                        )
                    }
                }
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    stringResource(R.string.reminder_content_max_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    stringResource(R.string.reminder_content_counter, content.length),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextHint
                )
            }

            Spacer(Modifier.height(12.dp))

            // ③选择声音（音色仅用于手机试听）
            SettingRow(
                label = stringResource(R.string.reminder_voice_select),
                value = PreviewVoices.displayNameOf(selectedVoice),
                onClick = onNavigateToVoiceSelect
            )
            Text(
                stringResource(R.string.reminder_voice_hint),
                modifier = Modifier.padding(start = 16.dp, top = 6.dp),
                style = MaterialTheme.typography.bodySmall,
                color = TextHint
            )

            Spacer(Modifier.height(12.dp))

            // ④开始时间
            SettingRow(
                label = stringResource(R.string.reminder_start_time),
                value = stringResource(R.string.reminder_time_format, timeHour, timeMin),
                onClick = { showTimePicker = true }
            )

            Spacer(Modifier.height(16.dp))

            // ⑤重复周期
            Text(
                stringResource(R.string.reminder_repeat_label),
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = repeatType == RemindPlanEntity.REPEAT_ONCE,
                    onClick = { repeatType = RemindPlanEntity.REPEAT_ONCE },
                    label = { Text(stringResource(R.string.reminder_repeat_once)) },
                    shape = RoundedCornerShape(16.dp)
                )
                FilterChip(
                    selected = repeatType == RemindPlanEntity.REPEAT_DAILY,
                    onClick = { repeatType = RemindPlanEntity.REPEAT_DAILY },
                    label = { Text(stringResource(R.string.reminder_repeat_daily)) },
                    shape = RoundedCornerShape(16.dp)
                )
                FilterChip(
                    selected = repeatType == RemindPlanEntity.REPEAT_WEEKLY,
                    onClick = { repeatType = RemindPlanEntity.REPEAT_WEEKLY },
                    label = { Text(stringResource(R.string.reminder_repeat_weekly)) },
                    shape = RoundedCornerShape(16.dp)
                )
            }

            // 单次 → 播报日期
            if (repeatType == RemindPlanEntity.REPEAT_ONCE) {
                Spacer(Modifier.height(8.dp))
                SettingRow(
                    label = stringResource(R.string.reminder_single_date),
                    value = singleDateText(singleDateMillis),
                    onClick = { showDatePicker = true }
                )
            }

            // 每周 → 星期多选
            if (repeatType == RemindPlanEntity.REPEAT_WEEKLY) {
                Spacer(Modifier.height(8.dp))
                val weekdayNames = stringResource(R.string.reminder_weekday_names)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    weekdayNames.forEachIndexed { index, ch ->
                        FilterChip(
                            selected = index in selectedWeekdays,
                            onClick = {
                                selectedWeekdays =
                                    if (index in selectedWeekdays) selectedWeekdays - index
                                    else selectedWeekdays + index
                            },
                            label = { Text(ch.toString()) },
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // 时间选择（Material3 TimePicker，24 小时制）
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = timeHour,
            initialMinute = timeMin,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.reminder_start_time)) },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    timeHour = timePickerState.hour
                    timeMin = timePickerState.minute
                    showTimePicker = false
                }) { Text(stringResource(R.string.reminder_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.message_text_cancel))
                }
            }
        )
    }

    // 日期选择（单次计划播报日期，默认今天）
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = singleDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { singleDateMillis = it }
                    showDatePicker = false
                }) { Text(stringResource(R.string.reminder_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.message_text_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

/** 通用设置行：label + 右侧 value + 箭头，整行可点击 */
@Composable
private fun SettingRow(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceColor,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Spacer(Modifier.weight(1f))
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = 1
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = TextHint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** singleDateMillis → yyyy-MM-dd（reminder_date_format） */
@Composable
private fun singleDateText(millis: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = millis }
    return stringResource(
        R.string.reminder_date_format,
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH)
    )
}
