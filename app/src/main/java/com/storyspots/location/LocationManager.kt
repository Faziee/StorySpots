package com.storyspots.location

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
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
    private val prefs = context.getSharedPreferences("LocationPrefs", Context.MODE_PRIVATE)
    private var lastKnownLocation: Point? = null
    private var locationUpdateListener: OnIndicatorPositionChangedListener? = null

    fun saveLocation(point: Point) {
        lastKnownLocation = point
        prefs.edit().apply {
            putFloat("last_lat", point.latitude().toFloat())
            putFloat("last_lon", point.longitude().toFloat())
            apply()
        }
    }

    fun getLastLocation(): Point? {
        return lastKnownLocation ?: if (prefs.contains("last_lat") && prefs.contains("last_lon")) {
            Point.fromLngLat(
                prefs.getFloat("last_lon", 0f).toDouble(),
                prefs.getFloat("last_lat", 0f).toDouble()
            ).also { lastKnownLocation = it }
        } else {
            null
        }
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

    fun setupLocationComponent(
        mapView: MapView,
        onLocationUpdate: (Point) -> Unit,
        centerOnFirstUpdate: Boolean = true
    ) {
        // Configure location appearance
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

        // Add new listener
        val listener = OnIndicatorPositionChangedListener { point ->
            if (isValidLocation(point)) {
                saveLocation(point)
                onLocationUpdate(point)
            }
        }
        locationUpdateListener = listener
        mapView.location.addOnIndicatorPositionChangedListener(listener)

        // Center on last known location first if requested
        if (centerOnFirstUpdate) {
            getLastLocation()?.let { lastLocation ->
                centerOnLocation(mapView, lastLocation)
            }
        }
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