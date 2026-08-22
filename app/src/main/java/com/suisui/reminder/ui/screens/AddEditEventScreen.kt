package com.suisui.reminder.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.suisui.reminder.data.CalendarType
import com.suisui.reminder.data.EventEntity
import com.suisui.reminder.data.EventType
import com.suisui.reminder.data.Recurrence
import com.suisui.reminder.data.ReminderPrecision
import com.suisui.reminder.lunar.LunarConverter
import com.suisui.reminder.ui.MainViewModel
import com.suisui.reminder.ui.Routes
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditEventScreen(navController: NavHostController, eventId: Long) {
    val vm: MainViewModel = viewModel(factory = MainViewModel.Factory)
    val events by vm.events.collectAsState()
    val settings by vm.settings.collectAsState()
    val existing = if (eventId > 0) events.firstOrNull { it.id == eventId } else null

    if (eventId > 0 && existing == null) {
        Scaffold(
            topBar = { TopAppBar(title = { Text("加载中…") }) }
        ) { }
        return
    }

    var name by remember(existing) { mutableStateOf(existing?.name ?: "") }
    var type by remember(existing) { mutableStateOf(existing?.type ?: EventType.BIRTHDAY) }
    var calendarType by remember(existing) { mutableStateOf(existing?.calendarType ?: CalendarType.LUNAR) }
    var recurrence by remember(existing) { mutableStateOf(existing?.recurrence ?: Recurrence.YEARLY) }
    var solarDate by remember(existing) {
        mutableStateOf(
            existing?.let {
                val y = it.solarYear ?: LocalDate.now().year
                val m = it.solarMonth ?: 1
                val d = it.solarDay ?: 1
                runCatching { LocalDate.of(y, m, d) }.getOrDefault(LocalDate.now())
            } ?: LocalDate.now()
        )
    }
    var lunarMonth by remember(existing) { mutableStateOf(existing?.lunarMonth ?: 1) }
    var lunarDay by remember(existing) { mutableStateOf(existing?.lunarDay ?: 1) }
    var lunarLeap by remember(existing) { mutableStateOf(existing?.lunarLeap ?: false) }
    var lunarYearText by remember(existing) {
        mutableStateOf(existing?.lunarYear?.toString() ?: LocalDate.now().year.toString())
    }
    var remind by remember(existing) { mutableStateOf(existing?.remind ?: true) }
    var precision by remember(existing) { mutableStateOf(existing?.precisionOverride) }
    var note by remember(existing) { mutableStateOf(existing?.note ?: "") }

    var showDatePicker by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (eventId > 0) "编辑日程" else "添加日程") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        val event = buildEvent(
                            name, type, calendarType, recurrence, solarDate,
                            lunarMonth, lunarDay, lunarLeap, lunarYearText,
                            remind, precision, note, existing
                        )
                        if (event == null) {
                            showError = true
                        } else {
                            if (eventId > 0) vm.updateEvent(event) else vm.addEvent(event)
                            navController.popBackStack()
                        }
                    }) { Text("保存") }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("名称（必填）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (showError) {
                Text("名称或日期不完整，请检查后保存", color = MaterialTheme.colorScheme.error)
            }

            SectionTitle("类型")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EventType.entries.forEach { t ->
                    FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t.label) })
                }
            }

            SectionTitle("历法")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CalendarType.entries.forEach { c ->
                    FilterChip(
                        selected = calendarType == c,
                        onClick = { calendarType = c },
                        label = { Text(c.label) }
                    )
                }
            }

            if (calendarType == CalendarType.SOLAR) {
                SectionTitle("日期（公历）")
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "${solarDate.year}年${solarDate.monthValue}月${solarDate.dayOfMonth}日"
                    )
                }
            } else {
                SectionTitle("日期（农历）")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = lunarLeap, onCheckedChange = { lunarLeap = it })
                    Text("闰月")
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    PickerField(
                        label = "农历月",
                        value = lunarMonth,
                        options = (1..12).toList(),
                        display = { m -> (if (lunarLeap) "闰" else "") + LunarConverter.monthName(m) },
                        onSelect = { lunarMonth = it },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    PickerField(
                        label = "农历日",
                        value = lunarDay,
                        options = (1..30).toList(),
                        display = { d -> LunarConverter.dayName(d) },
                        onSelect = { lunarDay = it },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (recurrence == Recurrence.NONE) {
                    OutlinedTextField(
                        value = lunarYearText,
                        onValueChange = { lunarYearText = it.filter { c -> c.isDigit() } },
                        label = { Text("农历年份（一次性日程需要）") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            SectionTitle("重复")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Recurrence.entries.forEach { r ->
                    FilterChip(selected = recurrence == r, onClick = { recurrence = r }, label = { Text(r.label) })
                }
            }

            SectionTitle("提醒")
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("开启提醒", modifier = Modifier.weight(1f))
                Switch(checked = remind, onCheckedChange = { remind = it })
            }
            if (remind) {
                Text(
                    "将按时提醒：提前1个月、1周、1天与当天早晨（具体时间可在设置中调整）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                PickerField(
                    label = "提醒方式",
                    value = precision,
                    options = listOf<ReminderPrecision?>(null, ReminderPrecision.EXACT, ReminderPrecision.INEXACT, ReminderPrecision.BOTH),
                    display = { it?.label ?: "跟随全局设置" },
                    onSelect = { precision = it }
                )
            }

            SectionTitle("备注")
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("备注（可选）") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Spacer(Modifier.width(1.dp))
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = solarDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { ms ->
                        solarDate = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun <T> PickerField(
    label: String,
    value: T,
    options: List<T>,
    display: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(display(value), maxLines = 1)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(display(opt)) },
                    onClick = {
                        onSelect(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun buildEvent(
    name: String,
    type: EventType,
    calendarType: CalendarType,
    recurrence: Recurrence,
    solarDate: LocalDate,
    lunarMonth: Int,
    lunarDay: Int,
    lunarLeap: Boolean,
    lunarYearText: String,
    remind: Boolean,
    precision: ReminderPrecision?,
    note: String,
    existing: EventEntity?
): EventEntity? {
    val n = name.trim()
    if (n.isEmpty()) return null
    val lunarYear = if (calendarType == CalendarType.LUNAR && recurrence == Recurrence.NONE) {
        lunarYearText.toIntOrNull() ?: return null
    } else {
        null
    }
    return EventEntity(
        id = existing?.id ?: 0,
        name = n,
        type = type,
        calendarType = calendarType,
        recurrence = recurrence,
        solarYear = if (calendarType == CalendarType.SOLAR) solarDate.year else null,
        solarMonth = if (calendarType == CalendarType.SOLAR) solarDate.monthValue else null,
        solarDay = if (calendarType == CalendarType.SOLAR) solarDate.dayOfMonth else null,
        lunarYear = lunarYear,
        lunarMonth = if (calendarType == CalendarType.LUNAR) lunarMonth else null,
        lunarDay = if (calendarType == CalendarType.LUNAR) lunarDay else null,
        lunarLeap = if (calendarType == CalendarType.LUNAR) lunarLeap else false,
        remind = remind,
        precisionOverride = precision,
        note = note.trim().ifEmpty { null }
    )
}
