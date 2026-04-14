package com.sirius.proxima.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sirius.proxima.data.model.AssignmentReminder
import kotlinx.coroutines.flow.Flow

@Dao
interface AssignmentReminderDao {
    @Query("SELECT * FROM assignment_reminders ORDER BY dueAtMillis ASC")
    fun getAll(): Flow<List<AssignmentReminder>>

    @Query("SELECT * FROM assignment_reminders ORDER BY dueAtMillis ASC")
    suspend fun getAllList(): List<AssignmentReminder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: AssignmentReminder): Long

    @Delete
    suspend fun delete(item: AssignmentReminder)

    @Query("DELETE FROM assignment_reminders")
    suspend fun deleteAll()
}

