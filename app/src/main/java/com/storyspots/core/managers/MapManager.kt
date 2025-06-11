package com.storyspots.core.managers

import android.graphics.BitmapFactory
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.gestures
import com.storyspots.R
import com.storyspots.core.StorySpot
import com.storyspots.caption.MapLoader
import com.storyspots.core.AppComponents
import com.storyspots.pin.ClusterZoomHandler
import com.storyspots.pin.SimpleClustering
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MapManager {
    private var mapView: MapView? = null
    private var pointAnnotationManager: PointAnnotationManager? = null

    suspend fun initializeMap(mapView: MapView) = withContext(Dispatchers.Main) {
        this@MapManager.mapView = mapView

        setupMapGestures()
        setupAnnotations()

        // Initialize MapLoader
        MapLoader.initialize(mapView)

        // Set up pin click listener
        MapLoader.setOnPinClickListener { storiesAtPin, offset ->
            // Handle pin clicks
        }
    }

    private fun setupMapGestures() {
        mapView?.gestures?.updateSettings {
            scrollEnabled = true
            quickZoomEnabled = true
            rotateEnabled = true
            pitchEnabled = true
        }
    }

    private suspend fun setupAnnotations() = withContext(Dispatchers.Default) {
        val bitmap = BitmapFactory.decodeResource(
            StorySpot.instance.resources,
            R.drawable.pin_marker
        )

        withContext(Dispatchers.Main) {
            mapView?.let { map ->
                val annotationApi = map.annotations
                pointAnnotationManager = annotationApi.createPointAnnotationManager()

                SimpleClustering.setupClustering(map, pointAnnotationManager!!, bitmap)
                ClusterZoomHandler.setupClusterClickHandler(map, "clustering-pins")
            }
        }
    }

    fun cleanup() {
        mapView = null
        pointAnnotationManager = null
    }
}