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

    private val _currentLocation = MutableStateFlow<Point?>(null)
    val currentLocation: StateFlow<Point?> = _currentLocation.asStateFlow()

    private val prefs = context.getSharedPreferences("location_prefs", Context.MODE_PRIVATE)
    private var isFollowingUser = true

    init {
        loadLastLocation()
    }


    fun isFollowingUser(): Boolean = isFollowingUser

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
            Log.e("LocationsManager", "Error loading last location", e)
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

        locationUpdateListener?.let {
            mapView.location.removeOnIndicatorPositionChangedListener(it)
        }

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

    fun initializeCameraPosition(mapView: MapView) {
        _currentLocation.value?.let { lastLocation ->
            mapView.mapboxMap.setCamera(
                CameraOptions.Builder()
                    .center(lastLocation)
                    .zoom(15.0)
                    .build()
            )
        } ?: run {
            mapView.mapboxMap.setCamera(
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
        Log.d("LocationsManager", "Current location: ${_currentLocation.value}")
        Log.d("LocationsManager", "MapView is null: ${mapView == null}")

        if (mapView == null) {
            Log.e("LocationsManager", "MapView is null, cannot recenter")
            return false
        }

        return _currentLocation.value?.let { point ->
            Log.d("LocationsManager", "Recentering to: lat=${point.latitude()}, lng=${point.longitude()}")

            val wasFollowing = isFollowingUser
            isFollowingUser = false
            Log.d("LocationsManager", "Disabled follow mode to prevent animation conflicts")

            try {
                mapView.location.enabled = false
                Log.d("LocationsManager", "Temporarily disabled location component")
            } catch (e: Exception) {
                Log.w("LocationsManager", "Could not disable location component: ${e.message}")
            }

            Log.d("LocationsManager", "Calling centerOnLocation...")
            centerOnLocation(mapView, point, zoom)
            Log.d("LocationsManager", "centerOnLocation completed")

            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    mapView.location.enabled = true
                    Log.d("LocationsManager", "Re-enabled location component")

                    isFollowingUser = wasFollowing
                    Log.d("LocationsManager", "Re-enabled follow mode: $wasFollowing")

                    val currentCenter = mapView.getMapboxMap().cameraState.center
                    Log.d("LocationsManager", "Final camera center: lat=${currentCenter.latitude()}, lng=${currentCenter.longitude()}")
                    Log.d("LocationsManager", "Distance from target: ${distanceBetween(point, currentCenter)} meters")
                } catch (e: Exception) {
                    Log.e("LocationsManager", "Error re-enabling location features", e)
                }
            }, 1200)

            true
        } ?: run {
            Log.w("LocationsManager", "Cannot recenter - current location is null")
            false
        }
    }

    private fun centerOnLocation(mapView: MapView?, point: Point, zoom: Double) {
        Log.d("LocationsManager", "centerOnLocation called with zoom: $zoom")

        if (mapView == null) {
            Log.e("LocationsManager", "MapView is null in centerOnLocation")
            return
        }

        try {
            val currentCamera = mapView.getMapboxMap().cameraState
            Log.d("LocationsManager", "Current camera before: lat=${currentCamera.center.latitude()}, lng=${currentCamera.center.longitude()}, zoom=${currentCamera.zoom}")

            val cameraOptions = CameraOptions.Builder()
                .center(point)
                .zoom(zoom)
                .build()

            Log.d("LocationsManager", "Flying to: lat=${point.latitude()}, lng=${point.longitude()}, zoom=$zoom")

            mapView.camera.flyTo(
                cameraOptions,
                MapAnimationOptions.Builder()
                    .duration(1000)
                    .build()
            )

            Log.d("LocationsManager", "Camera flyTo animation started")

            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                val newCamera = mapView.getMapboxMap().cameraState
                Log.d("LocationsManager", "Camera during animation: lat=${newCamera.center.latitude()}, lng=${newCamera.center.longitude()}")
            }, 500)

        } catch (e: Exception) {
            Log.e("LocationsManager", "Error in centerOnLocation", e)
        }
    }

    private fun distanceBetween(point1: Point, point2: Point): Double {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            point1.latitude(), point1.longitude(),
            point2.latitude(), point2.longitude(),
            results
        )
        return results[0].toDouble()
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
