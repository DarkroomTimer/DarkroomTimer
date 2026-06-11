package fr.mathgl.darkroomtimer.system

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.net.wifi.WifiManager.WifiLock
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import fr.mathgl.darkroomtimer.MainActivity
import fr.mathgl.darkroomtimer.R

class ForegroundTimerService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

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

                // A repeated START must not overwrite held locks (leak until timeout)
                releaseLocks()

                // Acquire locks to prevent CPU/WiFi sleep
                val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
                val lockTimeout = if (remaining > 0) remaining + LOCK_MARGIN_MS else MAX_LOCK_MS
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DarkroomTimer:ExposureLock").apply { acquire(lockTimeout) }

                val wm = getSystemService(Context.WIFI_SERVICE) as WifiManager
                val lockType = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    WifiManager.WIFI_MODE_FULL_LOW_LATENCY
                } else {
                    WifiManager.WIFI_MODE_FULL
                }
                wifiLock = wm.createWifiLock(lockType, "DarkroomTimer:WifiLock").apply { acquire() }

                startForeground(NOTIFICATION_ID, buildNotification(remaining))
            }
            ACTION_UPDATE -> {
                val remaining = intent.getLongExtra(EXTRA_REMAINING_MS, 0L)
                notificationManager.notify(NOTIFICATION_ID, buildNotification(remaining))
            }
            ACTION_STOP -> {
                releaseLocks()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        releaseLocks()
        super.onDestroy()
    }

    private fun releaseLocks() {
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
            if (wifiLock?.isHeld == true) wifiLock?.release()
        } finally {
            wakeLock = null
            wifiLock = null
        }
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Timer exposition",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val ACTION_START  = "fr.mathgl.darkroomtimer.TIMER_START"
        const val ACTION_UPDATE = "fr.mathgl.darkroomtimer.TIMER_UPDATE"
        const val ACTION_STOP   = "fr.mathgl.darkroomtimer.TIMER_STOP"
        const val EXTRA_REMAINING_MS = "remaining_ms"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "darkroom_timer_channel"
        private const val LOCK_MARGIN_MS = 60_000L
        private const val MAX_LOCK_MS = 3_600_000L
    }
}
