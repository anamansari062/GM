package com.example.gm.presentation.utils

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.gm.R

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null) {
            // Create and show the notification here
            val notificationManager = ContextCompat.getSystemService(
                context,
                NotificationManager::class.java
            ) as NotificationManager

            // Create a NotificationCompat.Builder and configure it
            val notificationBuilder = NotificationCompat.Builder(context, "001")
                .setSmallIcon(R.drawable.baseline_water_drop_24)
                .setContentTitle("Say gm.")
                .setContentText("have you said gm today?")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

            notificationManager.notify(/* notificationId */ 1, notificationBuilder.build())
        }
    }
}
