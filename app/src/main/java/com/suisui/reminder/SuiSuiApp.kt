package com.suisui.reminder

import android.app.Application
import com.suisui.reminder.data.AppDatabase
import com.suisui.reminder.data.EventRepository
import com.suisui.reminder.data.MomentRepository
import com.suisui.reminder.data.SettingsRepository
import com.suisui.reminder.reminder.NotificationHelper
import com.suisui.reminder.reminder.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SuiSuiApp : Application() {

    lateinit var eventRepository: EventRepository
    lateinit var momentRepository: MomentRepository
    lateinit var settingsRepository: SettingsRepository
    lateinit var reminderScheduler: ReminderScheduler

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.get(this)
        eventRepository = EventRepository(db.eventDao())
        momentRepository = MomentRepository(db.momentDao())
        settingsRepository = SettingsRepository(this)
        reminderScheduler = ReminderScheduler(this, settingsRepository)
        NotificationHelper.createChannel(this)
        CoroutineScope(Dispatchers.IO).launch {
            reminderScheduler.scheduleAll(eventRepository.getAll())
        }
    }
}
