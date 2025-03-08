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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(database: AppDatabase) {
    val scope = rememberCoroutineScope()
    var events by remember { mutableStateOf(listOf<TimelineEvent>()) }

    LaunchedEffect(Unit) {
        database.timelineDao().getAllEvents().collect { newEvents ->
            events = newEvents.sortedByDescending { it.timestamp }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        CenterAlignedTopAppBar(
            title = { Text(stringResource(R.string.tab_timeline)) }
        )

        if (events.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.timeline_empty),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn {
                items(
                    items = events,
                    key = { it.id }
                ) { event ->
                    var show by remember { mutableStateOf(true) }
                    
                    AnimatedVisibility(
                        visible = show,
                        exit = shrinkHorizontally(
                            animationSpec = tween(durationMillis = 300)
                        ) + fadeOut()
                    ) {
                        SwipeableItem(
                            event = event,
                            onDismiss = {
                                show = false
                                scope.launch {
                                    database.timelineDao().deleteEvent(event)
                                    events = events.filter { it.id != event.id }
                                }
                            }
                        )
                    }

                    if (events.last() != event) {
                        Divider()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableItem(
    event: TimelineEvent,
    onDismiss: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { 200f },
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                onDismiss()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                Color.Red
            } else {
                Color.Transparent
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.clear_timeline),
                    tint = Color.White
                )
            }
        },
        content = {
            TimelineEventItem(event)
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineEventItem(event: TimelineEvent) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    
    ListItem(
        headlineContent = { 
            Text(
                text = event.zoneName,
                style = MaterialTheme.typography.titleMedium
            )
        },
        supportingContent = {
            Text(
                text = dateFormat.format(event.timestamp),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        trailingContent = {
            Text(
                text = if (event.eventType == EventType.ENTER) 
                    stringResource(R.string.event_enter)
                else 
                    stringResource(R.string.event_exit),
                style = MaterialTheme.typography.bodyMedium,
                color = if (event.eventType == EventType.ENTER)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error
            )
        }
    )
} 