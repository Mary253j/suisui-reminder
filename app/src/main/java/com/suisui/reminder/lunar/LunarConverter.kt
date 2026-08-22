package com.suisui.reminder.lunar

import java.time.LocalDate

/**
 * 农历/公历换算（1900-2100）。
 * 纯算法实现，无第三方依赖；数据表已通过已知日期、闰月锚点、
 * 相邻新年间隔自洽与全量往返校验。
 */
object LunarConverter {

    data class LunarDate(
        val year: Int,
        val month: Int,
        val day: Int,
        val isLeap: Boolean
    ) {
        fun label(): String = (if (isLeap) "闰" else "") + monthName(month) + dayName(day)
        fun fullLabel(): String = ganzhiYear(year) + " " + label()
    }

    private val LUNAR_INFO = intArrayOf(
        0x4bd8, 0x4ae0, 0xa570, 0x54d5, 0xd260, 0xd950, 0x16554, 0x56a0, 0x9ad0, 0x55d2,
        0x4ae0, 0xa5b6, 0xa4d0, 0xd250, 0x1d255, 0xb540, 0xd6a0, 0xada2, 0x95b0, 0x14977,
        0x4970, 0xa4b0, 0xb4b5, 0x6a50, 0x6d40, 0x1ab54, 0x2b60, 0x9570, 0x52f2, 0x4970,
        0x6566, 0xd4a0, 0xea50, 0x6e95, 0x5ad0, 0x2b60, 0x186e3, 0x92e0, 0x1c8d7, 0xc950,
        0xd4a0, 0x1d8a6, 0xb550, 0x56a0, 0x1a5b4, 0x25d0, 0x92d0, 0xd2b2, 0xa950, 0xb557,
        0x6ca0, 0xb550, 0x15355, 0x4da0, 0xa5b0, 0x14573, 0x52b0, 0xa9a8, 0xe950, 0x6aa0,
        0xaea6, 0xab50, 0x4b60, 0xaae4, 0xa570, 0x5260, 0xf263, 0xd950, 0x5b57, 0x56a0,
        0x96d0, 0x4dd5, 0x4ad0, 0xa4d0, 0xd4d4, 0xd250, 0xd558, 0xb540, 0xb6a0, 0x195a6,
        0x95b0, 0x49b0, 0xa974, 0xa4b0, 0xb27a, 0x6a50, 0x6d40, 0xaf46, 0xab60, 0x9570,
        0x4af5, 0x4970, 0x64b0, 0x74a3, 0xea50, 0x6b58, 0x55c0, 0xab60, 0x96d5, 0x92e0,
        0xc960, 0xd954, 0xd4a0, 0xda50, 0x7552, 0x56a0, 0xabb7, 0x25d0, 0x92d0, 0xcab5,
        0xa950, 0xb4a0, 0xbaa4, 0xad50, 0x55d9, 0x4ba0, 0xa5b0, 0x15176, 0x52b0, 0xa930,
        0x7954, 0x6aa0, 0xad50, 0x5b52, 0x4b60, 0xa6e6, 0xa4e0, 0xd260, 0xea65, 0xd530,
        0x5aa0, 0x76a3, 0x96d0, 0x4afb, 0x4ad0, 0xa4d0, 0x1d0b6, 0xd250, 0xd520, 0xdd45,
        0xb5a0, 0x56d0, 0x55b2, 0x49b0, 0xa577, 0xa4b0, 0xaa50, 0x1b255, 0x6d20, 0xada0,
        0x14b63, 0x9370, 0x49f8, 0x4970, 0x64b0, 0x168a6, 0xea50, 0x6b20, 0x1a6c4, 0xaae0,
        0xa2e0, 0xd2e3, 0xc960, 0xd557, 0xd4a0, 0xda50, 0x5d55, 0x56a0, 0xa6d0, 0x55d4,
        0x52d0, 0xa9b8, 0xa950, 0xb4a0, 0xb6a6, 0xad50, 0x55a0, 0xaba4, 0xa5b0, 0x52b0,
        0xb273, 0x6930, 0x7337, 0x6aa0, 0xad50, 0x14b55, 0x4b60, 0xa570, 0x54e4, 0xd160,
        0xe968, 0xd520, 0xdaa0, 0x16aa6, 0x56d0, 0x4ae0, 0xa9d4, 0xa2d0, 0xd150, 0xf252,
        0xd520
    )

    private fun leapMonth(year: Int): Int = LUNAR_INFO[year - 1900] and 0xf

    private fun leapDays(year: Int): Int =
        if (leapMonth(year) > 0) (if ((LUNAR_INFO[year - 1900] and 0x10000) != 0) 30 else 29) else 0

    private fun monthDays(year: Int, month: Int): Int =
        if ((LUNAR_INFO[year - 1900] and (0x10000 shr month)) != 0) 30 else 29

    private fun yearDays(year: Int): Int {
        var sum = 348
        var i = 0x8000
        while (i > 0x8) {
            if ((LUNAR_INFO[year - 1900] and i) != 0) sum++
            i = i shr 1
        }
        return sum + leapDays(year)
    }

    private val BASE_EPOCH_DAY: Long = LocalDate.of(1900, 1, 31).toEpochDay()

    fun solarToLunar(date: LocalDate): LunarDate? {
        val offset = date.toEpochDay() - BASE_EPOCH_DAY
        if (offset < 0) return null
        var ly = 1900
        var remaining = offset
        var yd = yearDays(ly)
        while (remaining >= yd) {
            remaining -= yd
            ly++
            yd = yearDays(ly)
        }
        val leap = leapMonth(ly)
        var month = 1
        var isLeap = false
        while (true) {
            val dim = monthDays(ly, month)
            if (remaining < dim) break
            remaining -= dim
            if (leap == month) {
                val ld = leapDays(ly)
                if (remaining < ld) {
                    isLeap = true
                    break
                }
                remaining -= ld
            }
            month++
            if (month > 12) break
        }
        return LunarDate(ly, month, (remaining + 1).toInt(), isLeap)
    }

    fun lunarToSolar(year: Int, month: Int, day: Int, isLeap: Boolean): LocalDate? {
        val leap = leapMonth(year)
        if (isLeap && leap != month) return null
        val maxDay = if (isLeap) leapDays(year) else monthDays(year, month)
        if (day < 1 || day > maxDay) return null
        var offset = 0L
        for (i in 1900 until year) offset += yearDays(i)
        for (i in 1 until month) {
            offset += monthDays(year, i)
            if (leap == i) offset += leapDays(year)
        }
        if (isLeap) offset += monthDays(year, month)
        offset += (day - 1)
        return LocalDate.ofEpochDay(BASE_EPOCH_DAY + offset)
    }

    private val MONTH_NAMES = listOf(
        "正月", "二月", "三月", "四月", "五月", "六月",
        "七月", "八月", "九月", "十月", "冬月", "腊月"
    )

    private val DAY_NAMES = listOf(
        "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
        "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
        "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"
    )

    fun monthName(month: Int): String = MONTH_NAMES[month - 1]

    fun dayName(day: Int): String = DAY_NAMES[day - 1]

    private val GAN = listOf("甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸")
    private val ZHI = listOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")
    private val ZODIAC = listOf("鼠", "牛", "虎", "兔", "龙", "蛇", "马", "羊", "猴", "鸡", "狗", "猪")

    fun ganzhiYear(year: Int): String =
        GAN[Math.floorMod(year - 4, 10)] + ZHI[Math.floorMod(year - 4, 12)] + "年"

    fun zodiac(year: Int): String = ZODIAC[Math.floorMod(year - 4, 12)]
}
