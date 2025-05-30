package com.storyspots.pin

import android.util.Log
import com.mapbox.maps.MapView
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.gestures.addOnMapClickListener

class ClusterZoomHandler {

    companion object {
        private const val TAG = "ClusterZoomHandler"

        fun setupClusterClickHandler(mapView: MapView, sourceId: String) {
            Log.d(TAG, "Setting up cluster click handler - always zoom on click")

            mapView.mapboxMap.addOnMapClickListener { point ->
                val currentZoom = mapView.mapboxMap.cameraState.zoom

                if (currentZoom < 16.0) {  // Zoom in until level 16
                    Log.d(TAG, "Zoom $currentZoom - zooming in to break clusters")

                    mapView.camera.easeTo(
                        CameraOptions.Builder()
                            .center(point)
                            .zoom(currentZoom + 2.0)
                            .build(),
                        MapAnimationOptions.mapAnimationOptions {
                            duration(500)
                        }
                    )

                    true // Consume event
                } else {
                    Log.d(TAG, "Max zoom reached - allowing pin creation")
                    false // Allow pin creation
                }
            }
        }
    }
}