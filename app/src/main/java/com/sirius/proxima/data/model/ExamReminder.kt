package com.sirius.proxima.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exam_reminders")
data class ExamReminder(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val subject: String,
    val examAtMillis: Long,
    val remindAtMillis: Long,
    val isCompleted: Boolean = false
)

