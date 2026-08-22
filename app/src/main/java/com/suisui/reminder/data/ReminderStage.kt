package com.suisui.reminder.data

enum class ReminderStage(val label: String, val index: Int) {
    MONTH_BEFORE("提前一个月", 0),
    WEEK_BEFORE("提前一周", 1),
    DAY_BEFORE("提前一天", 2),
    ON_DAY("当天早晨", 3);

    companion object {
        fun all(): List<ReminderStage> = listOf(MONTH_BEFORE, WEEK_BEFORE, DAY_BEFORE, ON_DAY)
    }
}
