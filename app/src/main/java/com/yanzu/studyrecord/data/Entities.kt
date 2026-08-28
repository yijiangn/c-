package com.yanzu.studyrecord.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val colorArgb: Long,
    val isBuiltIn: Boolean,
    val sortOrder: Int,
)

@Entity(
    tableName = "study_tasks",
    foreignKeys = [ForeignKey(
        entity = SubjectEntity::class,
        parentColumns = ["id"],
        childColumns = ["subjectId"],
        onDelete = ForeignKey.SET_NULL,
    )],
    indices = [Index("subjectId"), Index("dateEpochDay")],
)
data class StudyTaskEntity(
    @PrimaryKey val id: String,
    val name: String,
    val subjectId: String?,
    val plannedMinutes: Int,
    val actualMinutes: Int,
    val dateEpochDay: Long,
    val priority: String,
    val status: String,
    val note: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "study_sessions",
    foreignKeys = [
        ForeignKey(
            entity = StudyTaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("taskId"), Index("subjectId"), Index("dateEpochDay")],
)
data class StudySessionEntity(
    @PrimaryKey val id: String,
    val taskId: String?,
    val subjectId: String?,
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val durationMinutes: Int,
    val dateEpochDay: Long,
    val mode: String,
    val source: String,
)

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val dailyGoalMinutes: Int = 180,
    val weeklyGoalMinutes: Int = 1260,
    val pomodoroMinutes: Int = 25,
    val themeMode: String = ThemeMode.SYSTEM.name,
    val timerState: String = TimerState.IDLE.name,
    val timerMode: String = TimerMode.STOPWATCH.name,
    val timerStartedAtEpochMillis: Long = 0,
    val timerBaseElapsedSeconds: Long = 0,
    val timerTargetSeconds: Long = 0,
    val timerTaskId: String? = null,
    val timerSubjectId: String? = null,
)

enum class TaskPriority { HIGH, MEDIUM, LOW }
enum class TaskStatus { NOT_STARTED, IN_PROGRESS, COMPLETED }
enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class TimerState { IDLE, RUNNING, PAUSED }
enum class TimerMode { STOPWATCH, POMODORO }

data class TaskItem(
    val task: StudyTaskEntity,
    val subject: SubjectEntity?,
)

data class SubjectDuration(
    val subject: SubjectEntity?,
    val minutes: Int,
)

object BuiltInSubjects {
    const val ENGLISH = "builtin_english"
    const val MATH = "builtin_math"
    const val PROGRAMMING = "builtin_programming"
    const val READING = "builtin_reading"
    const val OTHER = "builtin_other"

    fun all() = listOf(
        SubjectEntity(ENGLISH, "英语", 0xFFE57373, true, 0),
        SubjectEntity(MATH, "数学", 0xFFFFA726, true, 1),
        SubjectEntity(PROGRAMMING, "编程", 0xFF43A047, true, 2),
        SubjectEntity(READING, "阅读", 0xFF42A5F5, true, 3),
        SubjectEntity(OTHER, "其他", 0xFF9E9E9E, true, 4),
    )
}
