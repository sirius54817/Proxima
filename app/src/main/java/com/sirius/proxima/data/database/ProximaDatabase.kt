package com.sirius.proxima.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sirius.proxima.data.dao.SubjectDao
import com.sirius.proxima.data.dao.SubjectAttendanceRecordDao
import com.sirius.proxima.data.dao.TimetableEntryDao
import com.sirius.proxima.data.model.SubjectAttendanceRecord
import com.sirius.proxima.data.model.Subject
import com.sirius.proxima.data.model.TimetableEntry

@Database(
    entities = [Subject::class, TimetableEntry::class, SubjectAttendanceRecord::class],
    version = 5,
    exportSchema = false
)
abstract class ProximaDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun timetableEntryDao(): TimetableEntryDao
    abstract fun subjectAttendanceRecordDao(): SubjectAttendanceRecordDao
}

