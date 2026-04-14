package com.sirius.proxima.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sirius.proxima.data.model.PlannerEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannerEventDao {
    @Query("SELECT * FROM planner_events ORDER BY eventDate ASC")
    fun getAll(): Flow<List<PlannerEvent>>

    @Query("SELECT * FROM planner_events WHERE eventDate BETWEEN :startDate AND :endDate ORDER BY eventDate ASC")
    fun getBetween(startDate: String, endDate: String): Flow<List<PlannerEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: PlannerEvent)

    @Delete
    suspend fun delete(event: PlannerEvent)

    @Query("DELETE FROM planner_events")
    suspend fun deleteAll()
}

