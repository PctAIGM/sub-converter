package com.subconverter.server

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.subconverter.MainActivity
import com.subconverter.R
import com.subconverter.core.AppContainer
import com.subconverter.i18n.AppI18n
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class LocalHttpServerService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer.get(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopFromUser()
            return START_NOT_STICKY
        }

        startInForeground(AppI18n.text(this, "HTTP 服务启动中"))
        scope.launch {
            val settings = container.settingsStore.current()
            if (!settings.enabled) {
                stopSelf()
                return@launch
            }

            runCatching {
                if (!container.localHttpServer.running.value) {
                    container.localHttpServer.start(settings)
                }
            }.onSuccess {
                updateNotification(
                    AppI18n.format(
                        this@LocalHttpServerService,
                        "端口 %d · %s",
                        settings.port,
                        AppI18n.text(this@LocalHttpServerService, if (settings.allowLan) "局域网访问" else "仅本机访问"),
                    ),
                )
            }.onFailure {
                container.settingsStore.update(settings.copy(enabled = false))
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        container.localHttpServer.stop()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun stopFromUser() {
        scope.launch {
            val settings = container.settingsStore.current()
            container.settingsStore.update(settings.copy(enabled = false))
            container.localHttpServer.stop()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun startInForeground(contentText: String) {
        val notification = buildNotification(contentText)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(contentText: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(contentText))
    }

    private fun buildNotification(contentText: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = Intent(this, LocalHttpServerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_http_service)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(contentText)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(R.drawable.ic_stat_http_service, AppI18n.text(this, "停止"), stopPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            AppI18n.text(this, "本地 HTTP 服务"),
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val ACTION_START = "com.subconverter.action.START_HTTP_SERVER"
        private const val ACTION_STOP = "com.subconverter.action.STOP_HTTP_SERVER"
        private const val CHANNEL_ID = "local_http_server"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, LocalHttpServerService::class.java).apply {
                action = ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, LocalHttpServerService::class.java).apply {
                    action = ACTION_STOP
                },
            )
        }
    }
}
