package com.wanlok.scraper.server

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.wanlok.scraper.MainActivity
import com.wanlok.scraper.data.LogRepository
import com.wanlok.scraper.data.LogStatus
import io.ktor.http.ContentType
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Runs the web server. It's a foreground service (with a notification) so the system keeps it
 * alive while the app is closed or the screen is off.
 */
class ServerService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var server: EmbeddedServer<*, *>? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        NotificationManagerCompat.from(this).createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName("Web server")
                .build()
        )
        showNotification("Starting the web server")
        if (server == null) scope.launch { startServer() }
        return START_STICKY
    }

    private fun startServer() {
        val address = "http://${NetworkAddress.localIpv4(this) ?: "localhost"}:$PORT"
        try {
            val page = assets.open("index.html").bufferedReader().use { it.readText() }
            server = embeddedServer(CIO, port = PORT, host = "0.0.0.0") {
                routing {
                    get("/") { call.respondText(page, ContentType.Text.Html) }
                    taskRoutes(applicationContext)
                }
            }.start(wait = false)
        } catch (e: Exception) {
            LogRepository.append(this, LogStatus.STOPPED, "Web server failed: ${e.message}")
            stopSelf()
            return
        }
        showNotification(address)
        LogRepository.append(this, LogStatus.STARTED, "Web server $address")
    }

    override fun onDestroy() {
        val running = server
        server = null
        scope.launch {
            if (running != null) {
                running.stop(gracePeriodMillis = 500, timeoutMillis = 1_000)
                LogRepository.append(applicationContext, LogStatus.STOPPED, "Web server")
            }
            scope.cancel()
        }
    }

    // Calling startForeground again with the same id replaces the notification's text.
    private fun showNotification(text: String) {
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification(text),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )
    }

    private fun notification(text: String): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("Scraper web server")
            .setContentText(text)
            .setContentIntent(openApp)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val PORT = 8080
        private const val CHANNEL_ID = "web_server"
        private const val NOTIFICATION_ID = 1

fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, ServerService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ServerService::class.java))
        }
    }
}