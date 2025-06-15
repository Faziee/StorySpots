package com.storyspots.ui

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.mapbox.geojson.Point
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import androidx.compose.ui.geometry.Offset
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.storyspots.cache.StoryCache
import com.storyspots.caption.DismissibleStoryStack
import com.storyspots.caption.MapLoader
import com.storyspots.caption.StoryData
import com.storyspots.caption.fetchAllStories
import com.storyspots.caption.toStoryData
import com.storyspots.core.AppComponents
import com.storyspots.core.managers.LocationsManager
import com.storyspots.location.RecenterButton
import com.storyspots.pin.ClusterZoomHandler
import com.storyspots.pin.SimpleClustering
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MapScreen() {
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var mapReady by remember { mutableStateOf(false) }
    var showStoryStack by remember { mutableStateOf(false) }
    var selectedStories by remember { mutableStateOf<List<StoryData>>(emptyList()) }
    var storyStackOffset by remember { mutableStateOf(Offset.Zero) }
    val mapInstanceId = remember { System.currentTimeMillis().toInt() }
    val context = LocalContext.current

    LaunchedEffect(mapReady, mapView) {
        if (mapReady && mapView != null) {
            Log.d("MapScreen", "Map ready, loading stories for instance $mapInstanceId")
            var attempts = 0
            while (!SimpleClustering.isClusteringInitialized() && attempts < 10) {
                Log.d("MapScreen", "Waiting for clustering to initialize... attempt ${attempts + 1}")
                kotlinx.coroutines.delay(100)
                attempts++
            }

            Log.d("MapScreen", "Clustering initialized: ${SimpleClustering.isClusteringInitialized()}")

            val resourceId = context.resources.getIdentifier("pin_marker", "drawable", context.packageName)
            Log.d("MapScreen", "Pin marker resource ID: $resourceId (0 means not found)")

            if (!SimpleClustering.isClusteringInitialized()) {
                Log.e("MapScreen", "Clustering failed to initialize after waiting")
                return@LaunchedEffect
            }

            val currentStoriesCount = AppComponents.mapStateManager.getStoriesCount()
            Log.d("MapScreen", "Current stories count: $currentStoriesCount")

            if (currentStoriesCount > 0) {
                Log.d("MapScreen", "Using existing stories from MapStateManager: $currentStoriesCount")
                AppComponents.mapStateManager.showPinsOnMap(mapInstanceId)
            } else {
                Log.d("MapScreen", "No stories in memory, loading from cache/network")
                loadStoriesForMap(mapInstanceId)
            }
        }
    }

    DisposableEffect(Unit) {
        Log.d("MapScreen", "MapScreen created/recreated")
        mapReady = false

        onDispose {
            Log.d("MapScreen", "MapScreen disposed")
            mapView?.let { view ->
                AppComponents.locationManager.cleanup(view)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                Log.d("MapScreen", "Creating new MapView instance")
                val mapInitOptions = MapInitOptions(context = ctx)
                MapView(ctx, mapInitOptions).also { map ->
                    mapView = map
                    map.getMapboxMap().loadStyleUri("mapbox://styles/jordana-gc/cmad3b95m00oo01sdbs0r2rag") { style ->
                        AppComponents.appScope.launch {
                            if (!map.isDestroyed) {
                                Log.d("MapScreen", "Style loaded, initializing map")
                                initializeMapAsync(map, style) { storiesAtPin, offset ->
                                    selectedStories = storiesAtPin
                                    storyStackOffset = offset
                                    showStoryStack = true
                                }
                                mapReady = true
                                Log.d("MapScreen", "Map marked as ready")
                            }
                        }
                    }
                }
            },
            update = { }
        )

        // Recenter button
        if (mapReady && mapView != null) {
            RecenterButton(
                mapView = mapView,
                locationManager = AppComponents.locationManager,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 30.dp)
            )
        }

        // Story stack overlay
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

private val MapView.isDestroyed: Boolean
    get() = try {
        mapboxMap.getStyle()
        false
    } catch (e: Exception) {
        true
    }

@Composable
private fun BoxScope.RecenterButton(
    mapView: MapView?,
    locationManager: LocationsManager,
    modifier: Modifier
) {
    com.storyspots.location.RecenterButton(
        mapView = mapView,
        locationManager = locationManager,
        modifier = modifier
    )
}

private suspend fun initializeMapAsync(
    mapView: MapView,
    style: Style,
    onPinClick: (List<StoryData>, Offset) -> Unit
) = withContext(Dispatchers.Default) {
    try {
        Log.d("MapScreen", "Starting map initialization")

        withContext(Dispatchers.Main) {
            // Setup location component
            AppComponents.locationManager.setupLocationComponent(
                mapView = mapView,
                onLocationUpdate = { /* Handle updates */ },
                centerOnFirstUpdate = true
            )

            // Initialize the map manager for this new instance
            AppComponents.mapManager.initializeMap(mapView)

            // Initialize MapLoader
            MapLoader.initialize(mapView)

            Log.d("MapScreen", "About to initialize clustering")

            // Initialize clustering directly here
            val context = mapView.context
            val resourceId = context.resources.getIdentifier("pin_marker", "drawable", context.packageName)
            if (resourceId != 0) {
                val bitmap = BitmapFactory.decodeResource(context.resources, resourceId)
                if (bitmap != null) {
                    val annotationApi = mapView.annotations
                    val pointAnnotationManager = annotationApi.createPointAnnotationManager()

                    SimpleClustering.setupClustering(mapView, pointAnnotationManager, bitmap)

                    // Set up small cluster click listener
                    SimpleClustering.setOnSmallClusterClickListener { stories, offset ->
                        Log.d("MapScreen", "Small cluster clicked with ${stories.size} stories")
                        onPinClick(stories, offset)
                    }

                    // Set up cluster zoom handler with small cluster callback
                    ClusterZoomHandler.setupClusterClickHandler(mapView, "clustering-pins") { point, pointCount ->
                        Log.d("MapScreen", "Small cluster clicked at $point with $pointCount stories")

                        // Get stories near this cluster point
                        val storiesAtLocation = AppComponents.mapStateManager.currentStories.value.filter { story ->
                            story.location?.let { geoPoint ->
                                val distance = calculateDistance(point, geoPoint)
                                distance < 0.001 // Slightly larger radius for clusters
                            } ?: false
                        }

                        Log.d("MapScreen", "Cluster clicked: found ${storiesAtLocation.size} stories at location")

                        if (storiesAtLocation.isNotEmpty()) {
                            val offset = convertPointToOffsetWithPadding(point, mapView, 80f)
                            onPinClick(storiesAtLocation, offset)
                        }
                    }

                    Log.d("MapScreen", "Clustering setup completed")
                } else {
                    Log.e("MapScreen", "Failed to decode pin_marker bitmap")
                }
            } else {
                Log.e("MapScreen", "pin_marker drawable not found")
            }

            // Delay to allow clustering to initialize
            kotlinx.coroutines.delay(300)

            Log.d("MapScreen", "Clustering initialized after setup: ${SimpleClustering.isClusteringInitialized()}")

            // Set up direct pin click handling through SimpleClustering
            SimpleClustering.setOnPinClickListener { clickedPoint ->
                Log.d("MapScreen", "Individual pin clicked at: ${clickedPoint.latitude()}, ${clickedPoint.longitude()}")
                val storiesAtLocation = AppComponents.mapStateManager.currentStories.value.filter { story ->
                    story.location?.let { geoPoint ->
                        val distance = calculateDistance(clickedPoint, geoPoint)
                        distance < 0.0001
                    } ?: false
                }

                Log.d("MapScreen", "Pin clicked: found ${storiesAtLocation.size} stories at location")

                if (storiesAtLocation.isNotEmpty()) {
                    val offset = convertPointToOffsetWithPadding(clickedPoint, mapView, 80f)
                    onPinClick(storiesAtLocation, offset)
                }
            }
        }

        Log.d("MapScreen", "Map initialization completed")

    } catch (e: Exception) {
        Log.e("MapScreen", "Map initialization failed", e)
    }
}

private fun calculateDistance(point: com.mapbox.geojson.Point, geoPoint: com.google.firebase.firestore.GeoPoint): Double {
    val latDiff = point.latitude() - geoPoint.latitude
    val lngDiff = point.longitude() - geoPoint.longitude
    return kotlin.math.sqrt(latDiff * latDiff + lngDiff * lngDiff)
}

private fun convertPointToOffsetWithPadding(point: com.mapbox.geojson.Point, mapView: MapView, yPadding: Float = 100f): Offset {
    val screenCoordinate = mapView.mapboxMap.pixelForCoordinate(point)
    return Offset(
        x = screenCoordinate.x.toFloat(),
        y = screenCoordinate.y.toFloat() - yPadding
    )
}

private suspend fun loadStoriesForMap(mapInstanceId: Int) = withContext(Dispatchers.IO) {
    try {
        Log.d("MapScreen", "Loading stories for map instance $mapInstanceId")

        val cachedStories = AppComponents.storyCache.getCachedStories()
        if (cachedStories.isNotEmpty()) {
            Log.d("MapScreen", "Using ${cachedStories.size} cached stories")
            withContext(Dispatchers.Main) {
                AppComponents.mapStateManager.updateStories(cachedStories)
                AppComponents.mapStateManager.showPinsOnMap(mapInstanceId)
            }
        }

        AppComponents.firestore?.let { firestore ->
            firestore.collection("story")
                .orderBy("created_at", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener { snapshot ->
                    val fetchedStories = snapshot.documents.mapNotNull { it.toStoryData() }
                    Log.d("MapScreen", "Loaded ${fetchedStories.size} stories from Firestore")

                    AppComponents.appScope.launch {
                        AppComponents.storyCache.cacheStories(fetchedStories)

                        withContext(Dispatchers.Main) {
                            AppComponents.mapStateManager.updateStories(fetchedStories)
                            AppComponents.mapStateManager.refreshPins(mapInstanceId)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("MapScreen", "Failed to load stories from Firestore", e)
                }
        }
    } catch (e: Exception) {
        Log.e("MapScreen", "Failed to load stories", e)
    }
}