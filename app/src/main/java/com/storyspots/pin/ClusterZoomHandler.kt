package com.storyspots.pin

import android.util.Log
import com.mapbox.maps.MapView
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.RenderedQueryGeometry
import com.mapbox.maps.RenderedQueryOptions
import com.mapbox.maps.plugin.gestures.addOnMapClickListener

class ClusterZoomHandler {

    companion object {
        private const val TAG = "ClusterZoomHandler"

        fun setupClusterClickHandler(mapView: MapView, sourceId: String) {
            Log.d(TAG, "Setting up cluster-specific click handler")

            mapView.mapboxMap.addOnMapClickListener { point ->
                Log.d(TAG, "Map clicked, checking if it's on a cluster...")

                val screenCoordinate = mapView.mapboxMap.pixelForCoordinate(point)

                mapView.mapboxMap.queryRenderedFeatures(
                    RenderedQueryGeometry(screenCoordinate),
                    RenderedQueryOptions(listOf("cluster-circles"), null)
                ) { result ->
                    val features = result.value

                    if (!features.isNullOrEmpty()) {
                        Log.d(TAG, "Clicked on cluster! Zooming in...")

                        val currentZoom = mapView.mapboxMap.cameraState.zoom

                        mapView.camera.easeTo(
                            CameraOptions.Builder()
                                .center(point)
                                .zoom(currentZoom + 3.0)
                                .build(),
                            MapAnimationOptions.mapAnimationOptions {
                                duration(500)
                            }
                        )
                    } else {
                        Log.d(TAG, "Not a cluster click - ignoring")
                    }
                }

                false
            }
        }
    }
}