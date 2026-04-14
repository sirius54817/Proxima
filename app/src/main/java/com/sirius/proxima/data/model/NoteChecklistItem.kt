package com.sirius.proxima.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "note_checklist_items")
data class NoteChecklistItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val noteId: Int,
    val text: String,
    val isChecked: Boolean = false,
    val position: Int = 0
)

