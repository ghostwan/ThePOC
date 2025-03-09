package com.ghostwan.thepoc

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TimelineDao {
    @Query("SELECT * FROM timeline_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<TimelineEvent>>

    @Insert
    suspend fun insertEvent(event: TimelineEvent): Long

    @Delete
    suspend fun deleteEvent(event: TimelineEvent)

    @Update
    suspend fun updateEvent(event: TimelineEvent)

    @Query("DELETE FROM timeline_events")
    suspend fun deleteAllEvents()
} 