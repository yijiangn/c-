package com.yanzu.studyrecord.util

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

object DateUtils {
    private val fullFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE", Locale.CHINA)
    private val shortFormatter = DateTimeFormatter.ofPattern("M月d日 · EEEE", Locale.CHINA)
    private val csvFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.CHINA)
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA)

    fun todayEpochDay(): Long = LocalDate.now().toEpochDay()
    fun fullDate(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(fullFormatter)
    fun shortDate(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(shortFormatter)
    fun csvDate(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(csvFormatter)
    fun time(epochMillis: Long): String = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(timeFormatter)
    fun epochDayAtNoonMillis(epochDay: Long): Long = LocalDate.ofEpochDay(epochDay).atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun weekRange(today: LocalDate = LocalDate.now()): LongRange {
        val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toEpochDay()
        return start..(start + 6)
    }

    fun monthRange(month: YearMonth = YearMonth.now()): LongRange =
        month.atDay(1).toEpochDay()..month.atEndOfMonth().toEpochDay()

    fun formatMinutes(minutes: Int): String = when {
        minutes < 60 -> "${minutes}分钟"
        minutes % 60 == 0 -> "${minutes / 60}小时"
        else -> "${minutes / 60}小时${minutes % 60}分钟"
    }

    fun formatTimer(seconds: Long): String = "%02d:%02d:%02d".format(seconds / 3600, seconds / 60 % 60, seconds % 60)
}
