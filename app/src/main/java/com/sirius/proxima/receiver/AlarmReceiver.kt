package com.sirius.proxima.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sirius.proxima.notification.AlarmScheduler
import com.sirius.proxima.notification.NotificationHelper

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.getStringExtra("alarm_type") ?: AlarmScheduler.TYPE_CLASS) {
            AlarmScheduler.TYPE_ASSIGNMENT -> {
                val title = intent.getStringExtra("title") ?: "Assignment"
                val timeText = intent.getStringExtra("time_text") ?: "soon"
                val itemId = intent.getIntExtra("item_id", 0)
                NotificationHelper.showAssignmentNotification(context, title, timeText, 300000 + itemId)
                return
            }

            AlarmScheduler.TYPE_EXAM -> {
                val subject = intent.getStringExtra("title") ?: "Exam"
                val timeText = intent.getStringExtra("time_text") ?: "soon"
                val itemId = intent.getIntExtra("item_id", 0)
                NotificationHelper.showExamNotification(context, subject, timeText, 400000 + itemId)
                return
            }
        }

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

