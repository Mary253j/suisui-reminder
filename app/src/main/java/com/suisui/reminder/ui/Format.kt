package com.suisui.reminder.ui

import java.time.DayOfWeek

fun weekdayCN(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> "一"
    DayOfWeek.TUESDAY -> "二"
    DayOfWeek.WEDNESDAY -> "三"
    DayOfWeek.THURSDAY -> "四"
    DayOfWeek.FRIDAY -> "五"
    DayOfWeek.SATURDAY -> "六"
    DayOfWeek.SUNDAY -> "日"
}

fun daysUntilLabel(days: Long): String = when {
    days == 0L -> "今天"
    days == 1L -> "明天"
    days > 0L -> "还有 $days 天"
    else -> "已过 ${-days} 天"
}

fun timeLabel(minutes: Int): String = String.format("%02d:%02d", minutes / 60, minutes % 60)
