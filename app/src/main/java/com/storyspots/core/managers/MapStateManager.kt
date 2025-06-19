package com.storyspots.core.managers

import android.util.Log
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.storyspots.caption.StoryData
import com.storyspots.core.AppComponents
import com.storyspots.pin.ClusterZoomHandler
import com.storyspots.pin.SimpleClustering
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager

object MapStateManager {
    private const val TAG = "MapStateManager"

    private val _currentStories = MutableStateFlow<List<StoryData>>(emptyList())
    val currentStories: StateFlow<List<StoryData>> = _currentStories.asStateFlow()

    fun updateStories(stories: List<StoryData>) {
        Log.d(TAG, "Updating stories: ${stories.size} stories")
        _currentStories.value = stories
    }

    fun showPinsOnMap(mapInstanceId: Int) {
        Log.d(TAG, "Showing pins on map for instance $mapInstanceId")

        if (!SimpleClustering.isClusteringInitialized()) {
            Log.e(TAG, "Clustering not initialized, cannot show pins!")
            return
        }

        val stories = currentStories.value
        if (stories.isEmpty()) {
            Log.d(TAG, "No stories to display")
            return
        }

        Log.d(TAG, "About to clear existing pins and add ${stories.size} new pins")

        SimpleClustering.clearPins()
        var addedPins = 0

        stories.forEach { story ->
            story.location?.let { geoPoint ->
                val point = Point.fromLngLat(geoPoint.longitude, geoPoint.latitude)
                SimpleClustering.addClusterPin(point)
                addedPins++
            }
        }

        Log.d(TAG, "Successfully added $addedPins pins out of ${stories.size} stories")
    }

    fun refreshPins(mapInstanceId: Int) {
        Log.d(TAG, "Refreshing pins for map instance $mapInstanceId")

        AppComponents.appScope.launch {
            kotlinx.coroutines.delay(100)
            withContext(Dispatchers.Main) {
                showPinsOnMap(mapInstanceId)
            }
        }
    }

    private fun initializeClusteringWithBitmap(mapView: MapView, bitmap: android.graphics.Bitmap) {
        val annotationApi = mapView.annotations
        val pointAnnotationManager = annotationApi.createPointAnnotationManager()

        SimpleClustering.setupClustering(mapView, pointAnnotationManager, bitmap)
        ClusterZoomHandler.setupClusterClickHandler(mapView)

        ClusterZoomHandler.setupClusterClickHandler(mapView) { point, pointCount ->
            Log.d(TAG, "Small cluster clicked at $point with $pointCount stories")

            val storiesAtLocation = currentStories.value.filter { story ->
                story.location?.let { geoPoint ->
                    val distance = calculateDistance(point, geoPoint)
                    distance < 0.001
                } == true
            }

            Log.d(TAG, "Cluster clicked: found ${storiesAtLocation.size} stories at location")
        }

        Log.d(TAG, "Map initialization completed for new instance")
    }

    fun getStoriesCount(): Int = _currentStories.value.size

    private fun calculateDistance(point: Point, geoPoint: com.google.firebase.firestore.GeoPoint): Double {
        val latDiff = point.latitude() - geoPoint.latitude
        val lngDiff = point.longitude() - geoPoint.longitude
        return kotlin.math.sqrt(latDiff * latDiff + lngDiff * lngDiff)
    }
}