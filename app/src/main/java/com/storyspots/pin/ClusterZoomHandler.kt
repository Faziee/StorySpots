package com.storyspots.pin

import android.util.Log
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.RenderedQueryGeometry
import com.mapbox.maps.RenderedQueryOptions
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.google.gson.JsonElement

// Extension function to get properties from QueriedRenderedFeature
fun com.mapbox.maps.QueriedRenderedFeature.getProperty(propertyName: String): JsonElement? {
    return this.queriedFeature.feature.properties()?.get(propertyName)
}

class ClusterZoomHandler {

    companion object {
        private const val TAG = "ClusterZoomHandler"
        private const val SMALL_CLUSTER_THRESHOLD = 5

        fun setupClusterClickHandler(
            mapView: MapView,
            onSmallClusterClick: ((Point, Int) -> Unit)? = null
        ) {
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
                        val feature = features.first()
                        val pointCount = feature.getProperty("point_count")?.asInt ?: 0

                        Log.d(TAG, "Clicked on cluster with $pointCount stories")

                        if (pointCount <= SMALL_CLUSTER_THRESHOLD) {
                            Log.d(TAG, "Small cluster ($pointCount stories) - showing story stack")
                            onSmallClusterClick?.invoke(point, pointCount)
                        } else {
                            Log.d(TAG, "Large cluster ($pointCount stories) - zooming in")
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
                        }
                    } else {
                        Log.d(TAG, "Not a cluster click - ignoring")
                    }
                }

                false
            }
        }
    }
}