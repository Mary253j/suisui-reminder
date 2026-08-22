package com.suisui.reminder.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.suisui.reminder.SuiSuiApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 开机、时间/时区/日期变化、应用更新后重新排定提醒。
 */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as SuiSuiApp
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                app.reminderScheduler.scheduleAll(app.eventRepository.getAll())
            } finally {
                pendingResult.finish()
            }
        }
    }
}
