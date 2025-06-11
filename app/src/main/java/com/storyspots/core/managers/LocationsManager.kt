package com.storyspots.core.managers

import android.content.Context
import android.graphics.Color
import android.util.Log
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

class LocationsManager(private val context: Context) {
    private var locationUpdateListener: OnIndicatorPositionChangedListener? = null
    private var moveListener: OnMoveListener? = null

    // Expose current location as StateFlow
    private val _currentLocation = MutableStateFlow<Point?>(null)
    val currentLocation: StateFlow<Point?> = _currentLocation.asStateFlow()

    private var hasCenteredOnFirstLocation = false
    private val prefs = context.getSharedPreferences("location_prefs", Context.MODE_PRIVATE)
    private var isFollowingUser = true

    init {
        loadLastLocation()
    }

    fun isFollowingUser(): Boolean = isFollowingUser

    fun enableFollowMode(mapView: MapView? = null) {
        isFollowingUser = true
        mapView?.let {
            _currentLocation.value?.let { point ->
                centerOnLocation(it, point)
            }
        }
    }

    fun disableFollowMode() {
        isFollowingUser = false
    }

    private fun loadLastLocation() {
        try {
            val lat = prefs.getFloat("last_lat", 0f).toDouble()
            val lng = prefs.getFloat("last_lng", 0f).toDouble()
            if (lat != 0.0 && lng != 0.0) {
                _currentLocation.value = Point.fromLngLat(lng, lat)
            }
        } catch (e: Exception) {
            Log.e("LocationManager", "Error loading last location", e)
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

        moveListener = object : OnMoveListener {
            override fun onMoveBegin(detector: MoveGestureDetector) {
                disableFollowMode()
            }
            override fun onMove(detector: MoveGestureDetector) = false
            override fun onMoveEnd(detector: MoveGestureDetector) {}
        }.also { mapView.gestures.addOnMoveListener(it) }

        // Remove previous location listener if exists
        locationUpdateListener?.let {
            mapView.location.removeOnIndicatorPositionChangedListener(it)
        }

        // Add new location listener
        locationUpdateListener = OnIndicatorPositionChangedListener { point ->
            if (isValidLocation(point)) {
                _currentLocation.value = point
                saveLastLocation(point)
                if (isFollowingUser) {
                    smoothFollowLocation(mapView, point)
                }
                onLocationUpdate(point)
            }
        }.also { mapView.location.addOnIndicatorPositionChangedListener(it) }

        if (centerOnFirstUpdate) {
            initializeCameraPosition(mapView)
        }
    }

    private fun initializeCameraPosition(mapView: MapView) {
        _currentLocation.value?.let { lastLocation ->
            mapView.getMapboxMap().setCamera(
                CameraOptions.Builder()
                    .center(lastLocation)
                    .zoom(15.0)
                    .build()
            )
        } ?: run {
            mapView.getMapboxMap().setCamera(
                CameraOptions.Builder()
                    .center(Point.fromLngLat(0.0, 0.0))
                    .zoom(1.0)
                    .build()
            )
        }
    }

    private fun smoothFollowLocation(mapView: MapView, point: Point) {
        mapView.camera.easeTo(
            CameraOptions.Builder()
                .center(point)
                .zoom(15.0)
                .build(),
            MapAnimationOptions.mapAnimationOptions { duration(500) }
        )
    }

    fun recenterOnUserLocation(mapView: MapView?, zoom: Double = 15.0): Boolean {
        return _currentLocation.value?.let { point ->
            enableFollowMode(mapView)
            centerOnLocation(mapView, point, zoom)
            true
        } ?: false
    }

    fun centerOnLocation(mapView: MapView?, point: Point, zoom: Double = 15.0) {
        mapView!!.camera.easeTo(
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
        moveListener?.let {
            mapView.gestures.removeOnMoveListener(it)
        }
        locationUpdateListener = null
        moveListener = null
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
