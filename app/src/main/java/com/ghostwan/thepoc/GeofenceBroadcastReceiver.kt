package com.ghostwan.thepoc

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import java.util.*

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Geofence event received")
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: run {
            Log.e(TAG, "Error: GeofencingEvent.fromIntent returned null")
            return
        }

        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e(TAG, "Error in geofencing event: $errorMessage")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition
        val isEntering = geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER
        
        Log.d(TAG, "Geofence transition: ${if (isEntering) "ENTER" else "EXIT"}")

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER || 
            geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences
            val location = geofencingEvent.triggeringLocation

            Log.d(TAG, "Number of triggering geofences: ${triggeringGeofences?.size ?: 0}")
            Log.d(TAG, "Triggering location: lat=${location?.latitude}, lng=${location?.longitude}")

            triggeringGeofences?.forEach { geofence ->
                Log.d(TAG, "Processing geofence: ${geofence.requestId}")
                
                // Enregistrer l'événement dans la timeline
                val event = TimelineEvent(
                    timestamp = Date(),
                    zoneId = geofence.requestId,
                    zoneName = geofence.requestId,
                    eventType = if (isEntering) EventType.ENTER else EventType.EXIT,
                    latitude = location?.latitude ?: 0.0,
                    longitude = location?.longitude ?: 0.0
                )

                // Sauvegarder l'événement dans la base de données
                val database = AppDatabase.getDatabase(context)
                kotlinx.coroutines.runBlocking {
                    try {
                        database.timelineDao().insertEvent(event)
                        Log.d(TAG, "Timeline event saved successfully")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error saving timeline event", e)
                    }
                }

                sendNotification(context, geofence.requestId, isEntering)
                Log.d(TAG, "Notification sent for geofence: ${geofence.requestId}")
            }
        } else {
            Log.d(TAG, "Unhandled geofence transition type: $geofenceTransition")
        }
    }

    private fun sendNotification(context: Context, geofenceId: String, isEntering: Boolean) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Créer l'intent pour ouvrir l'application sur la timeline
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("openTimeline", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Créer le canal de notification pour Android 8.0 et supérieur
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.geofence_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.geofence_channel_description)
        }
        notificationManager.createNotificationChannel(channel)

        // Construire la notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setContentTitle(context.getString(R.string.geofence_notification_title))
            .setContentText(
                if (isEntering) 
                    context.getString(R.string.geofence_enter_notification, geofenceId)
                else 
                    context.getString(R.string.geofence_exit_notification, geofenceId)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // Afficher la notification
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val TAG = "GeofenceReceiver"
        private const val CHANNEL_ID = "GeofenceChannel"
        private const val NOTIFICATION_ID = 1
    }
} 