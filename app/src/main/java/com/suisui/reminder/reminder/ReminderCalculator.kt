package com.suisui.reminder.reminder

import com.suisui.reminder.data.CalendarType
import com.suisui.reminder.data.EventEntity
import com.suisui.reminder.data.Recurrence
import com.suisui.reminder.data.ReminderPrecision
import com.suisui.reminder.data.ReminderSettings
import com.suisui.reminder.data.ReminderStage
import com.suisui.reminder.lunar.LunarConverter
import java.time.LocalDate
import java.time.ZoneId

/**
 * 计算日程的下一次发生日期，以及各级提醒的触发时间。
 */
object ReminderCalculator {

    data class PlannedReminder(
        val stage: ReminderStage,
        val atEpochMillis: Long,
        val occurrence: LocalDate,
        val precision: ReminderPrecision
    )

    data class Plan(
        val event: EventEntity,
        val occurrence: LocalDate,
        val reminders: List<PlannedReminder>
    )

    fun nextOccurrence(event: EventEntity, today: LocalDate): LocalDate? {
        return when (event.calendarType) {
            CalendarType.SOLAR -> {
                val month = event.solarMonth ?: return null
                val day = event.solarDay ?: return null
                if (event.recurrence == Recurrence.YEARLY) {
                    val thisYear = safeDate(today.year, month, day)
                    when {
                        thisYear != null && !thisYear.isBefore(today) -> thisYear
                        today.year < 2100 -> safeDate(today.year + 1, month, day)
                        else -> null
                    }
                } else {
                    val year = event.solarYear ?: return null
                    val fixed = safeDate(year, month, day) ?: return null
                    if (fixed.isBefore(today)) null else fixed
                }
            }
            CalendarType.LUNAR -> {
                val month = event.lunarMonth ?: return null
                val day = event.lunarDay ?: return null
                if (event.recurrence == Recurrence.YEARLY) {
                    for (year in today.year..today.year + 1) {
                        val found = findLunarInSolarYear(year, month, day, event.lunarLeap)
                            ?: if (event.lunarLeap) findLunarInSolarYear(year, month, day, false) else null
                        if (found != null && !found.isBefore(today)) return found
                    }
                    null
                } else {
                    val year = event.lunarYear ?: return null
                    val found = LunarConverter.lunarToSolar(year, month, day, event.lunarLeap)
                        ?: if (event.lunarLeap) LunarConverter.lunarToSolar(year, month, day, false) else null
                    if (found != null && !found.isBefore(today)) found else null
                }
            }
        }
    }

    /**
     * 兼容无效日期：2月29日生日在平年回退到2月28日，避免 LocalDate.of 抛异常。
     */
    private fun safeDate(year: Int, month: Int, day: Int): LocalDate? {
        runCatching { LocalDate.of(year, month, day) }.getOrNull()?.let { return it }
        if (month == 2 && day == 29) {
            runCatching { LocalDate.of(year, 2, 28) }.getOrNull()?.let { return it }
        }
        return null
    }

    private fun findLunarInSolarYear(
        solarYear: Int,
        lunarMonth: Int,
        lunarDay: Int,
        isLeap: Boolean
    ): LocalDate? {
        val start = LocalDate.of(solarYear, 1, 1)
        val end = LocalDate.of(solarYear, 12, 31)
        var d = start
        while (!d.isAfter(end)) {
            val l = LunarConverter.solarToLunar(d) ?: return null
            if (l.month == lunarMonth && l.day == lunarDay && l.isLeap == isLeap) return d
            d = d.plusDays(1)
        }
        return null
    }

    fun plan(event: EventEntity, settings: ReminderSettings, today: LocalDate): Plan? {
        val occurrence = nextOccurrence(event, today) ?: return null
        val precision = event.precisionOverride ?: settings.precision
        val reminders = mutableListOf<PlannedReminder>()
        val now = System.currentTimeMillis()

        fun add(stage: ReminderStage, baseDate: LocalDate, minutes: Int) {
            val at = baseDate.atTime(minutes / 60, minutes % 60)
                .atZone(ZoneId.systemDefault())
                .toInstant().toEpochMilli()
            if (at > now) {
                reminders.add(PlannedReminder(stage, at, occurrence, precision))
            }
        }

        if (settings.monthBefore) add(ReminderStage.MONTH_BEFORE, occurrence.minusMonths(1), settings.advanceTimeMinutes)
        if (settings.weekBefore) add(ReminderStage.WEEK_BEFORE, occurrence.minusWeeks(1), settings.advanceTimeMinutes)
        if (settings.dayBefore) add(ReminderStage.DAY_BEFORE, occurrence.minusDays(1), settings.advanceTimeMinutes)
        if (settings.onDay) add(ReminderStage.ON_DAY, occurrence, settings.morningTimeMinutes)

        return Plan(event, occurrence, reminders)
    }
}
