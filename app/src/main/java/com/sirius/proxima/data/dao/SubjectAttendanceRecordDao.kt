package com.sirius.proxima.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sirius.proxima.data.model.SubjectAttendanceRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectAttendanceRecordDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRecord(record: SubjectAttendanceRecord)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRecords(records: List<SubjectAttendanceRecord>)

    @Query("SELECT * FROM subject_attendance_records ORDER BY date DESC, id DESC")
    suspend fun getAllRecordsList(): List<SubjectAttendanceRecord>

    @Query("DELETE FROM subject_attendance_records")
    suspend fun deleteAllRecords()

    @Query("SELECT * FROM subject_attendance_records WHERE subjectId = :subjectId ORDER BY date DESC, id DESC")
    fun getRecordsBySubjectId(subjectId: Int): Flow<List<SubjectAttendanceRecord>>
}

