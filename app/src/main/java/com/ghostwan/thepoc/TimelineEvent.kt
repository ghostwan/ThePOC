package com.ghostwan.thepoc

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "timeline_events")
data class TimelineEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Date,
    val realTimestamp: Date? = null,
    val zoneId: String,
    val zoneName: String,
    val eventType: EventType,
    val latitude: Double,
    val longitude: Double
)

enum class EventType {
    ENTER,
    EXIT
} 