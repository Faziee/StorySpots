package com.storyspots

import BottomNavBar
import NavItem
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.storyspots.caption.DismissibleStoryStack
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.geojson.Point
import com.storyspots.location.LocationManager
import com.storyspots.location.RecenterButton
import com.storyspots.pin.ClusterZoomHandler
import com.storyspots.pin.SimpleClustering

@OptIn(MapboxExperimental::class)
class MainActivity : ComponentActivity(), PermissionsListener {

    private val TAG = "MainActivity"
    private lateinit var permissionsManager: PermissionsManager
    private val locationPermissionGranted = mutableStateOf(false)

    private lateinit var mapView: MapView
    private var currentScreen by mutableStateOf("home")
    private var pointAnnotationManager: PointAnnotationManager? = null
    private var contentInitialized = false
    private lateinit var locationManager: LocationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        locationManager = LocationManager(this)

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

    fun initializeContent() {
        contentInitialized = true
        setContent {
            MaterialTheme {
                Scaffold(
                    bottomBar = {
                        BottomNavBar(
                            currentRoute = currentScreen,
                            onItemClick = { item ->
                                handleNavItemClick(item)
                            }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when {
                            !locationPermissionGranted.value -> PermissionRequestScreen()
                            currentScreen == "notifications" -> NotificationFeedScreen()
                            else -> MapScreen()
                        }
                    }
                }
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
        
        val selectedPin = remember { mutableStateOf<Point?>(null) }
        val pinScreenOffset = remember { mutableStateOf<Offset?>(null) }
        var mapReady by remember { mutableStateOf(false) }

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

                        getMapboxMap().setCamera(
                            CameraOptions.Builder()
                                .center(Point.fromLngLat(0.0, 0.0))
                                .zoom(1.0)
                                .build()
                        )

                        getMapboxMap().loadStyleUri(
                            "mapbox://styles/jordana-gc/cmad3b95m00oo01sdbs0r2rag"
                        ) {
                            FirebaseFirestore.getInstance()
                                .collection("story")
                                .get()
                                .addOnSuccessListener { documents ->
                                    pointAnnotationManager?.deleteAll()

                                    for (document in documents) {
                                        val geoPoint = document.getGeoPoint("location") // assuming your field is named "location"
                                        if (geoPoint != null) {
                                            val point = Point.fromLngLat(geoPoint.longitude, geoPoint.latitude)
                                            addPin(point)
                                        }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.w(TAG, "Error fetching geo points", e)
                                }

                            if (locationPermissionGranted.value) {
                                setupLocationTracking()
                            }

                            setupMapGestures()
                            setupAnnotations(context)
                            mapReady = true
                        }
                    }
                }
            )

            if(mapReady && ::locationManager.isInitialized)
            {
                RecenterButton(
                    mapView = mapView,
                    locationManager = locationManager,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 30.dp)
                )
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

    private fun setupLocationTracking() {
        locationManager.setupLocationComponent(
            mapView = mapView,
            onLocationUpdate = { point ->
            },
            centerOnFirstUpdate = true
        )
    }

    private fun MapView.setupMapGestures() {
        gestures.updateSettings {
            scrollEnabled = true
            quickZoomEnabled = true
            rotateEnabled = true
            pitchEnabled = true
        }

        mapboxMap.addOnMapClickListener { point ->
            if (mapboxMap.cameraState.zoom >= 12.0) {
                addPin(point)
                false  // Don't consume event
            } else {
                false
            }
        }
    }

    private fun MapView.setupAnnotations(context: android.content.Context) {
        val annotationApi = annotations
        pointAnnotationManager = annotationApi.createPointAnnotationManager()

        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.pin_marker)
        SimpleClustering.setupClustering(this, pointAnnotationManager!!, bitmap)
        ClusterZoomHandler.setupClusterClickHandler(this, "clustering-pins")

        SimpleClustering.setOnPinClickListener { point ->
            Log.d("MainActivity", "Pin clicked at: $point")
        }

        pointAnnotationManager?.addClickListener { annotation ->
            true
        }
    }

    private fun handleNavItemClick(item: NavItem) {
        when (item) {
            NavItem.Home -> {
                currentScreen = "home"
                Toast.makeText(this, "Home selected", Toast.LENGTH_SHORT).show()
            }
            NavItem.Favourites -> {
                currentScreen = "favorites"
                Toast.makeText(this, "Favourites selected", Toast.LENGTH_SHORT).show()
            }
            NavItem.Notifications -> {
                // Toggle between notifications and map
                currentScreen = if (currentScreen == "notifications") "home" else "notifications"
            }
            NavItem.Settings -> {
                currentScreen = "settings"
                Toast.makeText(this, "Settings selected", Toast.LENGTH_SHORT).show()
            }
            NavItem.CreatePost -> {
                currentScreen = "create"
                Toast.makeText(this, "Create Post selected", Toast.LENGTH_SHORT).show()
            }

            else -> {}
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
        val message = if (granted) "Location permission granted!" else "Location permission not granted :("
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

        if (granted) {
            if (!contentInitialized) {
                initializeContent()
            } else if (::mapView.isInitialized) {
                setupLocationTracking()
            }
        } else if (!contentInitialized) {
            initializeContent()
        }
    }

    override fun onDestroy() {
        if (::mapView.isInitialized && ::locationManager.isInitialized) {
            locationManager.cleanup(mapView)
        }
        super.onDestroy()
    }

    private fun addPin(point: Point) {
        SimpleClustering.addClusterPin(point)
    }
}