package com.sirius.proxima.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.room.Room
import com.sirius.proxima.data.database.ProximaDatabase
import com.sirius.proxima.notification.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            CoroutineScope(Dispatchers.IO).launch {
                val db = Room.databaseBuilder(
                    context.applicationContext,
                    ProximaDatabase::class.java,
                    "proxima_database"
                ).fallbackToDestructiveMigration(true).build()

                val entries = db.timetableEntryDao().getAllEntriesList()
                val subjects = db.subjectDao().getAllSubjectsList()
                val assignments = db.assignmentReminderDao().getAllList()
                val exams = db.examReminderDao().getAllList()
                val percentages: Map<Int?, Float> = subjects.associate { it.id as Int? to it.percentage }

                AlarmScheduler.scheduleAllAlarms(context, entries, percentages)

                val now = System.currentTimeMillis()
                assignments.forEach { item ->
                    if (item.remindAtMillis > now) {
                        AlarmScheduler.scheduleAssignmentReminder(
                            context = context,
                            reminderId = item.id,
                            title = item.title,
                            dueDateText = java.time.Instant.ofEpochMilli(item.dueAtMillis)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate()
                                .toString(),
                            triggerAtMillis = item.remindAtMillis
                        )
                    }
                }

                exams.forEach { item ->
                    if (item.remindAtMillis > now) {
                        AlarmScheduler.scheduleExamReminder(
                            context = context,
                            reminderId = item.id,
                            subject = item.subject,
                            examTimeText = java.time.Instant.ofEpochMilli(item.examAtMillis)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDateTime()
                                .toString(),
                            triggerAtMillis = item.remindAtMillis
                        )
                    }
                }
                db.close()
            }
        }
    }
}


