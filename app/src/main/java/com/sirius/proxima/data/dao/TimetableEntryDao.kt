package com.sirius.proxima.data.dao

import androidx.room.*
import com.sirius.proxima.data.model.TimetableEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface TimetableEntryDao {
    @Query("SELECT * FROM timetable_entries WHERE dayOfWeek = :dayOfWeek ORDER BY hourSlot ASC")
    fun getEntriesByDay(dayOfWeek: Int): Flow<List<TimetableEntry>>

    @Query("SELECT * FROM timetable_entries ORDER BY dayOfWeek ASC, hourSlot ASC")
    fun getAllEntries(): Flow<List<TimetableEntry>>

    @Query("SELECT * FROM timetable_entries ORDER BY dayOfWeek ASC, hourSlot ASC")
    suspend fun getAllEntriesList(): List<TimetableEntry>

    @Query("SELECT * FROM timetable_entries WHERE dayOfWeek = :dayOfWeek AND hourSlot = :hourSlot LIMIT 1")
    suspend fun getEntryBySlot(dayOfWeek: Int, hourSlot: Int): TimetableEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: TimetableEntry): Long

    @Update
    suspend fun updateEntry(entry: TimetableEntry)

    @Delete
    suspend fun deleteEntry(entry: TimetableEntry)

    @Query("DELETE FROM timetable_entries WHERE id = :id")
    suspend fun deleteEntryById(id: Int)

    @Query("UPDATE timetable_entries SET subjectName = '[Deleted Subject]', subjectId = NULL WHERE subjectId = :subjectId")
    suspend fun markSubjectDeleted(subjectId: Int)

    @Query("UPDATE timetable_entries SET subjectName = '[Deleted Subject]', subjectId = NULL WHERE subjectId IN (:subjectIds)")
    suspend fun markSubjectsDeleted(subjectIds: List<Int>)

    @Query("DELETE FROM timetable_entries")
    suspend fun deleteAllEntries()
}

