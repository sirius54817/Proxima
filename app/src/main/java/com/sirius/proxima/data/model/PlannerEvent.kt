package com.sirius.proxima.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "planner_events")
data class PlannerEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val eventDate: String,
    val type: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)

object PlannerEventType {
    const val ASSIGNMENT = "Assignment"
    const val EXAM = "Exam"
    const val HOLIDAY = "Holiday"
    const val INTERNAL = "Internal"
}

