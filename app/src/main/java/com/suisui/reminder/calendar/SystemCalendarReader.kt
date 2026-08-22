package com.suisui.reminder.calendar

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.suisui.reminder.data.CalendarType
import com.suisui.reminder.data.EventEntity
import com.suisui.reminder.data.EventType
import com.suisui.reminder.data.Recurrence
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object SystemCalendarReader {

    data class CalendarEvent(
        val id: Long,
        val title: String,
        val allDay: Boolean,
        val startMillis: Long,
        val rrule: String?
    ) {
        val recurring: Boolean get() = rrule != null
    }

    fun hasReadPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    fun readEvents(
        context: Context,
        fromMillis: Long = System.currentTimeMillis() - 90L * 86400000L,
        toMillis: Long = System.currentTimeMillis() + 400L * 86400000L
    ): List<CalendarEvent> {
        if (!hasReadPermission(context)) return emptyList()
        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.ALL_DAY,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.RRULE
        )
        val selection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?"
        val args = arrayOf(fromMillis.toString(), toMillis.toString())
        val out = mutableListOf<CalendarEvent>()
        context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            projection,
            selection,
            args,
            "${CalendarContract.Events.DTSTART} ASC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(CalendarContract.Events._ID)
            val titleCol = cursor.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
            val allDayCol = cursor.getColumnIndexOrThrow(CalendarContract.Events.ALL_DAY)
            val startCol = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
            val rruleCol = cursor.getColumnIndexOrThrow(CalendarContract.Events.RRULE)
            while (cursor.moveToNext()) {
                val title = cursor.getString(titleCol) ?: continue
                val start = cursor.getLong(startCol)
                val rrule = if (cursor.isNull(rruleCol)) null else cursor.getString(rruleCol)
                out.add(
                    CalendarEvent(
                        id = cursor.getLong(idCol),
                        title = title,
                        allDay = cursor.getInt(allDayCol) == 1,
                        startMillis = start,
                        rrule = rrule
                    )
                )
            }
        }
        return out
    }

    fun toEvent(ce: CalendarEvent): EventEntity {
        val date: LocalDate =
            Instant.ofEpochMilli(ce.startMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        val yearly = ce.rrule?.contains("FREQ=YEARLY") == true
        return EventEntity(
            name = ce.title,
            type = EventType.GENERAL,
            calendarType = CalendarType.SOLAR,
            recurrence = if (yearly) Recurrence.YEARLY else Recurrence.NONE,
            solarYear = date.year,
            solarMonth = date.monthValue,
            solarDay = date.dayOfMonth,
            remind = true,
            note = "从系统日历导入"
        )
    }
}
