package com.sirius.proxima.notification

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.sirius.proxima.ProximaApplication
import com.sirius.proxima.R

object NotificationHelper {

    fun showClassNotification(
        context: Context,
        subjectName: String,
        classNumber: String,
        percentage: Float,
        notificationId: Int
    ) {
        val notification = NotificationCompat.Builder(context, ProximaApplication.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Upcoming Class: $subjectName - $classNumber")
            .setContentText("Current attendance: ${"%.1f".format(percentage)}% | Class in 5 minutes")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notification)
    }

    fun sendDemoNotification(context: Context) {
        val notification = NotificationCompat.Builder(context, ProximaApplication.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Upcoming Class: Mathematics - Room 301")
            .setContentText("Current attendance: 82.5% | Class in 5 minutes")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(99999, notification)
    }
}


