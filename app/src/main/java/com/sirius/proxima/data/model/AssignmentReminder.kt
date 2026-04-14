package com.sirius.proxima.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "assignment_reminders")
data class AssignmentReminder(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val dueAtMillis: Long,
    val remindAtMillis: Long,
    val isCompleted: Boolean = false
)

