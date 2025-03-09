package com.ghostwan.thepoc

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class TimelineViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    val events: Flow<List<TimelineEvent>> = database.timelineDao().getAllEvents()

    fun updateEvent(event: TimelineEvent) {
        viewModelScope.launch {
            database.timelineDao().updateEvent(event)
        }
    }

    fun deleteEvent(event: TimelineEvent) {
        viewModelScope.launch {
            database.timelineDao().deleteEvent(event)
        }
    }
} 