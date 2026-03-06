package com.sirius.proxima.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.sirius.proxima.data.model.TimetableEntry
import com.sirius.proxima.receiver.AlarmReceiver
import java.util.Calendar

object AlarmScheduler {

    private fun getRequestCode(dayOfWeek: Int, hourSlot: Int): Int {
        return dayOfWeek * 100 + hourSlot
    }

    fun scheduleAlarm(context: Context, entry: TimetableEntry, subjectPercentage: Float) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmReceiver::class.java).apply {
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

