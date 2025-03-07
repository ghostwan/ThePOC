package com.ghostwan.thepoc

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GeofenceDao {
    @Query("SELECT * FROM geofences")
    fun getAllGeofences(): Flow<List<GeofenceEntity>>
    
    @Insert
    suspend fun insertGeofence(geofence: GeofenceEntity): Long
    
    @Update
    suspend fun updateGeofence(geofence: GeofenceEntity)
    
    @Delete
    suspend fun deleteGeofence(geofence: GeofenceEntity)
    
    @Query("DELETE FROM geofences")
    suspend fun deleteAllGeofences()
} 