package com.ghostwan.thepoc

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.Divider
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    viewModel: TimelineViewModel = viewModel()
) {
    val events by viewModel.events.collectAsState(initial = emptyList())

    LazyColumn {
        items(events) { event ->
            SwipeableTimelineItem(
                event = event,
                onEventUpdated = { updatedEvent ->
                    viewModel.updateEvent(updatedEvent)
                },
                onEventDeleted = {
                    viewModel.deleteEvent(event)
                }
            )
            if (events.last() != event) {
                HorizontalDivider()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableTimelineItem(
    event: TimelineEvent,
    onEventUpdated: (TimelineEvent) -> Unit,
    onEventDeleted: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<Long?>(null) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { 200f },
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.EndToStart -> {
                    onEventDeleted()
                    true
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    showDatePicker = true
                    false
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val alignment = when {
                dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                else -> Alignment.Center
            }
            val icon = when {
                dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Edit
                else -> null
            }
            val color = when {
                dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart -> Color.Red
                dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd -> Color.Green
                else -> Color.Transparent
            }
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color.copy(alpha = 0.1f))
                        .padding(horizontal = 16.dp),
                    contentAlignment = alignment
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color
                    )
                }
            }
        }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = when (event.eventType) {
                    EventType.ENTER -> Color(0xFF4CAF50).copy(alpha = 0.05f)
                    EventType.EXIT -> Color(0xFFF44336).copy(alpha = 0.05f)
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = event.zoneName,
                    style = MaterialTheme.typography.titleMedium,
                    color = when (event.eventType) {
                        EventType.ENTER -> Color(0xFF4CAF50)
                        EventType.EXIT -> Color(0xFFF44336)
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when (event.eventType) {
                        EventType.ENTER -> stringResource(R.string.event_enter)
                        EventType.EXIT -> stringResource(R.string.event_exit)
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeFormat.format(event.timestamp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (event.realTimestamp != null) {
                        Text(
                            text = " (${timeFormat.format(event.realTimestamp)})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    text = dateFormat.format(event.timestamp).substringBefore(" "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = event.timestamp.time
        )

        AlertDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        selectedDate = datePickerState.selectedDateMillis
                        showDatePicker = false
                        showTimePicker = true
                    }
                ) {
                    Text(stringResource(R.string.next))
                }
            },
            dismissButton = {
                Button(onClick = { showDatePicker = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
            text = {
                DatePicker(
                    state = datePickerState,
                    showModeToggle = false
                )
            }
        )
    }

    if (showTimePicker) {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = event.timestamp.time
        }
        val timePickerState = rememberTimePickerState(
            initialHour = calendar.get(Calendar.HOUR_OF_DAY),
            initialMinute = calendar.get(Calendar.MINUTE),
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        val newCalendar = Calendar.getInstance().apply {
                            timeInMillis = selectedDate ?: event.timestamp.time
                            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            set(Calendar.MINUTE, timePickerState.minute)
                        }
                        onEventUpdated(event.copy(realTimestamp = Date(newCalendar.timeInMillis)))
                        showTimePicker = false
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                Button(onClick = { showTimePicker = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }
} 