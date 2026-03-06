package com.sirius.proxima.data.repository

import com.sirius.proxima.data.dao.TimetableEntryDao
import com.sirius.proxima.data.model.TimetableEntry
import kotlinx.coroutines.flow.Flow

class TimetableRepository(
    private val timetableEntryDao: TimetableEntryDao
) {
    fun getEntriesByDay(dayOfWeek: Int): Flow<List<TimetableEntry>> =
        timetableEntryDao.getEntriesByDay(dayOfWeek)

    fun getAllEntries(): Flow<List<TimetableEntry>> = timetableEntryDao.getAllEntries()

    suspend fun getAllEntriesList(): List<TimetableEntry> = timetableEntryDao.getAllEntriesList()

    suspend fun getEntryBySlot(dayOfWeek: Int, hourSlot: Int): TimetableEntry? =
        timetableEntryDao.getEntryBySlot(dayOfWeek, hourSlot)

    suspend fun insertEntry(entry: TimetableEntry): Long = timetableEntryDao.insertEntry(entry)

    suspend fun updateEntry(entry: TimetableEntry) = timetableEntryDao.updateEntry(entry)

    suspend fun deleteEntry(entry: TimetableEntry) = timetableEntryDao.deleteEntry(entry)

    suspend fun deleteEntryById(id: Int) = timetableEntryDao.deleteEntryById(id)

    suspend fun deleteAll() = timetableEntryDao.deleteAllEntries()
}
