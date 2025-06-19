package com.storyspots.notification

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.Timestamp
import com.storyspots.caption.StoryData
import com.storyspots.receiver.GeofenceReceiver
import com.storyspots.utils.Constants
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class LocationNotificationManager(private val context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private val geofencingClient = LocationServices.getGeofencingClient(context)
    private val notificationSender = NotificationSender()

    private val oneSignalAppId = Constants.ONESIGNAL_APP_ID
    private val oneSignalRestApiKey = Constants.ONESIGNAL_REST_API_KEY

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Geofence settings
    private val geofenceRadius = 100f // 100 meters
    private val geofenceExpirationTime = 24 * 60 * 60 * 1000L // 24 hours

    fun startLocationTracking() {
        if (!hasLocationPermissions()) {
            Log.w("LocationNotificationManager", "Location permissions not granted")
            return
        }

        scope.launch {
            try {
                // Load nearby stories and set up geofences
                loadNearbyStoriesAndSetupGeofences()

                // Start continuous location updates for dynamic geofence management
                startLocationUpdates()

                Log.d("LocationNotificationManager", "Location tracking started")
            } catch (e: Exception) {
                Log.e("LocationNotificationManager", "Failed to start location tracking", e)
            }
        }
    }

    fun stopLocationTracking() {
        scope.launch {
            try {
                fusedLocationClient.removeLocationUpdates(locationCallback)
                removeAllGeofences()
                Log.d("LocationNotificationManager", "Location tracking stopped")
            } catch (e: Exception) {
                Log.e("LocationNotificationManager", "Failed to stop location tracking", e)
            }
        }
    }

    private suspend fun loadNearbyStoriesAndSetupGeofences() = withContext(Dispatchers.IO) {
        try {
            val currentLocation = getCurrentLocation() ?: return@withContext

            // Query stories within a larger radius (e.g., 5km) to set up geofences
            val stories = getNearbyStories(currentLocation, 5000.0) // 5km radius

            Log.d("LocationNotificationManager", "Found ${stories.size} nearby stories")

            // Set up geofences for these stories
            setupGeofencesForStories(stories)

        } catch (e: Exception) {
            Log.e("LocationNotificationManager", "Failed to load nearby stories", e)
        }
    }

    private suspend fun getCurrentLocation(): Location? = withContext(Dispatchers.IO) {
        try {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return@withContext null
            }

            fusedLocationClient.lastLocation.await()
        } catch (e: Exception) {
            Log.e("LocationNotificationManager", "Failed to get current location", e)
            null
        }
    }

    private suspend fun getNearbyStories(userLocation: Location, radiusInMeters: Double): List<StoryData> = withContext(Dispatchers.IO) {
        try {
            val stories = mutableListOf<StoryData>()

            // Get all stories from Firestore
            val querySnapshot = db.collection("story")
                .get()
                .await()

            for (document in querySnapshot.documents) {
                try {
                    val data = document.data
                    if (data != null) {
                        val geoPoint = data["location"] as? GeoPoint
                        if (geoPoint != null) {
                            val storyLocation = Location("").apply {
                                latitude = geoPoint.latitude
                                longitude = geoPoint.longitude
                            }

                            val distance = userLocation.distanceTo(storyLocation)
                            if (distance <= radiusInMeters) {
                                val story = StoryData(
                                    id = document.id,
                                    title = data["title"] as? String ?: "Untitled Story",
                                    createdAt = data["createdAt"] as? Timestamp,
                                    location = geoPoint,
                                    caption = data["caption"] as? String,
                                    imageUrl = data["imageUrl"] as? String,
                                    mapRef = data["mapRef"] as? DocumentReference,
                                    authorRef = data["authorRef"] as? DocumentReference,
                                    userPath = data["userPath"] as? String
                                )
                                stories.add(story)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w("LocationNotificationManager", "Failed to parse story document ${document.id}", e)
                }
            }

            stories
        } catch (e: Exception) {
            Log.e("LocationNotificationManager", "Failed to get nearby stories", e)
            emptyList()
        }
    }

    private fun setupGeofencesForStories(stories: List<StoryData>) {
        if (stories.isEmpty()) return

        val geofenceList = stories.mapNotNull { story ->
            story.location?.let { geoPoint ->
                Geofence.Builder()
                    .setRequestId("story_${story.id}")
                    .setCircularRegion(
                        geoPoint.latitude,
                        geoPoint.longitude,
                        geofenceRadius
                    )
                    .setExpirationDuration(geofenceExpirationTime)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                    .build()
            }
        }

        if (geofenceList.isEmpty()) return

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofenceList)
            .build()

        val geofencePendingIntent = createGeofencePendingIntent()

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
                .addOnSuccessListener {
                    Log.d("LocationNotificationManager", "Geofences added successfully for ${geofenceList.size} stories")
                }
                .addOnFailureListener { e ->
                    Log.e("LocationNotificationManager", "Failed to add geofences", e)
                }
        }
    }

    private fun createGeofencePendingIntent(): PendingIntent {
        // Create intent that points to your GeofenceReceiver
        val intent = Intent(context, GeofenceReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            30000L // 30 seconds
        ).apply {
            setMinUpdateDistanceMeters(50f) // Update every 50 meters
            setMaxUpdateDelayMillis(60000L) // Max 1 minute delay
        }.build()

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let { location ->
                Log.d("LocationNotificationManager", "Location updated: ${location.latitude}, ${location.longitude}")

                // Periodically refresh geofences based on new location
                scope.launch {
                    refreshGeofencesIfNeeded(location)
                }
            }
        }
    }

    private suspend fun refreshGeofencesIfNeeded(currentLocation: Location) {
        // Only refresh every 5 minutes to avoid excessive API calls
        val lastRefreshTime = getLastGeofenceRefreshTime()
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastRefreshTime > 5 * 60 * 1000) { // 5 minutes
            loadNearbyStoriesAndSetupGeofences()
            saveLastGeofenceRefreshTime(currentTime)
        }
    }

    suspend fun handleGeofenceEntry(storyId: String) {
        try {
            Log.d("LocationNotificationManager", "User entered geofence for story: $storyId")

            // Get story details
            val storyDoc = db.collection("story").document(storyId).get().await()
            val data = storyDoc.data

            if (data != null) {
                val story = StoryData(
                    id = storyDoc.id,
                    title = data["title"] as? String ?: "Untitled Story",
                    createdAt = data["createdAt"] as? Timestamp,
                    location = data["location"] as? GeoPoint,
                    caption = data["caption"] as? String,
                    imageUrl = data["imageUrl"] as? String,
                    mapRef = data["mapRef"] as? DocumentReference,
                    authorRef = data["authorRef"] as? DocumentReference,
                    userPath = data["userPath"] as? String
                )

                // Send location-based notification
                sendLocationNotification(story, storyId)
            }

        } catch (e: Exception) {
            Log.e("LocationNotificationManager", "Failed to handle geofence entry for story $storyId", e)
        }
    }

    private suspend fun sendLocationNotification(story: StoryData, storyId: String) {
        val currentUserId = auth.uid ?: return

        try {
            // Create a location-specific notification
            val title = "📍 Story Nearby!"
            val message = "You're near \"${story.title}\" - Tap to read!"

            // Use the existing NotificationSender but modify for location notifications
            sendLocationPushNotification(title, message, story.title ?: "Unknown Story", storyId, currentUserId)

        } catch (e: Exception) {
            Log.e("LocationNotificationManager", "Failed to send location notification", e)
        }
    }

    private suspend fun sendLocationPushNotification(
        notificationTitle: String,
        message: String,
        storyTitle: String,
        storyId: String,
        userId: String
    ) = withContext(Dispatchers.IO) {
        try {
            // Use OneSignal to send push notification to specific user
            val jsonData = JSONObject().apply {
                put("app_id", oneSignalAppId)

                // Target specific user
                val filters = JSONArray().apply {
                    put(JSONObject().apply {
                        put("field", "tag")
                        put("key", "user_id")
                        put("relation", "=")
                        put("value", userId)
                    })
                }
                put("filters", filters)

                put("headings", JSONObject().put("en", notificationTitle))
                put("contents", JSONObject().put("en", message))

                // Add custom data for handling notification clicks
                put("data", JSONObject().apply {
                    put("type", "location_story")
                    put("story_id", storyId)
                    put("story_title", storyTitle)
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonData.toString().toRequestBody(mediaType)

            val client = OkHttpClient()
            val request = Request.Builder()
                .url("https://onesignal.com/api/v1/notifications")
                .header("Authorization", "Basic $oneSignalRestApiKey")
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful) {
                Log.d("LocationNotificationManager", "✅ Location notification sent successfully!")
                Log.d("LocationNotificationManager", "OneSignal Response: $responseBody")

                // Parse the response to get notification ID
                try {
                    val responseJson = JSONObject(responseBody ?: "")
                    val notificationId = responseJson.optString("id", "unknown")
                    Log.d("LocationNotificationManager", "📱 OneSignal Notification ID: $notificationId")
                } catch (e: Exception) {
                    Log.w("LocationNotificationManager", "Could not parse notification ID", e)
                }
            } else {
                Log.e("LocationNotificationManager", "❌ Location notification failed!")
                Log.e("LocationNotificationManager", "Response Code: ${response.code}")
                Log.e("LocationNotificationManager", "Response Body: $responseBody")
            }

        } catch (e: Exception) {
            Log.e("LocationNotificationManager", "Failed to send location push notification", e)
        }
    }

    private fun removeAllGeofences() {
        geofencingClient.removeGeofences(createGeofencePendingIntent())
            .addOnSuccessListener {
                Log.d("LocationNotificationManager", "All geofences removed")
            }
            .addOnFailureListener { e ->
                Log.e("LocationNotificationManager", "Failed to remove geofences", e)
            }
    }

    private fun hasLocationPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun getLastGeofenceRefreshTime(): Long {
        return context.getSharedPreferences("LocationNotifications", Context.MODE_PRIVATE)
            .getLong("last_geofence_refresh", 0L)
    }

    private fun saveLastGeofenceRefreshTime(time: Long) {
        context.getSharedPreferences("LocationNotifications", Context.MODE_PRIVATE)
            .edit()
            .putLong("last_geofence_refresh", time)
            .apply()
    }

    fun cleanup() {
        scope.cancel()
        stopLocationTracking()
    }

    // Add this to LocationNotificationManager
    fun testDirectNotification() {
        scope.launch {
            Log.d("LocationNotificationManager", "🧪 Testing direct notification...")

            val currentUserId = auth.uid ?: return@launch

            sendLocationPushNotification(
                "📍 Test Location Notification!",
                "Direct test - bypassing geofence",
                "Test Story",
                "test_123",
                currentUserId
            )
        }
    }
}