package com.example.calllimiter.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo // Required for FOREGROUND_SERVICE_TYPE constants
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat // For stopForeground with behavior flags
import com.example.calllimiter.MainActivity // To open the app when notification is clicked
import com.example.calllimiter.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint // Hilt can inject dependencies here if needed later
class CallLimiterService : Service() {

    private val NOTIFICATION_CHANNEL_ID = "CallLimiterChannel"
    private val NOTIFICATION_CHANNEL_NAME = "Call Limiter Service"
    private val NOTIFICATION_ID = 1 // Must be > 0

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()

        val foregroundServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Determine the foreground service type.
            // This is crucial for Android 14 (API 34) and higher if your service
            // declares a specific type in the manifest (e.g., "phoneCall").
            //
            // IMPORTANT: You MUST ensure your AndroidManifest.xml has the corresponding
            // <service android:foregroundServiceType="phoneCall" ... /> and
            // <uses-permission android:name="android.permission.FOREGROUND_SERVICE_PHONE_CALL" />
            // if you intend to use ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL.
            //
            // If your service does not have a specific type declared in the manifest
            // that requires this parameter on API 34+, then 0 (or ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE)
            // is the appropriate value.
            //
            // For a "CallLimiterService", it's plausible you'd use "phoneCall".
            // Change this if your manifest declares a different type or no specific type.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14+
                // Example: Assuming your service is declared with type "phoneCall"
                // in the AndroidManifest.xml.
                ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
            } else {
                // For Android Q (API 29) to Android 13 (API 33),
                // providing 0 is generally acceptable if no specific type necessitates otherwise.
                // ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE is also 0.
                0
            }
        } else {
            // For versions older than Q, foregroundServiceType is not applicable in this manner.
            // However, the error suggests the 4-argument version is being matched.
            // Passing 0 should be safe if the compiler insists on the 4-argument version.
            0
        }

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            foregroundServiceType // Pass the determined foreground service type
        )

        // If the service is killed by the system due to low memory,
        // START_STICKY will tell the system to try to recreate the service.
        // However, the Intent will be null in this case.
        return START_STICKY
    }

    private fun createNotification(): Notification {
        // Intent to launch the MainActivity when the notification is clicked
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_notification_icon) // REPLACE with your actual small notification icon
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Channel for Call Limiter background service"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        // Consider stopping the foreground service with notification removal if appropriate
        // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { // STOP_FOREGROUND_REMOVE is API 24+
        //    ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        // } else {
        //    stopForeground(true) // Legacy way
        // }
    }
}
