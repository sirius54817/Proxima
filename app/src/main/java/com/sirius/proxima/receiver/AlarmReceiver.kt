package com.sirius.proxima.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sirius.proxima.notification.NotificationHelper

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val subjectName = intent.getStringExtra("subject_name") ?: "Unknown"
        val classNumber = intent.getStringExtra("class_number") ?: ""
        val percentage = intent.getFloatExtra("percentage", 0f)
        val entryId = intent.getIntExtra("entry_id", 0)

        NotificationHelper.showClassNotification(
            context = context,
            subjectName = subjectName,
            classNumber = classNumber,
            percentage = percentage,
            notificationId = entryId
        )
    }
}

