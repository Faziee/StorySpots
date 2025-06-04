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
    private val prefs = context.getSharedPreferences("location_prefs", Context.MODE_PRIVATE)

    init {
        loadLastLocation()
    }

    private fun loadLastLocation() {
        val lat = prefs.getFloat("last_lat", 0f).toDouble()
        val lng = prefs.getFloat("last_lng", 0f).toDouble()
        if (lat != 0.0 && lng != 0.0) {
            currentLocation = Point.fromLngLat(lng, lat)
        }
    }

    private fun saveLastLocation(point: Point) {
        prefs.edit().apply {
            putFloat("last_lat", point.latitude().toFloat())
            putFloat("last_lng", point.longitude().toFloat())
            apply()
        }
    }

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
        }

        // Remove previous listener if exists
        locationUpdateListener?.let {
            mapView.location.removeOnIndicatorPositionChangedListener(it)
        }

        // Add new listener to track current location
        val listener = OnIndicatorPositionChangedListener { point ->
            if (isValidLocation(point)) {
                currentLocation = point
                saveLastLocation(point) // Save whenever we get updates
                onLocationUpdate(point)
            }
        }
        locationUpdateListener = listener
        mapView.location.addOnIndicatorPositionChangedListener(listener)

        if (centerOnFirstUpdate) {
            // First try to center on last known location
            currentLocation?.let { lastLocation ->
                mapView.camera.easeTo(
                    CameraOptions.Builder()
                        .center(lastLocation)
                        .zoom(15.0)
                        .build(),
                    MapAnimationOptions.mapAnimationOptions {
                        duration(0)
                    } // Instant
                )
            }

            // Then update to current location when available
            mapView.location.addOnIndicatorPositionChangedListener(
                object : OnIndicatorPositionChangedListener {
                    override fun onIndicatorPositionChanged(point: Point) {
                        if (isValidLocation(point)) {
                            currentLocation = point
                            saveLastLocation(point)
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