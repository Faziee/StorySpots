package com.storyspots.pin

import android.util.Log
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.gestures.addOnMapClickListener

class ClusterZoomHandler {

    companion object {
        private const val TAG = "ClusterZoomHandler"

        fun setupClusterClickHandler(mapView: MapView, sourceId: String) {
            Log.d(TAG, "Setting up cluster-specific click handler")

            mapView.mapboxMap.addOnMapClickListener { clickPoint ->
                val currentZoom = mapView.mapboxMap.cameraState.zoom

                if (currentZoom < 12.0) {
                    Log.d(TAG, "Low zoom level ($currentZoom), assuming cluster click - zooming in")

                    val newZoom = currentZoom + 3.0

                    mapView.camera.easeTo(
                        CameraOptions.Builder()
                            .center(clickPoint)
                            .zoom(newZoom)
                            .build(),
                        MapAnimationOptions.mapAnimationOptions {
                            duration(500)
                        }
                    )

                    return@addOnMapClickListener true
                } else {
                    Log.d(TAG, "High zoom level ($currentZoom), letting individual pin clicks work")
                    return@addOnMapClickListener false
                }
            }
        }
    }
}