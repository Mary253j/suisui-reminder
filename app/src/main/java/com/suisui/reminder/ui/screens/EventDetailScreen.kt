package com.suisui.reminder.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.suisui.reminder.data.CalendarType
import com.suisui.reminder.data.EventEntity
import com.suisui.reminder.data.Recurrence
import com.suisui.reminder.lunar.LunarConverter
import com.suisui.reminder.reminder.ReminderCalculator
import com.suisui.reminder.ui.MainViewModel
import com.suisui.reminder.ui.Routes
import com.suisui.reminder.ui.timeLabel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(navController: NavHostController, eventId: Long) {
    val vm: MainViewModel = viewModel(factory = MainViewModel.Factory)
    val events by vm.events.collectAsState()
    val settings by vm.settings.collectAsState()
    val event = events.firstOrNull { it.id == eventId }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("日程详情") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        if (event == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("未找到该日程")
            }
            return@Scaffold
        }
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(event.name, style = MaterialTheme.typography.headlineSmall)
            Text(
                "${event.type.label} · ${event.calendarType.label} · ${event.recurrence.label}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider()

            DetailRow("日期", dateText(event))
            if (event.recurrence == Recurrence.YEARLY) {
                DetailRow("重复", "每年")
            } else {
                DetailRow("重复", "一次性")
            }
            DetailRow("提醒", if (event.remind) "开启（${event.precisionOverride?.label ?: "跟随全局设置"}）" else "关闭")
            if (!event.note.isNullOrEmpty()) DetailRow("备注", event.note)

            HorizontalDivider()

            val plan = runCatching { ReminderCalculator.plan(event, settings, LocalDate.now()) }.getOrNull()
            if (plan != null) {
                Text("下次：${plan.occurrence.monthValue}月${plan.occurrence.dayOfMonth}日", style = MaterialTheme.typography.titleSmall)
                if (plan.reminders.isEmpty()) {
                    Text("该日程暂无待触发的提醒", style = MaterialTheme.typography.bodySmall)
                } else {
                    plan.reminders.forEach { r ->
                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    r.stage.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    timeLabel(
                                        if (r.stage == com.suisui.reminder.data.ReminderStage.ON_DAY) settings.morningTimeMinutes else settings.advanceTimeMinutes
                                    ),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { navController.navigate(Routes.addEvent(event.id)) },
                    modifier = Modifier.weight(1f)
                ) { Text("编辑") }
                Button(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.weight(1f)
                ) { Text("删除") }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除日程") },
            text = { Text("确定要删除「${event?.name ?: ""}」吗？相关提醒也会一并取消。") },
            confirmButton = {
                TextButton(onClick = {
                    event?.let { vm.deleteEvent(it) }
                    showDeleteDialog = false
                    navController.popBackStack()
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun dateText(event: EventEntity): String {
    return if (event.calendarType == CalendarType.SOLAR) {
        val y = event.solarYear
        val m = event.solarMonth ?: 1
        val d = event.solarDay ?: 1
        if (y != null) "${y}年${m}月${d}日" else "${m}月${d}日"
    } else {
        val m = event.lunarMonth ?: 1
        val d = event.lunarDay ?: 1
        val base = (if (event.lunarLeap) "闰" else "") + LunarConverter.monthName(m) + LunarConverter.dayName(d)
        val y = event.lunarYear
        if (event.recurrence == Recurrence.NONE && y != null) "农历 ${base}（${y}年）" else "农历 ${base}"
    }
}
