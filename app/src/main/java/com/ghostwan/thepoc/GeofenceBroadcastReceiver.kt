package com.ghostwan.thepoc

import android.app.NotificationChannel
import android.app.NotificationManager
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
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return

        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e(TAG, errorMessage)
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition
        val isEntering = geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER || 
            geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences
            val location = geofencingEvent.triggeringLocation

            triggeringGeofences?.forEach { geofence ->
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
                    database.timelineDao().insertEvent(event)
                }

                sendNotification(context, geofence.requestId, isEntering)
            }
        }
    }

    private fun sendNotification(context: Context, geofenceId: String, isEntering: Boolean) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Créer le canal de notification pour Android 8.0 et supérieur
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.geofence_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.geofence_channel_description)
            }
            notificationManager.createNotificationChannel(channel)
        }

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