package com.suisui.reminder.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: EventType,
    val calendarType: CalendarType,
    val recurrence: Recurrence,
    /** 公历年（一次性日程必填；每年重复时作为出生/起始年，仅用于展示） */
    val solarYear: Int? = null,
    val solarMonth: Int? = null,
    val solarDay: Int? = null,
    /** 农历年月日（农历日程使用）；lunarLeap 表示是否为闰月 */
    val lunarYear: Int? = null,
    val lunarMonth: Int? = null,
    val lunarDay: Int? = null,
    val lunarLeap: Boolean = false,
    val remind: Boolean = true,
    /** 提醒精度覆盖；null 表示使用全局设置 */
    val precisionOverride: ReminderPrecision? = null,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
