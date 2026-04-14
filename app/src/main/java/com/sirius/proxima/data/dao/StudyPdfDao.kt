package com.sirius.proxima.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sirius.proxima.data.model.StudyPdf
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyPdfDao {
    @Query("SELECT * FROM study_pdfs ORDER BY subjectId ASC, title ASC")
    fun getAll(): Flow<List<StudyPdf>>

    @Query("SELECT * FROM study_pdfs ORDER BY subjectId ASC, title ASC")
    suspend fun getAllList(): List<StudyPdf>

    @Query("SELECT * FROM study_pdfs WHERE id = :id LIMIT 1")
    fun getById(id: Int): Flow<StudyPdf?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: StudyPdf): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<StudyPdf>)

    @Delete
    suspend fun delete(item: StudyPdf)

    @Query("DELETE FROM study_pdfs")
    suspend fun deleteAll()
}


