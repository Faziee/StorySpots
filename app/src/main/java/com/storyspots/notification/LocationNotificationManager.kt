package com.storyspots.notificationAdd

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.storyspots.caption.StoryData
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class SimpleLocationStoryDetector(private val context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Simple proximity settings
    private val proximityRadius = 100.0 // 100 meters
    private val checkInterval = 15000L // Check every 15 seconds

    // Track which stories we've already notified about
    private val notifiedStories = mutableSetOf<String>()
    private var isTracking = false
    private var nearbyStories = listOf<StoryData>()

    fun startSimpleLocationTracking() {
        if (!hasLocationPermissions()) {
            Log.w("SimpleLocationDetector", "❌ Location permissions not granted")
            showToast("Location permissions needed for story detection")
            return
        }

        if (isTracking) {
            Log.d("SimpleLocationDetector", "Already tracking location")
            return
        }

        isTracking = true
        Log.d("SimpleLocationDetector", "🎯 Starting simple location tracking...")

        // Start location updates
        startLocationUpdates()

        // Load stories once at startup
        scope.launch {
            loadAllStories()
        }
    }

    fun stopLocationTracking() {
        if (!isTracking) return

        isTracking = false
        fusedLocationClient.removeLocationUpdates(locationCallback)
        notifiedStories.clear()
        Log.d("SimpleLocationDetector", "🛑 Location tracking stopped")
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            checkInterval
        ).apply {
            setMinUpdateIntervalMillis(checkInterval)
            setMinUpdateDistanceMeters(10f) // Update every 10 meters
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
            Log.d("SimpleLocationDetector", "📍 Location updates started")
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val location = locationResult.lastLocation
            if (location != null) {
                Log.d("SimpleLocationDetector",
                    "📍 Location: ${location.latitude}, ${location.longitude} (Accuracy: ${location.accuracy}m)")

                // Check proximity to stories
                scope.launch {
                    checkProximityToStories(location)
                }
            }
        }

        override fun onLocationAvailability(availability: LocationAvailability) {
            Log.d("SimpleLocationDetector", "Location availability: ${availability.isLocationAvailable}")
        }
    }

    private suspend fun loadAllStories() = withContext(Dispatchers.IO) {
        try {
            Log.d("SimpleLocationDetector", "📚 Loading all stories...")

            val stories = mutableListOf<StoryData>()
            val querySnapshot = db.collection("story").get().await()

            for (document in querySnapshot.documents) {
                try {
                    val data = document.data
                    if (data != null) {
                        val geoPoint = data["location"] as? GeoPoint
                        if (geoPoint != null) {
                            val story = StoryData(
                                id = document.id,
                                title = data["title"] as? String ?: "Untitled Story",
                                location = geoPoint,
                                caption = data["caption"] as? String,
                                imageUrl = data["imageUrl"] as? String,
                                userPath = data["userPath"] as? String,
                                createdAt = null,
                                mapRef = null,
                                authorRef = null
                            )
                            stories.add(story)
                            Log.d("SimpleLocationDetector",
                                "📖 Loaded story: ${story.title} at ${geoPoint.latitude}, ${geoPoint.longitude}")
                        }
                    }
                } catch (e: Exception) {
                    Log.w("SimpleLocationDetector", "Failed to parse story ${document.id}", e)
                }
            }

            nearbyStories = stories
            Log.d("SimpleLocationDetector", "✅ Loaded ${stories.size} stories total")

        } catch (e: Exception) {
            Log.e("SimpleLocationDetector", "❌ Failed to load stories", e)
            withContext(Dispatchers.Main) {
                showToast("Failed to load stories: ${e.message}")
            }
        }
    }

    private suspend fun checkProximityToStories(userLocation: Location) = withContext(Dispatchers.IO) {
        try {
            val nearStories = mutableListOf<Pair<StoryData, Double>>()

            for (story in nearbyStories) {
                val geoPoint = story.location
                if (geoPoint != null) {
                    val storyLocation = Location("").apply {
                        latitude = geoPoint.latitude
                        longitude = geoPoint.longitude
                    }

                    val distance = userLocation.distanceTo(storyLocation)

                    if (distance <= proximityRadius) {
                        nearStories.add(Pair(story, distance.toDouble()))

                        // Only notify once per story
                        if (!notifiedStories.contains(story.id)) {
                            notifiedStories.add(story.id)

                            Log.d("SimpleLocationDetector",
                                "🎉 NEAR STORY: '${story.title}' - Distance: ${distance.toInt()}m")

                            withContext(Dispatchers.Main) {
                                showStoryProximityMessage(story, distance.toInt())
                            }
                        }
                    }
                }
            }

            // Log current proximity status
            if (nearStories.isNotEmpty()) {
                Log.d("SimpleLocationDetector",
                    "📍 Currently near ${nearStories.size} stories")
            }

            // Reset notifications if user moves away from all stories
            if (nearStories.isEmpty() && notifiedStories.isNotEmpty()) {
                notifiedStories.clear()
                Log.d("SimpleLocationDetector", "🔄 Reset story notifications - moved away from all stories")
            } else {
                // Do nothing - either we have near stories or no previous notifications
            }

        } catch (e: Exception) {
            Log.e("SimpleLocationDetector", "❌ Error checking story proximity", e)
        }
    }

    private fun showStoryProximityMessage(story: StoryData, distanceMeters: Int) {
        val message = "📍 You're ${distanceMeters}m from \"${story.title}\"!"

        // Show toast message
        showToast(message)

        // Also log it prominently
        Log.i("SimpleLocationDetector", "🎯 STORY PROXIMITY: $message")

        // You can add more UI notifications here:
        // - Show a snackbar
        // - Trigger a custom notification
        // - Update UI elements
        // - Play a sound
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private fun hasLocationPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Call this when user force-refresh
    fun forceRefreshStories() {
        scope.launch {
            Log.d("SimpleLocationDetector", "🔄 Force refreshing stories...")
            loadAllStories()
        }
    }

    // Debug method to show current status
    fun getDebugInfo(): String {
        return """
            Tracking: $isTracking
            Stories loaded: ${nearbyStories.size}
            Notified stories: ${notifiedStories.size}
            Proximity radius: ${proximityRadius}m
        """.trimIndent()
    }

    fun cleanup() {
        stopLocationTracking()
        scope.cancel()
    }
}

// Extension function for easy usage in Activity/Fragment
fun Context.createSimpleLocationDetector(): SimpleLocationStoryDetector {
    return SimpleLocationStoryDetector(this)
}

// Example usage in Activity:
/*
class MainActivity : AppCompatActivity() {
    private lateinit var locationDetector: SimpleLocationStoryDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        locationDetector = createSimpleLocationDetector()

        // Start after getting permissions
        if (hasLocationPermissions()) {
            locationDetector.startSimpleLocationTracking()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        locationDetector.cleanup()
    }

    private fun hasLocationPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}
*/