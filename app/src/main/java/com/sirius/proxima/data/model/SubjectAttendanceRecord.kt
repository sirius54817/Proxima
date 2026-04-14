package com.sirius.proxima.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "subject_attendance_records",
    indices = [Index(value = ["subjectId", "status", "date", "slotName"], unique = true)]
)
data class SubjectAttendanceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val subjectId: Int,
    val status: String,
    val date: String,
    val slotName: String? = null,
    val recordedAtMillis: Long = System.currentTimeMillis()
)

object AttendanceStatus {
    const val PRESENT = "Present"
    const val ABSENT = "Absent"
    const val ON_DUTY = "On Duty"
}

