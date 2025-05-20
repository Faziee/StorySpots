package com.storyspots

import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.location


@OptIn(MapboxExperimental::class)
class MainActivity : ComponentActivity(), PermissionsListener {

    private val TAG = "MainActivity"
    private lateinit var permissionsManager: PermissionsManager

    private val locationPermissionGranted = mutableStateOf(false)

    private var pointAnnotationManager: PointAnnotationManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)
        FirebaseFirestore.getInstance().firestoreSettings =
            FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()

//        testFirestoreConnection()

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
                        mapboxMap.loadStyle("mapbox://styles/jordana-gc/cmad3b95m00oo01sdbs0r2rag"
                        ) {style ->
                            if (locationPermissionGranted.value) {
                                enableLocationComponent(this)
                            }

                            //have to explicitly call gestures to be allowed
                            val gesturesPlugin = this.gestures
                            gesturesPlugin.updateSettings {
                                scrollEnabled = true
                                quickZoomEnabled = true
                                rotateEnabled = true
                                pitchEnabled = true
                            }

                            //annotation manager
                            val annotationApi = annotations
                            pointAnnotationManager = annotationApi.createPointAnnotationManager()


                            mapboxMap.addOnMapClickListener {point ->

                                //Code for adding caption and image goes here
                                addPin(point)

                                true
                            }
                        }
                    }
                }
            )
        }
    }

    @Deprecated("Test")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onExplanationNeeded(permissionsToExplain: List<String>) {
        Toast.makeText(this, "This app needs location permission to show your location on the map.", Toast.LENGTH_LONG).show()
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            Toast.makeText(this, "Location permission granted!", Toast.LENGTH_SHORT).show()
            // Update the state to enable location on the map
            enableLocationOnMap()
        } else {
            Toast.makeText(this, "Location permission not granted :(", Toast.LENGTH_SHORT).show()
            locationPermissionGranted.value = false
        }
    }

    private fun enableLocationOnMap() {
        // This is the non-Compose version you can call from your activity
        runOnUiThread {
            locationPermissionGranted.value = true
            Log.d(TAG, "Location features enabled on map")
        }
    }

    private fun enableLocationComponent(mapView: MapView) {

        val locationComponentPlugin = mapView.location

        locationComponentPlugin.updateSettings {
            enabled = true
            pulsingEnabled = true
            pulsingColor = Color.BLUE
            pulsingMaxRadius = 40f
            showAccuracyRing = true
            accuracyRingColor = Color.parseColor("#4d89cff0")
            accuracyRingBorderColor = Color.parseColor("#80ffffff")
        }

        locationComponentPlugin.locationPuck = LocationPuck2D(
            bearingImage = null,  // You can add a custom bearing image
            shadowImage = null,   // You can add a custom shadow
            scaleExpression = null
        )

        locationComponentPlugin.enabled = true
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

    private fun addPin(point: Point){
        val context = this
        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.pin_marker)

        val annotationOptions = PointAnnotationOptions().withPoint(point).withIconImage(bitmap)

        pointAnnotationManager?.create(annotationOptions)
    }

}