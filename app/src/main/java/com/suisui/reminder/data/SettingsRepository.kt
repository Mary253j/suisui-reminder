package com.suisui.reminder.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class ReminderSettings(
    val precision: ReminderPrecision,
    val advanceTimeMinutes: Int,
    val morningTimeMinutes: Int,
    val monthBefore: Boolean,
    val weekBefore: Boolean,
    val dayBefore: Boolean,
    val onDay: Boolean
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val PRECISION = stringPreferencesKey("precision")
        val ADVANCE_TIME = intPreferencesKey("advance_time")
        val MORNING_TIME = intPreferencesKey("morning_time")
        val MONTH_BEFORE = booleanPreferencesKey("month_before")
        val WEEK_BEFORE = booleanPreferencesKey("week_before")
        val DAY_BEFORE = booleanPreferencesKey("day_before")
        val ON_DAY = booleanPreferencesKey("on_day")
    }

    val settings: Flow<ReminderSettings> = context.dataStore.data.map { p ->
        ReminderSettings(
            precision = runCatching { ReminderPrecision.valueOf(p[Keys.PRECISION] ?: "EXACT") }
                .getOrDefault(ReminderPrecision.EXACT),
            advanceTimeMinutes = p[Keys.ADVANCE_TIME] ?: 540,
            morningTimeMinutes = p[Keys.MORNING_TIME] ?: 480,
            monthBefore = p[Keys.MONTH_BEFORE] ?: true,
            weekBefore = p[Keys.WEEK_BEFORE] ?: true,
            dayBefore = p[Keys.DAY_BEFORE] ?: true,
            onDay = p[Keys.ON_DAY] ?: true
        )
    }

    suspend fun setPrecision(precision: ReminderPrecision) {
        context.dataStore.edit { it[Keys.PRECISION] = precision.name }
    }

    suspend fun setAdvanceTime(minutes: Int) {
        context.dataStore.edit { it[Keys.ADVANCE_TIME] = minutes }
    }

    suspend fun setMorningTime(minutes: Int) {
        context.dataStore.edit { it[Keys.MORNING_TIME] = minutes }
    }

    suspend fun setStageEnabled(stage: ReminderStage, enabled: Boolean) {
        val key = when (stage) {
            ReminderStage.MONTH_BEFORE -> Keys.MONTH_BEFORE
            ReminderStage.WEEK_BEFORE -> Keys.WEEK_BEFORE
            ReminderStage.DAY_BEFORE -> Keys.DAY_BEFORE
            ReminderStage.ON_DAY -> Keys.ON_DAY
        }
        context.dataStore.edit { it[key] = enabled }
    }
}
