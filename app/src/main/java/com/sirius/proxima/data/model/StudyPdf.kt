package com.sirius.proxima.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_pdfs")
data class StudyPdf(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val subjectId: Int,
    val title: String,
    val filePath: String,
    val addedAtMillis: Long = System.currentTimeMillis()
)

