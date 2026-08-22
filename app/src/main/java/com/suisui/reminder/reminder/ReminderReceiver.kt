package com.suisui.reminder.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.suisui.reminder.data.ReminderStage
import java.time.LocalDate

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getLongExtra(EXTRA_EVENT_ID, -1L)
        val stageIndex = intent.getIntExtra(EXTRA_STAGE_INDEX, -1)
        if (eventId < 0 || stageIndex < 0) return
        val stage = ReminderStage.all().getOrNull(stageIndex) ?: return
        val name = intent.getStringExtra(EXTRA_NAME) ?: "日程"
        val typeLabel = intent.getStringExtra(EXTRA_TYPE) ?: ""
        val occurrenceEpochDay = intent.getLongExtra(EXTRA_OCCURRENCE, 0L)

        val title: String
        val text: String
        if (stage == ReminderStage.ON_DAY) {
            title = "今天是 ${name} 的${typeLabel}！"
            text = "祝 ${name} ${typeLabel}快乐 🎉"
        } else {
            val occurrence = LocalDate.ofEpochDay(occurrenceEpochDay)
            title = name
            text = "${stage.label}：${name}的${typeLabel}在 ${occurrence.month}月${occurrence.day}日"
        }

        val notificationId = (eventId.toInt() and 0x0fffffff) * 4 + stageIndex
        NotificationHelper.show(context, notificationId, title, text)
    }

    companion object {
        const val ACTION_REMINDER = "com.suisui.reminder.REMINDER"
        const val EXTRA_EVENT_ID = "event_id"
        const val EXTRA_STAGE_INDEX = "stage_index"
        const val EXTRA_NAME = "name"
        const val EXTRA_TYPE = "type"
        const val EXTRA_OCCURRENCE = "occurrence"
    }
}
