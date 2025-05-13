package com.storyspots

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.MapboxExperimental
import androidx.compose.ui.viewinterop.AndroidView
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.animation.camera


@OptIn(MapboxExperimental::class)
class MainActivity : ComponentActivity(), PermissionsListener {

    private val TAG = "MainActivity"
    private lateinit var permissionsManager: PermissionsManager

    private val locationPermissionGranted = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)
        testFirestoreConnection()

        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            Log.d(TAG, "Location permission already granted")
            locationPermissionGranted.value = true
        } else {
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(this)
        }

        setContent {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    MapView(context).apply {
                        getMapboxMap().loadStyleUri(
                            "mapbox://styles/jordana-gc/cmad3b95m00oo01sdbs0r2rag"
                        ) {
                            if (locationPermissionGranted.value) {
                                enableLocationComponent(this)
                            }
                        }
                    }
                }
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onExplanationNeeded(permissionsToExplain: List<String>) {
        Toast.makeText(
            this,
            "This app needs location permission to show your location on the map.",
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onPermissionResult(granted: Boolean) {
        locationPermissionGranted.value = granted

        if (granted) {
            Toast.makeText(this, "Location permission granted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Location permission not granted :(", Toast.LENGTH_SHORT).show()
            locationPermissionGranted.value = false
        }
    }

    private fun MapView.safeCenterOnLocation() {
        val locationListener = object : (Point) -> Unit {
            override fun invoke(point: Point) {
                centerMapOnUserLocation(this@safeCenterOnLocation, point)
                location.removeOnIndicatorPositionChangedListener(this)
            }
        }
        location.addOnIndicatorPositionChangedListener(locationListener)
    }

    private fun enableLocationComponent(mapView: MapView) {

        mapView.location.updateSettings {
            enabled = true
            puckBearingEnabled = true
            locationPuck = LocationPuck2D()
            pulsingEnabled = true
            pulsingColor = android.graphics.Color.BLUE
            pulsingMaxRadius = 40f
            showAccuracyRing = true
            accuracyRingColor = android.graphics.Color.parseColor("#4d89cff0")
            accuracyRingBorderColor = android.graphics.Color.parseColor("#80ffffff")
        }

        mapView.safeCenterOnLocation()

        mapView.location.addOnIndicatorPositionChangedListener { point ->
            centerMapOnUserLocation(mapView, point)
        }
    }

    private fun centerMapOnUserLocation(mapView: MapView, point: Point) {
        mapView.camera.easeTo(
            CameraOptions.Builder()
                .center(point)
                .zoom(15.0) 
                .build()
        )
    }

    private fun testFirestoreConnection() {
        try {
            Log.d(TAG, "Attempting to initialize Firebase Firestore")
            val db = FirebaseFirestore.getInstance()
            Log.d(TAG, "Firestore instance obtained successfully")

            val testData = hashMapOf(
                "timestamp" to System.currentTimeMillis(),
                "message" to "Firebase connection test"
            )

            db.collection("connection_tests")
                .add(testData)
                .addOnSuccessListener { documentReference ->
                    Log.d(TAG, "Firebase connection successful! Document ID: ${documentReference.id}")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Firebase connection failed", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase", e)
        }
    }
}