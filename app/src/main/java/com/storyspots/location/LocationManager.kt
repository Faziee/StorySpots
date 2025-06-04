package com.storyspots.location

import android.content.Context
import android.graphics.Color
import android.util.Log
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.PuckBearing
import kotlin.math.abs

class LocationManager(private val context: Context) {
    private var locationUpdateListener: OnIndicatorPositionChangedListener? = null
    private var currentLocation: Point? = null

    fun setupLocationComponent(
        mapView: MapView,
        onLocationUpdate: (Point) -> Unit = {},
        centerOnFirstUpdate: Boolean = true
    ) {
        mapView.location.updateSettings {
            enabled = true
            puckBearingEnabled = true
            puckBearing = PuckBearing.COURSE
            locationPuck = LocationPuck2D()
            pulsingEnabled = true
            pulsingColor = Color.BLUE
            pulsingMaxRadius = 40f
            showAccuracyRing = true
            accuracyRingColor = Color.parseColor("#4d89cff0")
            accuracyRingBorderColor = Color.parseColor("#80ffffff")
        }

        // Remove previous listener if exists
        locationUpdateListener?.let {
            mapView.location.removeOnIndicatorPositionChangedListener(it)
        }

        // Add new listener to track current location
        val listener = OnIndicatorPositionChangedListener { point ->
            if (isValidLocation(point)) {
                currentLocation = point
                onLocationUpdate(point)
            }
        }
        locationUpdateListener = listener
        mapView.location.addOnIndicatorPositionChangedListener(listener)
        
        if (centerOnFirstUpdate) {
            mapView.location.addOnIndicatorPositionChangedListener(
                object : OnIndicatorPositionChangedListener {
                    override fun onIndicatorPositionChanged(point: Point) {
                        if (isValidLocation(point)) {
                            currentLocation = point
                            centerOnLocation(mapView, point)
                            mapView.location.removeOnIndicatorPositionChangedListener(this)
                        }
                    }
                }
            )
        }
    }

    fun recenterOnUserLocation(mapView: MapView, zoom: Double = 15.0): Boolean {
        return currentLocation?.let { point ->
            centerOnLocation(mapView, point, zoom)
            true
        } ?: false
    }

    fun centerOnLocation(mapView: MapView, point: Point, zoom: Double = 15.0) {
        mapView.camera.easeTo(
            CameraOptions.Builder()
                .center(point)
                .zoom(zoom)
                .build(),
            MapAnimationOptions.mapAnimationOptions { duration(1000) }
        )
    }

    fun cleanup(mapView: MapView) {
        locationUpdateListener?.let {
            mapView.location.removeOnIndicatorPositionChangedListener(it)
        }
        locationUpdateListener = null
    }

    private fun isValidLocation(point: Point): Boolean {
        return point.latitude() != 0.0 && point.longitude() != 0.0 &&
                abs(point.latitude()) <= 90 && abs(point.longitude()) <= 180
    }

    companion object {
        fun distanceBetween(point1: Point, point2: Point): Float {
            val results = FloatArray(1)
            android.location.Location.distanceBetween(
                point1.latitude(),
                point1.longitude(),
                point2.latitude(),
                point2.longitude(),
                results
            )
            return results[0]
        }
    }
}