package com.yanzu.studyrecord.util

import com.yanzu.studyrecord.data.StudySessionEntity
import com.yanzu.studyrecord.data.StudyTaskEntity
import com.yanzu.studyrecord.data.SubjectEntity
import com.yanzu.studyrecord.data.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StatsCalculatorTest {
    private val today = LocalDate.of(2026, 8, 28)
    private val subject = SubjectEntity("math", "数学", 0xFFFFA726, false, 0)

    @Test
    fun `统计只累加真实的正时长记录`() {
        val sessions = listOf(session(today.toEpochDay(), 45), session(today.toEpochDay(), 30), session(today.minusDays(1).toEpochDay(), 60))
        val result = StatsCalculator.calculate(sessions, emptyList(), listOf(subject), today)
        assertEquals(75, result.todayMinutes)
        assertEquals(135, result.weekMinutes)
        assertEquals(135, result.monthMinutes)
        assertEquals(2, result.streakDays)
        assertEquals(135, result.subjectDurations.single().minutes)
    }

    @Test
    fun `本周完成率按任务状态计算`() {
        val tasks = listOf(task("1", today.toEpochDay(), TaskStatus.COMPLETED), task("2", today.toEpochDay(), TaskStatus.NOT_STARTED), task("3", today.minusDays(1).toEpochDay(), TaskStatus.COMPLETED))
        val result = StatsCalculator.calculate(emptyList(), tasks, listOf(subject), today)
        assertEquals(1, result.todayCompleted)
        assertEquals(66, result.weekCompletionRate)
    }

    @Test
    fun `没有有效记录时连续学习为零`() {
        assertEquals(0, StatsCalculator.calculate(emptyList(), emptyList(), listOf(subject), today).streakDays)
    }

    private fun session(day: Long, minutes: Int) = StudySessionEntity("s${day}_$minutes", null, subject.id, 0, 0, minutes, day, "STOPWATCH", "TIMER")
    private fun task(id: String, day: Long, status: TaskStatus) = StudyTaskEntity(id, "任务$id", subject.id, 30, 0, day, "MEDIUM", status.name, "", 0, 0)
}
