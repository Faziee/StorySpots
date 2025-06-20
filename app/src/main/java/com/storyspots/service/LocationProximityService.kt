//package com.storyspots.service
//
//import android.Manifest
//import android.annotation.SuppressLint
//import android.app.Service
//import android.content.Context
//import android.content.Intent
//import android.content.pm.PackageManager
//import android.location.Location
//import android.os.Build
//import android.os.IBinder
//import android.os.Looper
//import android.util.Log
//import androidx.core.app.ActivityCompat
//import com.google.android.gms.location.*
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.firestore.DocumentReference
//import com.google.firebase.firestore.FirebaseFirestore
//import com.google.firebase.firestore.GeoPoint
//import com.storyspots.notification.NotificationSender
//import kotlinx.coroutines.*
//import kotlinx.coroutines.tasks.await
//import kotlin.math.*
//
//class LocationProximityService : Service() {
//
//    private lateinit var fusedLocationClient: FusedLocationProviderClient
//    private lateinit var locationCallback: LocationCallback
//    private val db = FirebaseFirestore.getInstance()
//    private val auth = FirebaseAuth.getInstance()
//    private lateinit var notificationSender: NotificationSender
//
//    // Track which stories user has been notified about to prevent spam
//    private val notifiedStories = mutableSetOf<String>()
//
//    companion object {
//        private const val PROXIMITY_RADIUS_METERS = 100.0 // 100 meters
//        private const val LOCATION_UPDATE_INTERVAL = 30000L // 30 seconds
//        private const val FASTEST_UPDATE_INTERVAL = 15000L // 15 seconds
//        private const val TAG = "LocationProximityService"
//        private const val CHANNEL_ID = "story_proximity_service_channel"
//
//        fun startService(context: Context) {
//            val intent = Intent(context, LocationProximityService::class.java)
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                context.startForegroundService(intent)
//            } else {
//                context.startService(intent)
//            }
//        }
//
//        fun stopService(context: Context) {
//            val intent = Intent(context, LocationProximityService::class.java)
//            context.stopService(intent)
//        }
//    }
//
//    override fun onCreate() {
//        super.onCreate()
//        notificationSender = NotificationSender(this)
//        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
//        setupLocationCallback()
//        Log.d(TAG, "LocationProximityService created")
//    }
//
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        Log.d(TAG, "Starting location proximity monitoring")
//
//        // Create foreground notification for API 26+
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            startForeground(1, createForegroundNotification())
//        }
//
//        startLocationUpdates()
//        return START_STICKY // Restart if killed
//    }
//
//    private fun createForegroundNotification(): android.app.Notification {
//        // Create notification channel first
//        createNotificationChannel()
//
//        // Create a simple notification to keep service running
//        return androidx.core.app.NotificationCompat.Builder(this, CHANNEL_ID)
//            .setContentTitle("Story Spots")
//            .setContentText("Looking for nearby stories...")
//            .setSmallIcon(android.R.drawable.ic_menu_mylocation) // Using built-in icon
//            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
//            .build()
//    }
//
//    private fun createNotificationChannel() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val channel = android.app.NotificationChannel(
//                CHANNEL_ID,
//                "Story Proximity Service",
//                android.app.NotificationManager.IMPORTANCE_LOW
//            ).apply {
//                description = "Keeps the app running to detect nearby stories"
//                setShowBadge(false)
//            }
//
//            val notificationManager = getSystemService(android.app.NotificationManager::class.java)
//            notificationManager.createNotificationChannel(channel)
//        }
//    }
//
//    override fun onBind(intent: Intent?): IBinder? = null
//
//    private fun setupLocationCallback() {
//        locationCallback = object : LocationCallback() {
//            override fun onLocationResult(locationResult: LocationResult) {
//                super.onLocationResult(locationResult)
//                locationResult.lastLocation?.let { location ->
//                    Log.d(TAG, "Location update: ${location.latitude}, ${location.longitude}")
//                    checkForNearbyStories(location)
//                }
//            }
//        }
//    }
//
//    @SuppressLint("MissingPermission")
//    private fun startLocationUpdates() {
//        if (!hasLocationPermission()) {
//            Log.e(TAG, "Location permission not granted")
//            return
//        }
//
//        val locationRequest = LocationRequest.Builder(
//            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
//            LOCATION_UPDATE_INTERVAL
//        ).apply {
//            setMinUpdateIntervalMillis(FASTEST_UPDATE_INTERVAL)
//            setMinUpdateDistanceMeters(50f) // Only update if moved 50+ meters
//        }.build()
//
//        fusedLocationClient.requestLocationUpdates(
//            locationRequest,
//            locationCallback,
//            Looper.getMainLooper()
//        )
//
//        Log.d(TAG, "Location updates started")
//    }
//
//    private fun checkForNearbyStories(userLocation: Location) {
//        val currentUserId = auth.uid ?: return
//
//        CoroutineScope(Dispatchers.IO).launch {
//            try {
//                Log.d(TAG, "Checking for nearby stories...")
//
//                // Query Firestore for stories
//                val stories = db.collection("story")
//                    .get()
//                    .await()
//
//                for (document in stories.documents) {
//                    val storyLocation = document.getGeoPoint("location") ?: continue
//                    val storyId = document.id
//
//                    // Get author ID from the user field (could be DocumentReference or String)
//                    val userField = document.get("user")
//                    val authorId = when (userField) {
//                        is DocumentReference -> userField.id
//                        is String -> {
//                            if (userField.startsWith("/user/")) {
//                                userField.removePrefix("/user/")
//                            } else {
//                                userField
//                            }
//                        }
//                        else -> null
//                    }
//
//                    // Skip stories by the current user
//                    if (authorId == currentUserId) continue
//
//                    // Skip if already notified about this story
//                    if (notifiedStories.contains(storyId)) continue
//
//                    val distance = calculateDistance(
//                        userLocation.latitude,
//                        userLocation.longitude,
//                        storyLocation.latitude,
//                        storyLocation.longitude
//                    )
//
//                    if (distance <= PROXIMITY_RADIUS_METERS) {
//                        Log.d(TAG, "Found nearby story: $storyId, distance: ${distance.toInt()}m")
//
//                        // Get story details
//                        val storyTitle = document.getString("title") ?: "Untitled Story"
//
//                        // Get author name from the user document
//                        val authorName = if (authorId != null) {
//                            try {
//                                val userDoc = db.collection("users").document(authorId).get().await()
//                                userDoc.getString("name") ?: userDoc.getString("username") ?: "Unknown Author"
//                            } catch (e: Exception) {
//                                Log.e(TAG, "Failed to get author name for user $authorId", e)
//                                "Unknown Author"
//                            }
//                        } else {
//                            "Unknown Author"
//                        }
//
//                        // Get author profile image
//                        val authorProfileImage = if (authorId != null) {
//                            try {
//                                val userDoc = db.collection("users").document(authorId).get().await()
//                                userDoc.getString("profileImageUrl") ?: userDoc.getString("profile_image_url")
//                            } catch (e: Exception) {
//                                Log.e(TAG, "Failed to get author profile image for user $authorId", e)
//                                null
//                            }
//                        } else {
//                            null
//                        }
//
//                        notificationSender.sendProximityNotification(
//                            postTitle = storyTitle,
//                            postId = storyId,
//                            authorName = authorName,
//                            distance = distance.toInt(),
//                            authorProfileImageUrl = authorProfileImage
//                        )
//
//                        // Mark as notified to prevent spam
//                        notifiedStories.add(storyId)
//
//                        // Optional: Remove from set after some time to allow re-notification
//                        CoroutineScope(Dispatchers.IO).launch {
//                            delay(24 * 60 * 60 * 1000) // 24 hours
//                            notifiedStories.remove(storyId)
//                        }
//                    }
//                }
//
//            } catch (e: Exception) {
//                Log.e(TAG, "Error checking nearby stories", e)
//            }
//        }
//    }
//
//    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
//        val earthRadius = 6371000.0 // Earth's radius in meters
//
//        val dLat = Math.toRadians(lat2 - lat1)
//        val dLon = Math.toRadians(lon2 - lon1)
//
//        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
//        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
//
//        return earthRadius * c
//    }
//
//    private fun hasLocationPermission(): Boolean {
//        return ActivityCompat.checkSelfPermission(
//            this,
//            Manifest.permission.ACCESS_FINE_LOCATION
//        ) == PackageManager.PERMISSION_GRANTED
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        fusedLocationClient.removeLocationUpdates(locationCallback)
//        Log.d(TAG, "LocationProximityService destroyed")
//    }
//}