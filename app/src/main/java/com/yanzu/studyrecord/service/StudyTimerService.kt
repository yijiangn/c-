package com.yanzu.studyrecord.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.yanzu.studyrecord.MainActivity
import com.yanzu.studyrecord.R
import com.yanzu.studyrecord.StudyRecordApplication
import com.yanzu.studyrecord.data.TimerState
import com.yanzu.studyrecord.util.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class StudyTimerService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var app: StudyRecordApplication
    private var ticker: Job? = null

    override fun onCreate() {
        super.onCreate()
        app = application as StudyRecordApplication
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> scope.launch { app.repository.pauseTimer() }
            ACTION_RESUME -> scope.launch { app.repository.resumeTimer() }
            ACTION_FINISH -> scope.launch { app.repository.finishTimer(true); stopSelf() }
        }
        startForeground(NOTIFICATION_ID, buildNotification("正在准备计时…", false))
        if (ticker?.isActive != true) ticker = scope.launch { tick() }
        return START_STICKY
    }

    private suspend fun tick() {
        while (true) {
            val settings = app.repository.settings.first()
            if (settings.timerState == TimerState.IDLE.name) { stopSelf(); return }
            val elapsed = app.repository.timerElapsedSeconds(settings)
            val pomodoroDone = settings.timerTargetSeconds > 0 && elapsed >= settings.timerTargetSeconds
            if (pomodoroDone) {
                app.repository.finishTimer(true)
                NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, buildNotification("番茄钟完成，学习时长已保存", false))
                delay(1200); stopSelf(); return
            }
            val display = if (settings.timerTargetSeconds > 0) (settings.timerTargetSeconds - elapsed).coerceAtLeast(0) else elapsed
            NotificationManagerCompat.from(this).notify(
                NOTIFICATION_ID,
                buildNotification(DateUtils.formatTimer(display), settings.timerState == TimerState.RUNNING.name)
            )
            delay(1000)
        }
    }

    private fun buildNotification(text: String, running: Boolean): Notification {
        val open = PendingIntent.getActivity(this, 10, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val toggleAction = if (running) ACTION_PAUSE else ACTION_RESUME
        val toggleText = if (running) "暂停" else "继续"
        val toggle = PendingIntent.getService(this, 11, Intent(this, StudyTimerService::class.java).setAction(toggleAction), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val finish = PendingIntent.getService(this, 12, Intent(this, StudyTimerService::class.java).setAction(ACTION_FINISH), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("学习记录 · 正在计时")
            .setContentText(text)
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .addAction(0, toggleText, toggle)
            .addAction(0, "结束并保存", finish)
            .build()
    }

    private fun createChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "学习计时", NotificationManager.IMPORTANCE_LOW).apply {
            description = "显示正在进行的本地学习计时"
            setSound(null, null)
        }
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() { ticker?.cancel(); scope.cancel(); super.onDestroy() }

    companion object {
        private const val CHANNEL_ID = "study_timer"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_PAUSE = "study.timer.PAUSE"
        const val ACTION_RESUME = "study.timer.RESUME"
        const val ACTION_FINISH = "study.timer.FINISH"
    }
}
