package com.storyspots.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.storyspots.notification.LocationNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GeofenceReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent == null) {
            Log.e("GeofenceReceiver", "GeofencingEvent is null")
            return
        }

        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e("GeofenceReceiver", "Geofence error: $errorMessage")
            return
        }

        // Get the transition type
        val geofenceTransition = geofencingEvent.geofenceTransition

        // Check if it's an entry transition
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            // Get the geofences that were triggered
            val triggeringGeofences = geofencingEvent.triggeringGeofences

            if (triggeringGeofences != null) {
                for (geofence in triggeringGeofences) {
                    val requestId = geofence.requestId

                    // Extract story ID from request ID (format: "story_<id>")
                    if (requestId.startsWith("story_")) {
                        val storyId = requestId.substring(6) // Remove "story_" prefix

                        Log.d("GeofenceReceiver", "User entered geofence for story: $storyId")

                        // Handle the geofence entry
                        scope.launch {
                            val locationManager = LocationNotificationManager(context)
                            locationManager.handleGeofenceEntry(storyId)
                        }
                    }
                }
            }
        } else {
            Log.d("GeofenceReceiver", "Geofence transition: $geofenceTransition")
        }
    }
}