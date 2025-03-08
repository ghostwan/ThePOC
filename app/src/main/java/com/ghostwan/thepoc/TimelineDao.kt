package com.ghostwan.thepoc

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TimelineDao {
    @Query("SELECT * FROM timeline_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<TimelineEvent>>

    @Insert
    suspend fun insertEvent(event: TimelineEvent): Long

    @Query("DELETE FROM timeline_events")
    suspend fun deleteAllEvents()
} 