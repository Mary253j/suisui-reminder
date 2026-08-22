package com.suisui.reminder.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.suisui.reminder.data.EventEntity
import com.suisui.reminder.data.ReminderPrecision
import com.suisui.reminder.data.ReminderStage
import com.suisui.reminder.data.SettingsRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.first

/**
 * 使用 AlarmManager 排定/取消精确或非精确的提醒闹钟。
 * 策略：EXACT -> 精确闹钟；INEXACT -> 非精确；BOTH -> 两者都排。
 */
class ReminderScheduler(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    suspend fun scheduleAll(events: List<EventEntity>) {
        val settings = settingsRepository.settings.first()
        val today = LocalDate.now()
        for (event in events) {
            cancelForEvent(event.id)
            if (!event.remind) continue
            val plan = ReminderCalculator.plan(event, settings, today) ?: continue
            for (r in plan.reminders) scheduleOne(event, r)
        }
    }

    fun cancelForEvent(eventId: Long) {
        for (stage in ReminderStage.all()) {
            alarmManager.cancel(schedulePendingIntent(buildIntent(eventId, stage.index, MODE_EXACT), eventId, stage.index, MODE_EXACT))
            alarmManager.cancel(schedulePendingIntent(buildIntent(eventId, stage.index, MODE_INEXACT), eventId, stage.index, MODE_INEXACT))
        }
    }

    private fun scheduleOne(event: EventEntity, r: ReminderCalculator.PlannedReminder) {
        fun build(mode: String): PendingIntent {
            val intent = buildIntent(event.id, r.stage.index, mode).apply {
                putExtra(ReminderReceiver.EXTRA_EVENT_ID, event.id)
                putExtra(ReminderReceiver.EXTRA_STAGE_INDEX, r.stage.index)
                putExtra(ReminderReceiver.EXTRA_NAME, event.name)
                putExtra(ReminderReceiver.EXTRA_TYPE, event.type.label)
                putExtra(ReminderReceiver.EXTRA_OCCURRENCE, r.occurrence.toEpochDay())
            }
            return schedulePendingIntent(intent, event.id, r.stage.index, mode)
        }

        when (r.precision) {
            ReminderPrecision.EXACT -> setAlarm(build(MODE_EXACT), r.atEpochMillis, exact = true)
            ReminderPrecision.INEXACT -> setAlarm(build(MODE_INEXACT), r.atEpochMillis, exact = false)
            ReminderPrecision.BOTH -> {
                setAlarm(build(MODE_EXACT), r.atEpochMillis, exact = true)
                setAlarm(build(MODE_INEXACT), r.atEpochMillis, exact = false)
            }
        }
    }

    private fun setAlarm(pi: PendingIntent, atMillis: Long, exact: Boolean) {
        if (exact && canScheduleExact()) {
            runCatching {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
            }.onFailure {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
            }
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
        }
    }

    private fun canScheduleExact(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return runCatching { alarmManager.canScheduleExactAlarms() }.getOrDefault(false)
    }

    private fun buildIntent(eventId: Long, stageIndex: Int, mode: String): Intent =
        Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_REMINDER
            data = Uri.parse("reminder://event/$eventId/$stageIndex/$mode")
        }

    private fun schedulePendingIntent(intent: Intent, eventId: Long, stageIndex: Int, mode: String): PendingIntent {
        val base = (eventId.toInt() and 0x0fffffff) * 4 + stageIndex
        val requestCode = base * 2 + (if (mode == MODE_EXACT) 0 else 1)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val MODE_EXACT = "e"
        const val MODE_INEXACT = "i"
    }
}
