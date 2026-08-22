package com.suisui.reminder.calendar

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.suisui.reminder.data.CalendarType
import com.suisui.reminder.data.EventEntity
import com.suisui.reminder.data.EventType
import com.suisui.reminder.data.Recurrence
import java.time.LocalDate

/**
 * 读取手机联系人中填写了「生日」字段的联系人，批量转为生日日程。
 */
object ContactsBirthdayReader {

    data class ContactBirthday(
        val contactId: Long,
        val name: String,
        val year: Int?,
        val month: Int,
        val day: Int
    )

    fun hasReadPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED

    fun readBirthdays(context: Context): List<ContactBirthday> {
        if (!hasReadPermission(context)) return emptyList()
        val projection = arrayOf(
            ContactsContract.Data.CONTACT_ID,
            ContactsContract.Data.DISPLAY_NAME,
            ContactsContract.Data.DATA1,
            ContactsContract.Data.DATA2
        )
        val selection = "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.Data.DATA2} = ?"
        val args = arrayOf(
            ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY.toString()
        )
        val out = mutableListOf<ContactBirthday>()
        context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI, projection, selection, args, null
        )?.use { c ->
            val idCol = c.getColumnIndexOrThrow(ContactsContract.Data.CONTACT_ID)
            val nameCol = c.getColumnIndexOrThrow(ContactsContract.Data.DISPLAY_NAME)
            val dataCol = c.getColumnIndexOrThrow(ContactsContract.Data.DATA1)
            while (c.moveToNext()) {
                val raw = c.getString(dataCol) ?: continue
                val parsed = parseBirthday(raw) ?: continue
                val name = c.getString(nameCol) ?: continue
                if (name.isBlank()) continue
                out.add(
                    ContactBirthday(
                        contactId = c.getLong(idCol),
                        name = name,
                        year = parsed.first,
                        month = parsed.second,
                        day = parsed.third
                    )
                )
            }
        }
        return out.distinctBy { "${it.contactId}_${it.month}_${it.day}" }
    }

    /** 返回 (年?, 月, 日)。联系人生日可能是 yyyy-MM-dd，也可能没有年份（如 -02-14）。 */
    private fun parseBirthday(raw: String): Triple<Int?, Int, Int>? {
        val full = runCatching { LocalDate.parse(raw.trim()) }.getOrNull()
        if (full != null) return Triple(full.year, full.monthValue, full.dayOfMonth)
        val m = Regex("(\\d{1,2})-(\\d{1,2})").find(raw) ?: return null
        val month = m.groupValues[1].toIntOrNull() ?: return null
        val day = m.groupValues[2].toIntOrNull() ?: return null
        if (month !in 1..12 || day !in 1..31) return null
        return Triple(null, month, day)
    }

    fun toEvent(cb: ContactBirthday): EventEntity = EventEntity(
        name = cb.name,
        type = EventType.BIRTHDAY,
        calendarType = CalendarType.SOLAR,
        recurrence = Recurrence.YEARLY,
        solarYear = cb.year,
        solarMonth = cb.month,
        solarDay = cb.day,
        remind = true,
        note = "从联系人导入的生日"
    )
}
