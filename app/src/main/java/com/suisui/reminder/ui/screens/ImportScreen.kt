package com.suisui.reminder.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.suisui.reminder.calendar.ContactsBirthdayReader
import com.suisui.reminder.calendar.SystemCalendarReader
import com.suisui.reminder.ui.MainViewModel
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(navController: NavHostController) {
    val vm: MainViewModel = viewModel(factory = MainViewModel.Factory)
    val context = LocalContext.current

    var calendarPerm by remember { mutableStateOf(SystemCalendarReader.hasReadPermission(context)) }
    var contactsPerm by remember { mutableStateOf(ContactsBirthdayReader.hasReadPermission(context)) }
    var calendarEvents by remember { mutableStateOf<List<SystemCalendarReader.CalendarEvent>>(emptyList()) }
    var contactBirthdays by remember { mutableStateOf<List<ContactsBirthdayReader.ContactBirthday>>(emptyList()) }
    var selectedCalendar by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var selectedContacts by remember { mutableStateOf<Set<Long>>(emptySet()) }

    val requestCalendar = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        calendarPerm = granted
        if (granted) calendarEvents = SystemCalendarReader.readEvents(context)
    }
    val requestContacts = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        contactsPerm = granted
        if (granted) contactBirthdays = ContactsBirthdayReader.readBirthdays(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("从系统日历 / 联系人导入") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("系统日历", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            }
            if (!calendarPerm) {
                item { Text("授权后可一键导入手机日历中的日程。") }
                item {
                    Button(
                        onClick = { requestCalendar.launch(Manifest.permission.READ_CALENDAR) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("授权读取系统日历") }
                }
            } else {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { calendarEvents = SystemCalendarReader.readEvents(context) },
                            modifier = Modifier.weight(1f)
                        ) { Text("刷新") }
                        Button(
                            onClick = {
                                calendarEvents.filter { it.id in selectedCalendar }
                                    .forEach { vm.addEvent(SystemCalendarReader.toEvent(it)) }
                                navController.popBackStack()
                            },
                            modifier = Modifier.weight(1f),
                            enabled = selectedCalendar.isNotEmpty()
                        ) { Text("导入选中（${selectedCalendar.size}）") }
                    }
                }
                item {
                    Text(
                        "将导入为“普通日程”；每年重复的会自动转为每年重复提醒。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (calendarEvents.isEmpty()) {
                    item { Text("未读取到日程，可点“刷新”。", style = MaterialTheme.typography.bodySmall) }
                } else {
                    items(calendarEvents, key = { "cal_${it.id}" }) { ce ->
                        ImportRow(
                            title = ce.title,
                            subtitle = importDateLabel(ce),
                            checked = ce.id in selectedCalendar,
                            onToggle = {
                                selectedCalendar =
                                    if (ce.id in selectedCalendar) selectedCalendar - ce.id else selectedCalendar + ce.id
                            }
                        )
                    }
                }
            }

            item {
                Column {
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                }
            }

            item {
                Text("联系人里的生日", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            }
            if (!contactsPerm) {
                item { Text("联系人中填写了生日字段的联系人会出现在这里，可一键转成每年自动提醒的生日日程。") }
                item {
                    Button(
                        onClick = { requestContacts.launch(Manifest.permission.READ_CONTACTS) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("授权读取联系人") }
                }
            } else {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { contactBirthdays = ContactsBirthdayReader.readBirthdays(context) },
                            modifier = Modifier.weight(1f)
                        ) { Text("刷新") }
                        Button(
                            onClick = {
                                contactBirthdays.filter { it.contactId in selectedContacts }
                                    .forEach { vm.addEvent(ContactsBirthdayReader.toEvent(it)) }
                                navController.popBackStack()
                            },
                            modifier = Modifier.weight(1f),
                            enabled = selectedContacts.isNotEmpty()
                        ) { Text("导入选中（${selectedContacts.size}）") }
                    }
                }
                item {
                    Text(
                        "以公历生日导入并每年自动提醒；联系人里没填生日的不会显示。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (contactBirthdays.isEmpty()) {
                    item { Text("未找到填写了生日的联系人。", style = MaterialTheme.typography.bodySmall) }
                } else {
                    items(contactBirthdays, key = { "con_${it.contactId}_${it.month}_${it.day}" }) { cb ->
                        val yearText = cb.year?.let { "${it}年" } ?: ""
                        ImportRow(
                            title = cb.name,
                            subtitle = "$yearText ${cb.month}月${cb.day}日",
                            checked = cb.contactId in selectedContacts,
                            onToggle = {
                                selectedContacts =
                                    if (cb.contactId in selectedContacts) selectedContacts - cb.contactId else selectedContacts + cb.contactId
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportRow(title: String, subtitle: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun importDateLabel(ce: SystemCalendarReader.CalendarEvent): String {
    val date = Instant.ofEpochMilli(ce.startMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    val base = "${date.year}年${date.monthValue}月${date.dayOfMonth}日"
    return if (ce.recurring) "${base}（每年重复）" else base
}
