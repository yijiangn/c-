package com.yanzu.studyrecord.util

import com.yanzu.studyrecord.data.StudySessionEntity
import com.yanzu.studyrecord.data.StudyTaskEntity
import com.yanzu.studyrecord.data.SubjectDuration
import com.yanzu.studyrecord.data.SubjectEntity
import com.yanzu.studyrecord.data.TaskStatus
import java.time.LocalDate
import java.time.YearMonth

data class DailyDuration(val epochDay: Long, val minutes: Int)

data class StudyStats(
    val todayMinutes: Int = 0,
    val weekMinutes: Int = 0,
    val monthMinutes: Int = 0,
    val todayCompleted: Int = 0,
    val weekCompletionRate: Int = 0,
    val last7Days: List<DailyDuration> = emptyList(),
    val subjectDurations: List<SubjectDuration> = emptyList(),
    val streakDays: Int = 0,
)

object StatsCalculator {
    fun calculate(
        sessions: List<StudySessionEntity>,
        tasks: List<StudyTaskEntity>,
        subjects: List<SubjectEntity>,
        today: LocalDate = LocalDate.now(),
    ): StudyStats {
        val todayEpoch = today.toEpochDay()
        val week = DateUtils.weekRange(today)
        val month = DateUtils.monthRange(YearMonth.from(today))
        val valid = sessions.filter { it.durationMinutes > 0 }
        val minutesByDay = valid.groupBy { it.dateEpochDay }.mapValues { (_, rows) -> rows.sumOf { it.durationMinutes } }
        val weekTasks = tasks.filter { it.dateEpochDay in week }
        val completed = weekTasks.count { it.status == TaskStatus.COMPLETED.name }
        val subjectMap = subjects.associateBy { it.id }
        val subjectDurations = valid.groupBy { it.subjectId }
            .map { (id, rows) -> SubjectDuration(subjectMap[id], rows.sumOf { it.durationMinutes }) }
            .filter { it.minutes > 0 }
            .sortedByDescending { it.minutes }
        val last7 = (6 downTo 0).map { offset ->
            val day = today.minusDays(offset.toLong()).toEpochDay()
            DailyDuration(day, minutesByDay[day] ?: 0)
        }
        var streak = 0
        var cursor = todayEpoch
        while ((minutesByDay[cursor] ?: 0) > 0) { streak++; cursor-- }
        return StudyStats(
            todayMinutes = minutesByDay[todayEpoch] ?: 0,
            weekMinutes = valid.filter { it.dateEpochDay in week }.sumOf { it.durationMinutes },
            monthMinutes = valid.filter { it.dateEpochDay in month }.sumOf { it.durationMinutes },
            todayCompleted = tasks.count { it.dateEpochDay == todayEpoch && it.status == TaskStatus.COMPLETED.name },
            weekCompletionRate = if (weekTasks.isEmpty()) 0 else completed * 100 / weekTasks.size,
            last7Days = last7,
            subjectDurations = subjectDurations,
            streakDays = streak,
        )
    }
}
