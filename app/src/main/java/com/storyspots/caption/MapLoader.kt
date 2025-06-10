// OPTION 1: Add the function inside MapLoader (Recommended)
// Update your MapLoader.kt:

package com.storyspots.caption

import android.util.Log
import androidx.compose.ui.geometry.Offset
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraState
import com.mapbox.maps.MapView
import com.storyspots.pin.SimpleClustering
import java.lang.Math.toRadians
import kotlin.math.*

class MapLoader {
    companion object {
        private const val TAG = "MapLoader"
        private var instance: MapLoader? = null
        private var mapView: MapView? = null
        private var allStories = mutableListOf<StoryData>()
        private var onPinClickListener: ((List<StoryData>, Offset) -> Unit)? = null
        private var storyListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

        fun getInstance(): MapLoader {
            if (instance == null) {
                instance = MapLoader()
            }
            return instance!!
        }

        fun initialize(mapView: MapView) {
            this.mapView = mapView
            getInstance().setupCameraListener()
        }

        // Updated to use real-time listener instead of one-time fetch
        fun loadAllStories(onResult: (List<StoryData>) -> Unit) {
            // Remove existing listener if any
            storyListenerRegistration?.remove()

            // Use real-time listener to automatically get new posts
            storyListenerRegistration = fetchAllStories { stories ->
                Log.d(TAG, "Stories updated: ${stories.size} total stories")
                allStories.clear()
                allStories.addAll(stories)
                updateVisiblePins()
                onResult(stories)
            }
        }

        // Add method to manually refresh (call this after posting)
        fun refreshStories() {
            Log.d(TAG, "Manually refreshing stories...")
            updateVisiblePins()
        }

        fun setOnPinClickListener(listener: (List<StoryData>, Offset) -> Unit) {
            onPinClickListener = listener

            SimpleClustering.setOnPinClickListener { clickedPoint ->
                val storiesAtLocation = findStoriesAtLocation(clickedPoint)
                Log.d(TAG, "Pin clicked: found ${storiesAtLocation.size} stories at location")

                mapView?.let { mv ->
                    val offset = convertPointToOffsetWithPadding(clickedPoint, mv, 80f)
                    onPinClickListener?.invoke(storiesAtLocation, offset)
                }
            }
        }

        private fun convertPointToOffsetWithPadding(point: Point, mapView: MapView, yPadding: Float = 100f): Offset {
            val screenCoordinate = mapView.mapboxMap.pixelForCoordinate(point)
            return Offset(
                x = screenCoordinate.x.toFloat(),
                y = screenCoordinate.y.toFloat() - yPadding
            )
        }

        private fun updateVisiblePins() {
            mapView?.let { map ->
                val visibleStories = getStoriesInScreenBounds(map.mapboxMap.cameraState)
                Log.d(TAG, "Updating visible pins: ${visibleStories.size} stories in bounds")

                // Clear existing pins and add visible ones
                SimpleClustering.clearPins()
                visibleStories.forEach { story ->
                    story.location?.let { geoPoint ->
                        val point = Point.fromLngLat(geoPoint.longitude, geoPoint.latitude)
                        SimpleClustering.addClusterPin(point)
                    }
                }
            }
        }

        private fun findStoriesAtLocation(clickedPoint: Point, tolerance: Double = 0.0001): List<StoryData> {
            return allStories.filter { story ->
                story.location?.let { geoPoint ->
                    val storyPoint = Point.fromLngLat(geoPoint.longitude, geoPoint.latitude)
                    val distance = calculateDistance(clickedPoint, storyPoint)
                    distance < tolerance
                } ?: false
            }
        }

        private fun calculateDistance(point1: Point, point2: Point): Double {
            val latDiff = point1.latitude() - point2.latitude()
            val lngDiff = point1.longitude() - point2.longitude()
            return kotlin.math.sqrt(latDiff * latDiff + lngDiff * lngDiff)
        }

        private fun getStoriesInScreenBounds(cameraState: CameraState): List<StoryData> {
            val center = cameraState.center
            val zoom = cameraState.zoom

            val latitudeDelta = when {
                zoom >= 15 -> 0.01
                zoom >= 12 -> 0.05
                zoom >= 10 -> 0.1
                zoom >= 8 -> 0.5
                else -> 1.0
            }

            val longitudeDelta = latitudeDelta / cos(center.latitude() * PI / 180)

            val bounds = ScreenBounds(
                north = center.latitude() + latitudeDelta,
                south = center.latitude() - latitudeDelta,
                east = center.longitude() + longitudeDelta,
                west = center.longitude() - longitudeDelta
            )

            return allStories.filter { story ->
                story.location?.let { geoPoint ->
                    val lat = geoPoint.latitude
                    val lng = geoPoint.longitude

                    lat <= bounds.north && lat >= bounds.south &&
                            lng <= bounds.east && lng >= bounds.west
                } ?: false
            }
        }

        // Cleanup method
        fun cleanup() {
            storyListenerRegistration?.remove()
            storyListenerRegistration = null
        }
    }

    private fun setupCameraListener() {
        mapView?.mapboxMap?.addOnCameraChangeListener {
            updateVisiblePins()
        }
    }

    data class ScreenBounds(
        val north: Double,
        val south: Double,
        val east: Double,
        val west: Double
    )
}