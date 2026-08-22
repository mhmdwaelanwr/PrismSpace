package com.prismspace.container.service

import android.app.Service
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.prismspace.container.core.PrismEngineFacade
import com.prismspace.container.utils.Slog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * PrismEngineService: A compliant foreground service that keeps the Prism Space engine warm
 * while adhering to Google Play's Background Execution Limits.
 *
 * This service:
 * - Initializes and maintains PrismSpaceCore readiness
 * - Displays a persistent, low-priority notification (Google Play compliant)
 * - Enables instant app launches from the virtual container
 * - Properly manages lifecycle to prevent background abuse
 */
class PrismEngineService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "PrismEngineService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "prism_engine_channel"
        private const val CHANNEL_NAME = "Prism Engine Status"

        @JvmStatic
        fun startService(context: Context) {
            val intent = Intent(context, PrismEngineService::class.java)
            try {
                ContextCompat.startForegroundService(context, intent)
                Slog.d(TAG, "Foreground service start request sent")
            } catch (e: Exception) {
                Slog.e(TAG, "Failed to start foreground service", e)
            }
        }

        @JvmStatic
        fun stopService(context: Context) {
            val intent = Intent(context, PrismEngineService::class.java)
            context.stopService(intent)
            Slog.d(TAG, "Foreground service stop request sent")
        }
    }

    override fun onCreate() {
        super.onCreate()
        Slog.d(TAG, "PrismEngineService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Slog.d(TAG, "onStartCommand called")

        // Enter foreground immediately to satisfy the platform's foreground-service start deadline.
        val notification = buildPrismNotification()
        try {
            startForeground(NOTIFICATION_ID, notification)
            Slog.d(TAG, "Foreground notification started")
        } catch (e: Exception) {
            Slog.e(TAG, "Failed to start foreground", e)
            stopSelfResult(startId)
            return START_NOT_STICKY
        }

        serviceScope.launch {
            try {
                if (!PrismEngineFacade.initEngine(applicationContext)) {
                    Slog.w(TAG, "Engine initialization returned false")
                } else {
                    Slog.d(TAG, "Engine initialized successfully")
                }
            } catch (e: Exception) {
                Slog.e(TAG, "Engine initialization exception", e)
            }
        }


        // Return START_STICKY to auto-restart if killed by system
        return START_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
        Slog.d(TAG, "PrismEngineService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Build a sleek, low-priority, persistent notification for Google Play compliance.
     */
    private fun buildPrismNotification(): android.app.Notification {
        // Create notification channel (required for API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Prism Space engine status and virtual environment security"
                // Allow vibration and sound but at low priority
                enableVibration(false)
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // Build notification with premium styling
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Prism Space")
            .setContentText("Securing your virtual environment...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // Optional: Add a subtle color accent
            .setColor(0xFF00E5FF.toInt()) // Cyan accent from theme
            .build()
    }
}

