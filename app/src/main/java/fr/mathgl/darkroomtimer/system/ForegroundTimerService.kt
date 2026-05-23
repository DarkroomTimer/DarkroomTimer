package fr.mathgl.darkroomtimer.system

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import fr.mathgl.darkroomtimer.MainActivity
import fr.mathgl.darkroomtimer.R

class ForegroundTimerService : Service() {

    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val remaining = intent.getLongExtra(EXTRA_REMAINING_MS, 0L)
                startForeground(NOTIFICATION_ID, buildNotification(remaining))
            }
            ACTION_UPDATE -> {
                val remaining = intent.getLongExtra(EXTRA_REMAINING_MS, 0L)
                notificationManager.notify(NOTIFICATION_ID, buildNotification(remaining))
            }
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun buildNotification(remainingMs: Long) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Exposition en cours")
            .setContentText(CountdownTimer.formatTime(maxOf(0L, remainingMs)))
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Timer exposition",
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_START  = "fr.mathgl.darkroomtimer.TIMER_START"
        const val ACTION_UPDATE = "fr.mathgl.darkroomtimer.TIMER_UPDATE"
        const val ACTION_STOP   = "fr.mathgl.darkroomtimer.TIMER_STOP"
        const val EXTRA_REMAINING_MS = "remaining_ms"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "darkroom_timer_channel"
    }
}
