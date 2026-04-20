package com.sirius.proxima.data.dao

import androidx.room.*
import com.sirius.proxima.data.model.Subject
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects ORDER BY isHidden ASC, name ASC")
    fun getAllSubjects(): Flow<List<Subject>>

    @Query("SELECT * FROM subjects ORDER BY isHidden ASC, name ASC")
    suspend fun getAllSubjectsList(): List<Subject>

    @Query("SELECT * FROM subjects WHERE id = :id")
    suspend fun getSubjectById(id: Int): Subject?

    @Query("SELECT * FROM subjects WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getSubjectByName(name: String): Subject?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: Subject): Long

    @Update
    suspend fun updateSubject(subject: Subject)

    @Delete
    suspend fun deleteSubject(subject: Subject)

    @Query("UPDATE subjects SET attendedClasses = attendedClasses + 1, totalClasses = totalClasses + 1 WHERE id = :id")
    suspend fun markPresent(id: Int)

    @Query("UPDATE subjects SET totalClasses = totalClasses + 1 WHERE id = :id")
    suspend fun markAbsent(id: Int)

    @Query("UPDATE subjects SET attendedClasses = attendedClasses + 1, totalClasses = totalClasses + 1 WHERE id = :id")
    suspend fun markOnDuty(id: Int)

    @Query("UPDATE subjects SET isHidden = 1 WHERE id = :id")
    suspend fun hideSubject(id: Int)

    @Query("UPDATE subjects SET isHidden = 1 WHERE id IN (:ids)")
    suspend fun hideSubjects(ids: List<Int>)

    @Query("UPDATE subjects SET isHidden = 0 WHERE id = :id")
    suspend fun unhideSubject(id: Int)

    @Query("DELETE FROM subjects WHERE id IN (:ids)")
    suspend fun deleteSubjectsByIds(ids: List<Int>)

    @Query("DELETE FROM subjects")
    suspend fun deleteAllSubjects()
}

