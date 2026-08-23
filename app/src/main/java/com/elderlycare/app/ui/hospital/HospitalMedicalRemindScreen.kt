package com.elderlycare.app.ui.hospital

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.elderlycare.app.R
import com.elderlycare.app.data.ezviz.ServiceLocator
import com.elderlycare.app.data.reminder.RemindPlanEntity
import com.elderlycare.app.ui.reminder.remindPlanTimeRepeatText
import com.elderlycare.app.ui.theme.Primary
import com.elderlycare.app.ui.theme.StatusGreen
import com.elderlycare.app.ui.theme.Surface as SurfaceColor
import com.elderlycare.app.ui.theme.TextHint
import com.elderlycare.app.ui.theme.TextPrimary
import com.elderlycare.app.ui.theme.TextSecondary
import com.elderlycare.app.util.limitCodePoints
import kotlinx.coroutines.flow.first
import java.util.Calendar

/**
 * 医院端「复诊提醒」。
 *
 * 老人选择（FilterChip 行，含未授权老人）+ 医院端计划列表 + 浮动按钮打开录入表单。
 * 表单：提醒日期/时间 + ≤20 字文本 + 复选框【下发设备进行播报】——
 * 强制授权校验：无 ACTIVE 医院授权 → 复选框置灰 + toast「暂无家属授权，不能下发提醒到设备」；
 * 有授权但未绑定设备 → 置灰 + toast「该老人未绑定设备，不能下发播报」。
 *
 * 保存两条路径（ViewModel）：勾选 → 复用现有提醒计划萤石 v3 clock 下发 RK3 播报（source=2）；
 * 不勾选 → 仅 App 本地提醒（source=1，不调萤石）。两条都落 remind_plan 表并调度 App 本地通知。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HospitalMedicalRemindScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: HospitalMedicalRemindViewModel = viewModel()

    val elderlyOptions by viewModel.elderlyOptions.collectAsStateWithLifecycle()
    val selectedElderlyId by viewModel.selectedElderlyId.collectAsStateWithLifecycle()
    val selectedElderly by viewModel.selectedElderly.collectAsStateWithLifecycle()
    val hospitalPlans by viewModel.hospitalPlans.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    // deviceSn → 老人姓名（计划列表行标注所属老人）
    var snToName by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    LaunchedEffect(Unit) {
        snToName = runCatching {
            ServiceLocator.profileStore.observeProfiles().first()
                .associate { it.deviceSn to it.name }
        }.getOrDefault(emptyMap())
    }

    // ===== 表单状态 =====
    var showForm by remember { mutableStateOf(false) }
    var draftDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var draftHour by remember { mutableIntStateOf(9) }
    var draftMin by remember { mutableIntStateOf(0) }
    var draftContent by remember { mutableStateOf("") }
    var sendToDevice by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // 通知权限（API 33+）：保存前请求；拒绝也保存，仅 toast 提示
    val notifyPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(
                context,
                R.string.hospital_remind_notify_permission_hint,
                Toast.LENGTH_LONG
            ).show()
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
                title = { Text(stringResource(R.string.hospital_remind_title), fontWeight = FontWeight.SemiBold) },
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
            FloatingActionButton(
                onClick = {
                    draftDateMillis = System.currentTimeMillis()
                    draftHour = 9
                    draftMin = 0
                    draftContent = ""
                    sendToDevice = false
                    showForm = true
                },
                containerColor = Primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.hospital_remind_new))
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // ===== 老人选择（含未授权老人；设备播报在表单内做授权校验） =====
            if (elderlyOptions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.hospital_remind_no_elderly),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(elderlyOptions, key = { it.elderlyId }) { option ->
                        FilterChip(
                            selected = option.elderlyId == selectedElderlyId,
                            onClick = {
                                viewModel.selectElderly(option.elderlyId)
                                sendToDevice = false
                            },
                            label = { Text(option.name) },
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }
            }

            // ===== 计划列表 =====
            if (hospitalPlans.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.hospital_remind_empty),
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(hospitalPlans, key = { it.id }) { plan ->
                        HospitalRemindPlanRow(plan, snToName[plan.deviceSerial])
                    }
                }
            }
        }
    }

    // ===== 录入表单弹窗 =====
    if (showForm) {
        AlertDialog(
            onDismissRequest = { if (!isSaving) showForm = false },
            title = { Text(stringResource(R.string.hospital_remind_new), fontWeight = FontWeight.SemiBold) },
            text = {
                Column {
                    // 提醒日期（单次）
                    FormSettingRow(
                        label = stringResource(R.string.hospital_remind_date),
                        value = singleDateText(draftDateMillis),
                        onClick = { showDatePicker = true }
                    )
                    Spacer(Modifier.height(8.dp))
                    // 提醒时间
                    FormSettingRow(
                        label = stringResource(R.string.reminder_start_time),
                        value = stringResource(R.string.reminder_time_format, draftHour, draftMin),
                        onClick = { showTimePicker = true }
                    )
                    Spacer(Modifier.height(8.dp))
                    // 提醒内容（≤20 字硬上限）
                    OutlinedTextField(
                        value = draftContent,
                        onValueChange = { draftContent = it.limitCodePoints(20) },
                        label = { Text(stringResource(R.string.hospital_remind_text_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 3
                    )
                    Text(
                        stringResource(R.string.reminder_content_counter, draftContent.length),
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextHint
                    )
                    Spacer(Modifier.height(8.dp))

                    // 复选框【下发设备进行播报】：强制授权校验
                    // 无 ACTIVE 授权 → 置灰 + 点击提示；有授权但未绑定设备 → 置灰 + 点击提示
                    val canPush = selectedElderly != null &&
                        selectedElderly!!.authorized &&
                        selectedElderly!!.deviceSn.isNotBlank()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !canPush) {
                                val sel = selectedElderly
                                val msg = if (sel == null || !sel.authorized) {
                                    context.getString(R.string.hospital_remind_no_auth_toast)
                                } else {
                                    context.getString(R.string.hospital_remind_no_device_toast)
                                }
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = sendToDevice && canPush,
                            onCheckedChange = { sendToDevice = it },
                            enabled = canPush
                        )
                        Text(
                            stringResource(R.string.hospital_remind_send_to_device),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (canPush) TextPrimary else TextHint
                        )
                    }
                    if (!canPush) {
                        val sel = selectedElderly
                        Text(
                            if (sel == null || !sel.authorized)
                                stringResource(R.string.hospital_remind_no_auth_toast)
                            else stringResource(R.string.hospital_remind_no_device_toast),
                            modifier = Modifier.padding(start = 16.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextHint
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val content = draftContent.trim()
                        if (content.isEmpty()) {
                            viewModel.toast(R.string.hospital_remind_content_required)
                            return@TextButton
                        }
                        // 单次日期顺延（与家属端表单一致）：早于今天 → 置今天；
                        // 今天且时间已过 → 顺延一天（weekdays 按最终日期推算，VM 内处理）
                        val cal = Calendar.getInstance().apply { timeInMillis = draftDateMillis }
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
                            draftHour * 60 + draftMin <=
                            now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
                        ) {
                            cal.add(Calendar.DAY_OF_YEAR, 1)
                        }

                        // 保存前请求通知权限（拒绝也保存，回调里 toast 提示）
                        if (Build.VERSION.SDK_INT >= 33 &&
                            ContextCompat.checkSelfPermission(
                                context, Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notifyPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        viewModel.save(
                            dateMillis = cal.timeInMillis,
                            timeHour = draftHour,
                            timeMin = draftMin,
                            content = content,
                            sendToDevice = sendToDevice
                        ) {
                            showForm = false
                            draftContent = ""
                            sendToDevice = false
                        }
                    },
                    enabled = !isSaving
                ) { Text(stringResource(R.string.reminder_save), color = Primary) }
            },
            dismissButton = {
                TextButton(onClick = { showForm = false }, enabled = !isSaving) {
                    Text(stringResource(R.string.message_text_cancel))
                }
            }
        )
    }

    // 时间选择（Material3 TimePicker，24 小时制）
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = draftHour,
            initialMinute = draftMin,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.reminder_start_time)) },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    draftHour = timePickerState.hour
                    draftMin = timePickerState.minute
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

    // 日期选择（默认今天）
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = draftDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { draftDateMillis = it }
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

/** 单条医院端复诊提醒计划：内容 + 时间/重复文案 + 来源 chip + 已播报 chip + 所属老人 */
@Composable
private fun HospitalRemindPlanRow(plan: RemindPlanEntity, elderlyName: String?) {
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
                    plan.content,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(Modifier.width(8.dp))
                // 来源 chip：设备播报（source=2）/ 仅 App 提醒（source=1）
                val isDevicePush = plan.source == RemindPlanEntity.SOURCE_HOSPITAL_DEVICE
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isDevicePush) Primary.copy(alpha = 0.10f) else TextSecondary.copy(alpha = 0.10f)
                ) {
                    Text(
                        stringResource(
                            if (isDevicePush) R.string.hospital_remind_device_push
                            else R.string.hospital_remind_local_only
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDevicePush) Primary else TextSecondary
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                remindPlanTimeRepeatText(plan),
                style = MaterialTheme.typography.labelSmall,
                color = TextHint
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (plan.executed == RemindPlanEntity.EXECUTED_YES) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = StatusGreen.copy(alpha = 0.12f)
                    ) {
                        Text(
                            stringResource(R.string.reminder_executed),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusGreen
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }
                if (!elderlyName.isNullOrBlank()) {
                    Text(
                        elderlyName,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextHint
                    )
                }
            }
        }
    }
}

/** 通用设置行：label + 右侧 value + 箭头，整行可点击（复诊提醒表单用） */
@Composable
private fun FormSettingRow(
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

/** millis → yyyy-MM-dd（reminder_date_format） */
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
