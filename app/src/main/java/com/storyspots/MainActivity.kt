package com.storyspots

import BottomNavBar
import NavItem
import NotificationFeedScreen
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.GeoPoint
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.storyspots.location.LocationManager
import com.storyspots.location.RecenterButton
import com.storyspots.pin.ClusterZoomHandler
import com.storyspots.pin.SimpleClustering
import com.storyspots.caption.DismissibleStoryStack
import com.storyspots.caption.MapLoader
import com.storyspots.caption.StoryData
import com.storyspots.post.PostStoryScreen
import com.storyspots.pushNotification.NotificationPermissionHandler
import com.storyspots.settings.SettingsScreen
import com.storyspots.yourFeed.YourFeedScreen

@OptIn(MapboxExperimental::class)
class MainActivity : ComponentActivity(), PermissionsListener {

    private val TAG = "MainActivity"
    private lateinit var permissionsManager: PermissionsManager
    private val locationPermissionGranted = mutableStateOf(false)

    private lateinit var mapView: MapView
    private var currentScreen by mutableStateOf("home")

    private var selectedImageUri by mutableStateOf<Uri?>(null)
    private var currentUserLocation by mutableStateOf<Point?>(null)
    private var pendingImageSelection by mutableStateOf(false)

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

    private fun initializeContent() {
        contentInitialized = true
        setContent {
            MaterialTheme {
                NotificationPermissionHandler()
                // Permission launcher for media access
                val mediaPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        // Permission granted, launch image picker
                        Toast.makeText(this@MainActivity, "Permission granted! Select your image.", Toast.LENGTH_SHORT).show()
                        pendingImageSelection = true
                    } else {
                        // Permission denied
                        Toast.makeText(this@MainActivity, "Permission denied. Cannot access photos.", Toast.LENGTH_LONG).show()
                        pendingImageSelection = false
                    }
                }

                // Image picker launcher
                val imagePickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri ->
                    selectedImageUri = uri
                    pendingImageSelection = false
                }

                // Auto-launch image picker when permission is granted
                LaunchedEffect(pendingImageSelection) {
                    if (pendingImageSelection) {
                        imagePickerLauncher.launch("image/*")
                    }
                }

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
                            currentScreen == "your_feed" -> YourFeedScreen()
                            currentScreen == "settings" -> SettingsScreen()
                            currentScreen == "create" -> {
                                PostStoryScreen(
                                    onImageSelect = {
                                        // Check permission before opening gallery
                                        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            Manifest.permission.READ_MEDIA_IMAGES
                                        } else {
                                            Manifest.permission.READ_EXTERNAL_STORAGE
                                        }

                                        when (PackageManager.PERMISSION_GRANTED) {
                                            ContextCompat.checkSelfPermission(this@MainActivity, permission) -> {
                                                // Permission already granted, open gallery
                                                imagePickerLauncher.launch("image/*")
                                            }
                                            else -> {
                                                // Request permission
                                                mediaPermissionLauncher.launch(permission)
                                            }
                                        }
                                    },
                                    selectedImageUri = selectedImageUri,
                                    onPostSuccess = {
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Story posted successfully!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        // Clear the selected image and go back to home
                                        selectedImageUri = null
                                        currentScreen = "home"
                                    },
                                    getLocation = ::getCurrentLocation
                                )
                            }
                            else -> MapScreen()
                        }
                    }
                }
            }
        }
    }

    private fun getCurrentLocation(): GeoPoint? {
        return locationManager.currentLocation?.let { point ->
            GeoPoint(point.latitude(), point.longitude())
        }
    }

    @Composable
    fun PermissionRequestScreen() {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Requesting location permissions...")
        }
    }

    fun onStoryPosted() {
        MapLoader.refreshStories()
    }

    @Composable
    fun MapScreen() {
        val showFeed = remember { mutableStateOf(false) }
        val selectedPin = remember { mutableStateOf<Point?>(null) }
        val pinScreenOffset = remember { mutableStateOf<Offset?>(null) }
        var mapReady by remember { mutableStateOf(false) }

        var selectedStories by remember { mutableStateOf<List<StoryData>>(emptyList()) }
        var storyStackOffset by remember { mutableStateOf(Offset.Zero) }
        var showStoryStack by remember { mutableStateOf(false) }
        var mapView by remember { mutableStateOf<MapView?>(null) }

        LaunchedEffect(selectedPin.value) {
            selectedPin.value?.let { pin ->
                val screenCoords = mapView?.getMapboxMap()!!.pixelForCoordinate(pin)
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

                        this@MainActivity.mapView = this
                        getMapboxMap().loadStyleUri(
                            "mapbox://styles/jordana-gc/cmad3b95m00oo01sdbs0r2rag"
                        ) {
                            if (locationPermissionGranted.value) {
                                setupLocationTracking()
                            }

                            setupMapGestures()
                            setupAnnotations(context)
                            mapReady = true

                            MapLoader.initialize(this)
                            MapLoader.loadAllStories { allStories ->
                                Log.d("MainScreen", "Loaded ${allStories.size} stories")
                            }

                            MapLoader.setOnPinClickListener { storiesAtPin, offset ->
                                selectedStories = storiesAtPin
                                storyStackOffset = offset
                                showStoryStack = true
                            }

                            onStoryPosted()
                        }
                    }
                }
            )

            if(mapReady && ::locationManager.isInitialized) {
                RecenterButton(
                    mapView = mapView,
                    locationManager = locationManager,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 30.dp)
                )
            }

            if (showStoryStack && selectedStories.isNotEmpty()) {
                DismissibleStoryStack(
                    stories = selectedStories,
                    offset = storyStackOffset,
                    onDismiss = {
                        showStoryStack = false
                        selectedStories = emptyList()
                    }
                )
            }
        }
    }

    private fun setupLocationTracking() {
        locationManager.setupLocationComponent(
            mapView = mapView,
            onLocationUpdate = { point ->
                currentUserLocation = point // Update the currentUserLocation state
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
            }
            NavItem.YourFeed -> {
                currentScreen = "your_feed"
            }
            NavItem.Notifications -> {
                currentScreen = if (currentScreen == "notifications") "home" else "notifications"
            }
            NavItem.Settings -> {
                currentScreen = "settings"
            }
            NavItem.CreatePost -> {
                currentScreen = "create"
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
        val context = this
        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.pin_marker)

        val annotationOptions = PointAnnotationOptions()
            .withPoint(point)
            .withIconImage(bitmap)
            .withIconSize(0.1)
        pointAnnotationManager?.create(annotationOptions)
    }
}