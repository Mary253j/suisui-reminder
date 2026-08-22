package com.suisui.reminder.ui.screens

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.suisui.reminder.data.ReminderPrecision
import com.suisui.reminder.data.ReminderStage
import com.suisui.reminder.ui.MainViewModel
import com.suisui.reminder.ui.Routes
import com.suisui.reminder.ui.components.BottomNav
import com.suisui.reminder.ui.timeLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavHostController) {
    val vm: MainViewModel = viewModel(factory = MainViewModel.Factory)
    val settings by vm.settings.collectAsState()
    val context = LocalContext.current

    var showAdvancePicker by remember { mutableStateOf(false) }
    var showMorningPicker by remember { mutableStateOf(false) }

    val requestNotif = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        runCatching { alarmManager.canScheduleExactAlarms() }.getOrDefault(false)
    } else {
        true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        bottomBar = { BottomNav(navController, Routes.SETTINGS) }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SectionTitle("提醒精度")
            ReminderPrecision.entries.forEach { p ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { vm.setPrecision(p) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = settings.precision == p, onClick = { vm.setPrecision(p) })
                    Text(p.label, style = MaterialTheme.typography.bodyLarge)
                }
            }
            Text(
                "精确闹钟：到点必响，最准时；普通通知：更省电，时间可能有偏差；两者都要：同时排定两种。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SectionTitle("提醒时间")
            SettingRow("提前1月/1周/1天 的提醒时间", timeLabel(settings.advanceTimeMinutes)) {
                showAdvancePicker = true
            }
            SettingRow("当天早晨的提醒时间", timeLabel(settings.morningTimeMinutes)) {
                showMorningPicker = true
            }

            SectionTitle("提醒级别")
            ReminderStage.all().forEach { stage ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stage.label, modifier = Modifier.weight(1f))
                    Switch(
                        checked = stageEnabled(settings, stage),
                        onCheckedChange = { vm.setStageEnabled(stage, it) }
                    )
                }
            }

            SectionTitle("权限")
            if (Build.VERSION.SDK_INT >= 33) {
                val notifGranted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                SettingRow("通知权限", if (notifGranted) "已开启" else "未开启") {
                    if (!notifGranted) requestNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            SettingRow("精确闹钟权限", if (canExact) "可用" else "需手动开启") {
                if (!canExact) {
                    val intent = Intent(
                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:${context.packageName}")
                    )
                    runCatching { context.startActivity(intent) }
                }
            }

            SectionTitle("系统日历")
            OutlinedButton(
                onClick = { navController.navigate(Routes.IMPORT) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("从系统日历导入日程") }

            SectionTitle("关于")
            Text(
                "农历换算为内置算法（覆盖1900-2100年），无需联网。提醒由手机本地闹钟服务触发，开机或日期变化后会自动重排，删除应用才会失效。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.padding(bottom = 12.dp))
        }
    }

    if (showAdvancePicker) {
        TimePickerDialogWrap(settings.advanceTimeMinutes, onConfirm = {
            vm.setAdvanceTime(it)
            showAdvancePicker = false
        }, onDismiss = { showAdvancePicker = false })
    }
    if (showMorningPicker) {
        TimePickerDialogWrap(settings.morningTimeMinutes, onConfirm = {
            vm.setMorningTime(it)
            showMorningPicker = false
        }, onDismiss = { showMorningPicker = false })
    }
}

private fun stageEnabled(settings: com.suisui.reminder.data.ReminderSettings, stage: ReminderStage): Boolean =
    when (stage) {
        ReminderStage.MONTH_BEFORE -> settings.monthBefore
        ReminderStage.WEEK_BEFORE -> settings.weekBefore
        ReminderStage.DAY_BEFORE -> settings.dayBefore
        ReminderStage.ON_DAY -> settings.onDay
    }

@Composable
private fun SettingRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialogWrap(initialMinutes: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    val state = rememberTimePickerState(
        initialHour = initialMinutes / 60,
        initialMinute = initialMinutes % 60
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        text = { TimePicker(state = state) }
    )
}
