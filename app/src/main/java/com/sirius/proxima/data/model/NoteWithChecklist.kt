package com.sirius.proxima.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class NoteWithChecklist(
    @Embedded val note: SubjectNote,
    @Relation(
        parentColumn = "id",
        entityColumn = "noteId"
    )
    val checklistItems: List<NoteChecklistItem>
)

