package com.suisui.reminder.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.suisui.reminder.SuiSuiApp
import com.suisui.reminder.data.EventEntity
import com.suisui.reminder.data.MomentEntity
import com.suisui.reminder.data.ReminderPrecision
import com.suisui.reminder.data.ReminderSettings
import com.suisui.reminder.data.ReminderStage
import com.suisui.reminder.reminder.ReminderCalculator
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeItem(
    val event: EventEntity,
    val occurrence: LocalDate,
    val daysUntil: Long
) {
    val isToday: Boolean get() = daysUntil == 0L
}

class MainViewModel(app: SuiSuiApp) : ViewModel() {

    private val eventRepository = app.eventRepository
    private val momentRepository = app.momentRepository
    private val settingsRepository = app.settingsRepository
    private val scheduler = app.reminderScheduler

    val events: StateFlow<List<EventEntity>> = eventRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val moments: StateFlow<List<MomentEntity>> = momentRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<ReminderSettings> = settingsRepository.settings
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            ReminderSettings(
                precision = ReminderPrecision.EXACT,
                advanceTimeMinutes = 540,
                morningTimeMinutes = 480,
                monthBefore = true,
                weekBefore = true,
                dayBefore = true,
                onDay = true
            )
        )

    val homeItems: StateFlow<List<HomeItem>> = events.map { list ->
        val today = LocalDate.now()
        list.mapNotNull { e ->
            ReminderCalculator.nextOccurrence(e, today)?.let { occ ->
                HomeItem(e, occ, ChronoUnit.DAYS.between(today, occ))
            }
        }.sortedWith(compareBy({ it.daysUntil < 0 }, { it.daysUntil }))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getEvent(id: Long): EventEntity? = events.value.firstOrNull { it.id == id }

    fun addEvent(event: EventEntity) = viewModelScope.launch {
        eventRepository.insert(event)
        scheduler.scheduleAll(eventRepository.getAll())
    }

    fun updateEvent(event: EventEntity) = viewModelScope.launch {
        eventRepository.update(event)
        scheduler.scheduleAll(eventRepository.getAll())
    }

    fun deleteEvent(event: EventEntity) = viewModelScope.launch {
        scheduler.cancelForEvent(event.id)
        eventRepository.delete(event)
        scheduler.scheduleAll(eventRepository.getAll())
    }

    fun addMoment(note: String?, imagePaths: List<String>) = viewModelScope.launch {
        momentRepository.insert(MomentEntity(note = note, imagePaths = imagePaths))
    }

    fun deleteMoment(moment: MomentEntity) = viewModelScope.launch {
        momentRepository.delete(moment)
    }

    fun setPrecision(precision: ReminderPrecision) = viewModelScope.launch {
        settingsRepository.setPrecision(precision)
        scheduler.scheduleAll(eventRepository.getAll())
    }

    fun setAdvanceTime(minutes: Int) = viewModelScope.launch {
        settingsRepository.setAdvanceTime(minutes)
        scheduler.scheduleAll(eventRepository.getAll())
    }

    fun setMorningTime(minutes: Int) = viewModelScope.launch {
        settingsRepository.setMorningTime(minutes)
        scheduler.scheduleAll(eventRepository.getAll())
    }

    fun setStageEnabled(stage: ReminderStage, enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setStageEnabled(stage, enabled)
        scheduler.scheduleAll(eventRepository.getAll())
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as SuiSuiApp
                MainViewModel(app)
            }
        }
    }
}
