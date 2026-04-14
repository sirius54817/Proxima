package com.sirius.proxima.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_sessions")
data class StudySession(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val subjectId: Int,
    val startedAtMillis: Long,
    val durationSeconds: Long,
    val sessionDate: String
)

