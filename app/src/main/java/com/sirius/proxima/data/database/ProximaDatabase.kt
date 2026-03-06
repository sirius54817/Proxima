package com.sirius.proxima.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sirius.proxima.data.dao.SubjectDao
import com.sirius.proxima.data.dao.TimetableEntryDao
import com.sirius.proxima.data.model.Subject
import com.sirius.proxima.data.model.TimetableEntry

@Database(
    entities = [Subject::class, TimetableEntry::class],
    version = 1,
    exportSchema = false
)
abstract class ProximaDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun timetableEntryDao(): TimetableEntryDao
}

