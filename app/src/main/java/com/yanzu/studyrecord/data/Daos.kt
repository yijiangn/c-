package com.yanzu.studyrecord.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects ORDER BY sortOrder, name")
    fun observeAll(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects ORDER BY sortOrder, name")
    suspend fun getAll(): List<SubjectEntity>

    @Query("SELECT * FROM subjects WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SubjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(subject: SubjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(subjects: List<SubjectEntity>)

    @Query("DELETE FROM subjects WHERE id = :id AND isBuiltIn = 0")
    suspend fun deleteCustom(id: String)

    @Query("DELETE FROM subjects")
    suspend fun clear()
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM study_tasks ORDER BY dateEpochDay DESC, createdAtEpochMillis DESC")
    fun observeAll(): Flow<List<StudyTaskEntity>>

    @Query("SELECT * FROM study_tasks ORDER BY dateEpochDay, createdAtEpochMillis")
    suspend fun getAll(): List<StudyTaskEntity>

    @Query("SELECT * FROM study_tasks WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): StudyTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: StudyTaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tasks: List<StudyTaskEntity>)

    @Query("DELETE FROM study_tasks WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE study_tasks SET actualMinutes = MAX(actualMinutes + :minutes, 0), status = CASE WHEN :minutes > 0 AND status = 'NOT_STARTED' THEN 'IN_PROGRESS' ELSE status END, updatedAtEpochMillis = :now WHERE id = :id")
    suspend fun addActualMinutes(id: String, minutes: Int, now: Long)

    @Query("UPDATE study_tasks SET status = 'IN_PROGRESS', updatedAtEpochMillis = :now WHERE id = :id AND status = 'NOT_STARTED'")
    suspend fun markInProgress(id: String, now: Long)

    @Query("SELECT COUNT(*) FROM study_tasks WHERE subjectId = :subjectId")
    suspend fun countBySubject(subjectId: String): Int

    @Query("UPDATE study_tasks SET subjectId = :otherId WHERE subjectId = :subjectId")
    suspend fun moveToOther(subjectId: String, otherId: String)

    @Query("DELETE FROM study_tasks")
    suspend fun clear()
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM study_sessions ORDER BY startEpochMillis")
    fun observeAll(): Flow<List<StudySessionEntity>>

    @Query("SELECT * FROM study_sessions ORDER BY startEpochMillis")
    suspend fun getAll(): List<StudySessionEntity>

    @Query("SELECT * FROM study_sessions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): StudySessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: StudySessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(sessions: List<StudySessionEntity>)

    @Query("DELETE FROM study_sessions WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM study_sessions WHERE taskId = :taskId AND source = 'MANUAL'")
    suspend fun deleteManualForTask(taskId: String)

    @Query("SELECT COALESCE(SUM(durationMinutes), 0) FROM study_sessions WHERE taskId = :taskId AND source != 'MANUAL'")
    suspend fun timedMinutesForTask(taskId: String): Int

    @Query("DELETE FROM study_sessions")
    suspend fun clear()
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM user_settings WHERE id = 1")
    fun observe(): Flow<UserSettingsEntity?>

    @Query("SELECT * FROM user_settings WHERE id = 1")
    suspend fun get(): UserSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: UserSettingsEntity)

    @Query("DELETE FROM user_settings")
    suspend fun clear()
}
