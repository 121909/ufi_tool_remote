package com.example.ufitoolsremote.easytier

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.ufitoolsremote.MainActivity
import com.example.ufitoolsremote.R
import com.example.ufitoolsremote.UfiRemoteApplication
import com.example.ufitoolsremote.model.EasyTierSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class EasyTierService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopEasyTier()
                return START_NOT_STICKY
            }
            ACTION_RESTART -> restartEasyTier()
            else -> startEasyTier()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun startEasyTier() {
        val settings = (applicationContext as UfiRemoteApplication).container.settingsRepository.current().easyTier
        startForeground(NOTIFICATION_ID, notification("EasyTier 正在启动", statusLine(settings)))
        scope.launch {
            runEasyTier(settings)
        }
    }

    private fun restartEasyTier() {
        val settings = (applicationContext as UfiRemoteApplication).container.settingsRepository.current().easyTier
        startForeground(NOTIFICATION_ID, notification("EasyTier 正在重启", statusLine(settings)))
        scope.launch {
            EasyTierRuntime.stop()
            runEasyTier((applicationContext as UfiRemoteApplication).container.settingsRepository.current().easyTier)
        }
    }

    private fun stopEasyTier() {
        scope.launch {
            EasyTierRuntime.stop()
            (applicationContext as UfiRemoteApplication).container.settingsRepository.updateEasyTier {
                copy(enabled = false)
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun notify(title: String, text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification(title, text))
    }

    private fun runEasyTier(settings: EasyTierSettings) {
        val socks = if (settings.socks5Enabled) {
            "SOCKS5 ${settings.socks5Host}:${settings.socks5Port}"
        } else {
            "SOCKS5 未启用"
        }
        val currentStatus = EasyTierRuntime.collectStatus()
        if (currentStatus.runningCount > 0) {
            notify("EasyTier 运行中", socks)
            return
        }
        when (val result = EasyTierRuntime.start(settings)) {
            EasyTierRuntimeResult.Success -> {
                notify("EasyTier 运行中", socks)
            }
            is EasyTierRuntimeResult.Failure -> {
                val latestStatus = EasyTierRuntime.collectStatus()
                if (latestStatus.runningCount > 0) {
                    notify("EasyTier 运行中", socks)
                    return
                }
                notify("EasyTier 启动失败", result.message)
                stopSelf()
            }
        }
    }

    private fun notification(title: String, text: String): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            6100,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_easytier)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "EasyTier",
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
    }

    private fun statusLine(settings: EasyTierSettings): String {
        return if (settings.socks5Enabled) {
            "no_tun, SOCKS5 ${settings.socks5Host}:${settings.socks5Port}"
        } else {
            "no_tun, SOCKS5 未启用"
        }
    }

    companion object {
        private const val CHANNEL_ID = "easytier"
        private const val NOTIFICATION_ID = 6201
        private const val ACTION_STOP = "com.example.ufitoolsremote.easytier.STOP"
        private const val ACTION_RESTART = "com.example.ufitoolsremote.easytier.RESTART"

        fun start(context: Context) {
            val intent = Intent(context, EasyTierService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.startService(Intent(context, EasyTierService::class.java).setAction(ACTION_STOP))
        }

        fun restart(context: Context) {
            val intent = Intent(context, EasyTierService::class.java).setAction(ACTION_RESTART)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
