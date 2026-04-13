package com.sirius.proxima.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val totalClasses: Int = 0,
    val attendedClasses: Int = 0,
    val sisPresent: Int? = null,
    val sisAbsent: Int? = null,
    val sisOnDuty: Int? = null
) {
    val percentage: Float
        get() = if (totalClasses > 0) (attendedClasses.toFloat() / totalClasses.toFloat()) * 100f else 0f

    val canMissClasses: Int
        get() {
            if (percentage < 75f) return 0
            val x = ((attendedClasses - 0.75 * totalClasses) / 0.75).toInt()
            return maxOf(x, 0)
        }

    val needToAttendClasses: Int
        get() {
            if (percentage >= 75f) return 0
            val x = kotlin.math.ceil((0.75 * totalClasses - attendedClasses) / 0.25).toInt()
            return maxOf(x, 0)
        }
}

