package com.ghostwan.thepoc

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.android.gms.maps.model.LatLng

@Entity(tableName = "geofences")
data class GeofenceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Float = 100f
) {
    fun toLatLng(): LatLng = LatLng(latitude, longitude)
    
    companion object {
        fun fromLatLng(name: String, latLng: LatLng, radius: Float = 100f): GeofenceEntity =
            GeofenceEntity(
                name = name,
                latitude = latLng.latitude,
                longitude = latLng.longitude,
                radius = radius
            )
    }
} 