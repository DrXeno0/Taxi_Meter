package com.nocturnal.taximeter.utils

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.nocturnal.taximeter.R
import pub.devrel.easypermissions.EasyPermissions


class NotificationHelper(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "fare_channel",
                "Ride Fare Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }
    }
    fun checkNotifficationEnabled(): Boolean {
        return notificationManager.areNotificationsEnabled()
    }

    fun enableNotifications(activity: Activity) {
        EasyPermissions.requestPermissions(activity, "Enable notifications", 0, android.Manifest.permission.POST_NOTIFICATIONS)
    }

    fun sendFareNotification(fare: Double, distance: Double, time: Long) {
        val notification = NotificationCompat.Builder(context, "fare_channel")
            .setContentTitle("Ride Completed")
            .setContentText("Fare: $fare DH | Distance: $distance km | Time: $time min")
            .setSmallIcon(R.drawable.baseline_taxi_alert_24)
            .build()

        notificationManager.notify(1, notification)
    }
}
