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
                ).build()

                val entries = db.timetableEntryDao().getAllEntriesList()
                val subjects = db.subjectDao().getAllSubjectsList()
                val percentages: Map<Int?, Float> = subjects.associate { it.id as Int? to it.percentage }

                AlarmScheduler.scheduleAllAlarms(context, entries, percentages)
                db.close()
            }
        }
    }
}


