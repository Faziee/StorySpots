package com.storyspots.core.managers

import android.graphics.BitmapFactory
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
import com.storyspots.R
import androidx.core.graphics.createBitmap

object MapStateManager {
    private const val TAG = "MapStateManager"

    private val _currentStories = MutableStateFlow<List<StoryData>>(emptyList())
    val currentStories: StateFlow<List<StoryData>> = _currentStories.asStateFlow()

    private val _pinsVisible = MutableStateFlow(false)
    val pinsVisible: StateFlow<Boolean> = _pinsVisible.asStateFlow()

    private var lastMapViewId: Int? = null

    fun updateStories(stories: List<StoryData>) {
        Log.d(TAG, "Updating stories: ${stories.size} stories")
        _currentStories.value = stories
    }

    fun hidePins() {
        Log.d(TAG, "Hiding pins")
        SimpleClustering.clearPins()
        _pinsVisible.value = false
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

    fun initializeMapForNewInstance(mapView: MapView) {
        Log.d(TAG, "Initializing map for new instance")

        try {
            val context = mapView.context
            val resourceId = context.resources.getIdentifier("pin_marker", "drawable", context.packageName)
            if (resourceId == 0) {
                Log.e(TAG, "pin_marker drawable not found! Using fallback.")
                val fallbackBitmap = createBitmap(32, 32)
                fallbackBitmap.eraseColor(android.graphics.Color.RED)
                initializeClusteringWithBitmap(mapView, fallbackBitmap)
                return
            }

            val bitmap = BitmapFactory.decodeResource(context.resources, resourceId)
            if (bitmap == null) {
                Log.e(TAG, "Failed to decode pin_marker bitmap")
                return
            }

            initializeClusteringWithBitmap(mapView, bitmap)

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing map for new instance", e)
        }
    }

    private fun initializeClusteringWithBitmap(mapView: MapView, bitmap: android.graphics.Bitmap) {
        val annotationApi = mapView.annotations
        val pointAnnotationManager = annotationApi.createPointAnnotationManager()

        SimpleClustering.setupClustering(mapView, pointAnnotationManager, bitmap)
        ClusterZoomHandler.setupClusterClickHandler(mapView, "clustering-pins")

        Log.d(TAG, "Map initialization completed for new instance")
    }

    fun getStoriesCount(): Int = _currentStories.value.size
}