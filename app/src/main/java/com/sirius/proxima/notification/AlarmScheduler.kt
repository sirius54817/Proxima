package com.sirius.proxima.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.sirius.proxima.data.model.TimetableEntry
import com.sirius.proxima.receiver.AlarmReceiver
import java.util.Calendar

object AlarmScheduler {

    const val TYPE_CLASS = "class"
    const val TYPE_ASSIGNMENT = "assignment"
    const val TYPE_EXAM = "exam"

    private const val ASSIGNMENT_REQUEST_OFFSET = 100000
    private const val EXAM_REQUEST_OFFSET = 200000

    private fun getRequestCode(dayOfWeek: Int, hourSlot: Int): Int {
        return dayOfWeek * 100 + hourSlot
    }

    fun scheduleAlarm(context: Context, entry: TimetableEntry, subjectPercentage: Float) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarm_type", TYPE_CLASS)
            putExtra("subject_name", entry.subjectName)
            putExtra("class_number", entry.classNumber)
            putExtra("percentage", subjectPercentage)
            putExtra("entry_id", entry.id)
        }

        val requestCode = getRequestCode(entry.dayOfWeek, entry.hourSlot)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Schedule for 5 minutes before the class hour
        val calendar = getNextAlarmTime(entry.dayOfWeek, entry.hourSlot)

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } catch (e: SecurityException) {
            // Exact alarm permission not granted, fall back to inexact
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun cancelAlarm(context: Context, dayOfWeek: Int, hourSlot: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val requestCode = getRequestCode(dayOfWeek, hourSlot)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun reminderRequestCode(type: String, id: Int): Int {
        return when (type) {
            TYPE_ASSIGNMENT -> ASSIGNMENT_REQUEST_OFFSET + id
            TYPE_EXAM -> EXAM_REQUEST_OFFSET + id
            else -> id
        }
    }

    fun scheduleAssignmentReminder(context: Context, reminderId: Int, title: String, dueDateText: String, triggerAtMillis: Long) {
        scheduleOneTimeReminder(
            context = context,
            type = TYPE_ASSIGNMENT,
            reminderId = reminderId,
            title = title,
            timeText = dueDateText,
            triggerAtMillis = triggerAtMillis
        )
    }

    fun scheduleExamReminder(context: Context, reminderId: Int, subject: String, examTimeText: String, triggerAtMillis: Long) {
        scheduleOneTimeReminder(
            context = context,
            type = TYPE_EXAM,
            reminderId = reminderId,
            title = subject,
            timeText = examTimeText,
            triggerAtMillis = triggerAtMillis
        )
    }

    fun cancelAssignmentReminder(context: Context, reminderId: Int) {
        cancelOneTimeReminder(context, TYPE_ASSIGNMENT, reminderId)
    }

    fun cancelExamReminder(context: Context, reminderId: Int) {
        cancelOneTimeReminder(context, TYPE_EXAM, reminderId)
    }

    private fun scheduleOneTimeReminder(
        context: Context,
        type: String,
        reminderId: Int,
        title: String,
        timeText: String,
        triggerAtMillis: Long
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarm_type", type)
            putExtra("item_id", reminderId)
            putExtra("title", title)
            putExtra("time_text", timeText)
        }

        val requestCode = reminderRequestCode(type, reminderId)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } catch (e: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun cancelOneTimeReminder(context: Context, type: String, reminderId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderRequestCode(type, reminderId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun getNextAlarmTime(dayOfWeek: Int, hourSlot: Int): Calendar {
        val now = Calendar.getInstance()
        val alarm = Calendar.getInstance().apply {
            // dayOfWeek: 1=Monday..7=Sunday -> Calendar: Sun=1, Mon=2, etc.
            val calDay = if (dayOfWeek == 7) Calendar.SUNDAY else dayOfWeek + 1
            set(Calendar.DAY_OF_WEEK, calDay)
            set(Calendar.HOUR_OF_DAY, hourSlot)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // 5 minutes before
            add(Calendar.MINUTE, -5)
        }

        // If the alarm time has already passed this week, schedule for next week
        if (alarm.before(now)) {
            alarm.add(Calendar.WEEK_OF_YEAR, 1)
        }

        return alarm
    }

    fun scheduleAllAlarms(context: Context, entries: List<TimetableEntry>, subjectPercentages: Map<Int?, Float>) {
        entries.forEach { entry ->
            val percentage = entry.subjectId?.let { subjectPercentages[it] } ?: 0f
            scheduleAlarm(context, entry, percentage)
        }
    }

    fun cancelAllAlarms(context: Context, entries: List<TimetableEntry>) {
        entries.forEach { entry ->
            cancelAlarm(context, entry.dayOfWeek, entry.hourSlot)
        }
    }
}

