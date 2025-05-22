package com.storyspots

import NotificationFeedScreen
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.geojson.Point
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.storyspots.caption.StoryStack
import com.storyspots.ui.components.DismissibleStoryStack


@OptIn(MapboxExperimental::class)
class MainActivity : ComponentActivity(), PermissionsListener {

    private val TAG = "MainActivity"
    private lateinit var permissionsManager: PermissionsManager
    private val locationPermissionGranted = mutableStateOf(false)

    private lateinit var mapView: MapView

    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener { point ->
        centerMapOnUserLocation(mapView, point)
    }

    private val onIndicatorBearingChangedListener = OnIndicatorBearingChangedListener { bearing ->
        mapView.mapboxMap.setCamera(CameraOptions.Builder().bearing(bearing).build())
    }

    private var pointAnnotationManager: PointAnnotationManager? = null
    private var contentInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)
        FirebaseFirestore.getInstance().firestoreSettings =
            FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()

        // checking permissions
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            Log.d(TAG, "Location permission already granted")
            locationPermissionGranted.value = true
            initializeContent()
        } else {
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(this)
        }
    }

    private fun initializeContent() {
        contentInitialized = true
        setContent {
            if (locationPermissionGranted.value) {
                MapScreen()
            } else {
                PermissionRequestScreen()
            }
        }
    }

    @Composable
    fun PermissionRequestScreen() {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Requesting location permissions...")
        }
    }

    @Composable
    fun MapScreen() {
        //This and (NAVIGATE TO LN 157) will be replaced by navbar later
        val showFeed = remember { mutableStateOf(false) }

        //This is for the map captions
        val selectedPin = remember { mutableStateOf<Point?>(null) }

        val pinScreenOffset = remember { mutableStateOf<Offset?>(null) }

        LaunchedEffect(selectedPin.value) {
            selectedPin.value?.let { pin ->
                val screenCoords = mapView.getMapboxMap().pixelForCoordinate(pin)
                pinScreenOffset.value = Offset(screenCoords.x.toFloat(), screenCoords.y.toFloat())
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    MapView(context).also { mapView = it }.apply {
                        mapboxMap.loadStyle("mapbox://styles/jordana-gc/cmad3b95m00oo01sdbs0r2rag"
                        ) { style ->
                            if (locationPermissionGranted.value) {
                                enableLocationComponent(this)
                            }

                            // have to explicitly call gestures to be allowed
                            val gesturesPlugin = this.gestures
                            gesturesPlugin.updateSettings {
                                scrollEnabled = true
                                quickZoomEnabled = true
                                rotateEnabled = true
                                pitchEnabled = true
                            }

                            // annotation manager
                            val annotationApi = annotations
                            pointAnnotationManager = annotationApi.createPointAnnotationManager()

                            mapboxMap.addOnMapClickListener { point ->
                                // Code for adding caption and image goes here
                                addPin(point)
                                true
                            }

                            pointAnnotationManager?.addClickListener { annotation ->
                                selectedPin.value = annotation.point
                                true
                            }
                        }
                    }
                }
            )

            Button(
                onClick = { showFeed.value = !showFeed.value },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .zIndex(1f)
            ) {
                Text(if (showFeed.value) "Back to Map" else "Open Feed")
            }

            when {
                showFeed.value -> NotificationFeedScreen()
            }

            selectedPin.value?.let { pin ->
                pinScreenOffset.value?.let { offset ->
                    DismissibleStoryStack(
                        offset = offset,
                        onDismiss = { selectedPin.value = null }
                    )
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
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

            // Initialize content if not already initialized
            if (!contentInitialized) {
                initializeContent()
            } else if (::mapView.isInitialized) {
                mapView.getMapboxMap().getStyle { style ->
                    enableLocationComponent(mapView)
                }
            }
        } else {
            Toast.makeText(this, "Location permission not granted :(", Toast.LENGTH_SHORT).show()
            locationPermissionGranted.value = false
            if (!contentInitialized) {
                initializeContent()
            }
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
            puckBearing = PuckBearing.COURSE
            locationPuck = LocationPuck2D()

            pulsingEnabled = true
            pulsingColor = Color.BLUE
            pulsingMaxRadius = 40f
            showAccuracyRing = true
            accuracyRingColor = Color.parseColor("#4d89cff0")
            accuracyRingBorderColor = Color.parseColor("#80ffffff")
        }

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

    override fun onDestroy() {
        super.onDestroy()

        if (::mapView.isInitialized) {
            mapView.location.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
            mapView.location.removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        }
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

    private fun addPin(point: Point) {
        val context = this
        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.pin_marker)

        val annotationOptions = PointAnnotationOptions()
            .withPoint(point)
            .withIconImage(bitmap)
            .withIconSize(0.1)

        pointAnnotationManager?.create(annotationOptions)
    }
}