package com.sirius.proxima.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sirius.proxima.data.model.ExamReminder
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamReminderDao {
    @Query("SELECT * FROM exam_reminders ORDER BY examAtMillis ASC")
    fun getAll(): Flow<List<ExamReminder>>

    @Query("SELECT * FROM exam_reminders ORDER BY examAtMillis ASC")
    suspend fun getAllList(): List<ExamReminder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ExamReminder): Long

    @Delete
    suspend fun delete(item: ExamReminder)

    @Query("DELETE FROM exam_reminders")
    suspend fun deleteAll()
}

