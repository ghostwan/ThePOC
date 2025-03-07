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

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return

        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e(TAG, errorMessage)
            return
        }

        // Obtenir le type de transition
        val geofenceTransition = geofencingEvent.geofenceTransition

        // Vérifier si la transition est une entrée dans la zone
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences

            triggeringGeofences?.forEach { geofence ->
                sendNotification(context, geofence.requestId)
            }
        }
    }

    private fun sendNotification(context: Context, geofenceId: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Créer le canal de notification pour Android 8.0 et supérieur
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Geofencing",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications de géofencing"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Construire la notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setContentTitle("Zone détectée")
            .setContentText("Vous êtes entré dans la zone : $geofenceId")
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