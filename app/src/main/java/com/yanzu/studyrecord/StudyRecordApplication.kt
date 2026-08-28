package com.yanzu.studyrecord

import android.app.Application
import com.yanzu.studyrecord.data.AppDatabase
import com.yanzu.studyrecord.data.BackupManager
import com.yanzu.studyrecord.data.StudyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class StudyRecordApplication : Application() {
    val database by lazy { AppDatabase.get(this) }
    val repository by lazy { StudyRepository(database) }
    val backupManager by lazy { BackupManager(database) }

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { repository.ensureSeeded() }
    }
}
