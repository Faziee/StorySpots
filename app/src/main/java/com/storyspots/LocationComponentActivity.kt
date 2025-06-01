package com.storyspots

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.storyspots.Initialise

public class LocationComponentActivity : ComponentActivity() {
    //TODO: Re-centering issue source?
    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener { point ->
        centerMapOnUserLocation(mapView, point)
    }

    //TODO: Re-centering issue source?
    private val onIndicatorBearingChangedListener = OnIndicatorBearingChangedListener { bearing ->
        mapView.mapboxMap.setCamera(CameraOptions.Builder().bearing(bearing).build())
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val mapViewportState = rememberMapViewportState()
            MapboxMap(
                Modifier.fillMaxSize(),
                mapViewportState = mapViewportState,
            ) {
                MapEffect(Unit) { mapView ->
                    mapView.location.updateSettings {
                        locationPuck = createDefault2DPuck(withBearing = true)
                        enabled = true
                        puckBearing = PuckBearing.COURSE
                        puckBearingEnabled = true
                    }
                    mapViewportState.transitionToFollowPuckState()
                }
            }
        }
    }

    //TODO: Map Control
    private fun centerMapOnUserLocation(mapView: MapView, point: Point) {
        mapView.camera.easeTo(
            CameraOptions.Builder()
                .center(point)
                .zoom(15.0)
                .build(),
            MapAnimationOptions.mapAnimationOptions {
                duration(1000)
            }
        )
    }

    //TODO: Map Control
    override fun onDestroy() {
        super.onDestroy()
        if (::mapView.isInitialized) {
            mapView.location.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
            mapView.location.removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        }
    }

    //TODO: Map Re-centering issue source?
    fun enableLocationComponent(mapView: MapView) {
        Initialise().mapLocationSettings(mapView)

        mapView.location.addOnIndicatorPositionChangedListener { point ->
            if (point.latitude() != 0.0 && point.longitude() != 0.0) {
                mapView.camera.easeTo(
                    CameraOptions.Builder()
                        .center(point)
                        .zoom(15.0)
                        .build(),
                    MapAnimationOptions.mapAnimationOptions { duration(1000) }
                )
            }
        }
    }
}