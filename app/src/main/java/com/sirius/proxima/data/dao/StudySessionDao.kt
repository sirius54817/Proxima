package com.sirius.proxima.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sirius.proxima.data.model.StudySession
import kotlinx.coroutines.flow.Flow

@Dao
interface StudySessionDao {
    @Query("SELECT * FROM study_sessions ORDER BY startedAtMillis DESC")
    fun getAll(): Flow<List<StudySession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: StudySession)

    @Query("SELECT DISTINCT sessionDate FROM study_sessions ORDER BY sessionDate DESC")
    suspend fun getDistinctSessionDates(): List<String>

    @Query("SELECT COALESCE(SUM(durationSeconds), 0) FROM study_sessions WHERE sessionDate BETWEEN :fromDate AND :toDate")
    fun getTotalDurationBetween(fromDate: String, toDate: String): Flow<Long>

    @Query("SELECT COALESCE(SUM(durationSeconds), 0) FROM study_sessions WHERE subjectId = :subjectId")
    fun getTotalDurationBySubject(subjectId: Int): Flow<Long>

    @Query("DELETE FROM study_sessions")
    suspend fun deleteAll()
}

