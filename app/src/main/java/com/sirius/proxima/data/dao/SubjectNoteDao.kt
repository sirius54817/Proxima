package com.sirius.proxima.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.sirius.proxima.data.model.NoteWithChecklist
import com.sirius.proxima.data.model.SubjectNote
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectNoteDao {
    @Query("SELECT * FROM subject_notes WHERE id = :noteId LIMIT 1")
    suspend fun getById(noteId: Int): SubjectNote?

    @Transaction
    @Query("SELECT * FROM subject_notes WHERE id = :noteId LIMIT 1")
    fun getNoteWithChecklistById(noteId: Int): Flow<NoteWithChecklist?>

    @Transaction
    @Query(
        """
        SELECT * FROM subject_notes
        ORDER BY isPinned DESC, updatedAtMillis DESC
        """
    )
    fun getAllNotes(): Flow<List<NoteWithChecklist>>

    @Query("SELECT * FROM subject_notes")
    suspend fun getAllNotesList(): List<SubjectNote>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: SubjectNote): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notes: List<SubjectNote>)

    @Update
    suspend fun update(note: SubjectNote)

    @Query("DELETE FROM subject_notes WHERE id = :noteId")
    suspend fun deleteById(noteId: Int)

    @Query("DELETE FROM subject_notes")
    suspend fun deleteAll()
}



