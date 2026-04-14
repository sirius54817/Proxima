package com.sirius.proxima.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sirius.proxima.data.model.NoteChecklistItem

@Dao
interface NoteChecklistItemDao {
    @Query("SELECT * FROM note_checklist_items ORDER BY noteId ASC, position ASC")
    suspend fun getAllList(): List<NoteChecklistItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<NoteChecklistItem>)

    @Update
    suspend fun update(item: NoteChecklistItem)

    @Query("DELETE FROM note_checklist_items WHERE noteId = :noteId")
    suspend fun deleteByNoteId(noteId: Int)

    @Query("DELETE FROM note_checklist_items")
    suspend fun deleteAll()
}

