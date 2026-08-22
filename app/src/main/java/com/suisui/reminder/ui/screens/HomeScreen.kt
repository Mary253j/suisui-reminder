package com.suisui.reminder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.suisui.reminder.lunar.LunarConverter
import com.suisui.reminder.ui.HomeItem
import com.suisui.reminder.ui.MainViewModel
import com.suisui.reminder.ui.Routes
import com.suisui.reminder.ui.components.BottomNav
import com.suisui.reminder.ui.daysUntilLabel
import com.suisui.reminder.ui.weekdayCN
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    val vm: MainViewModel = viewModel(factory = MainViewModel.Factory)
    val items by vm.homeItems.collectAsState()
    val today = LocalDate.now()
    val lunarToday = LunarConverter.solarToLunar(today)

    Scaffold(
        bottomBar = { BottomNav(navController, Routes.HOME) },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Routes.addEvent()) }) {
                Icon(Icons.Filled.Add, contentDescription = "添加日程")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { HeaderCard(today, lunarToday) }
            item {
                Text(
                    "日程列表",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            if (items.isEmpty()) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Text(
                            "还没有日程，点右下角 + 添加第一个生日或纪念日吧",
                            modifier = Modifier.padding(20.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(items, key = { it.event.id }) { item ->
                    EventRow(item, onClick = { navController.navigate(Routes.detail(item.event.id)) })
                }
            }
        }
    }
}

@Composable
private fun HeaderCard(today: LocalDate, lunar: LunarConverter.LunarDate?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                "${today.monthValue}月${today.dayOfMonth}日 星期${weekdayCN(today.dayOfWeek)}",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(Modifier.size(4.dp))
            Text(
                lunar?.let { "农历 ${it.fullLabel()} · ${LunarConverter.zodiac(it.year)}年" } ?: "农历：—",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.92f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventRow(item: HomeItem, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "${item.occurrence.monthValue}月",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "${item.occurrence.dayOfMonth}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.event.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${item.event.type.label} · ${item.event.calendarType.label} · ${item.event.recurrence.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                daysUntilLabel(item.daysUntil),
                style = MaterialTheme.typography.labelLarge,
                color = if (item.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
