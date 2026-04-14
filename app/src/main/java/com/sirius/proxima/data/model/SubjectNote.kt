package com.sirius.proxima.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subject_notes")
data class SubjectNote(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val subjectId: Int,
    val title: String,
    val content: String,
    val isChecklist: Boolean = false,
    val isPinned: Boolean = false,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

