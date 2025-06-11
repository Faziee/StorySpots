package com.storyspots.ui

import android.annotation.SuppressLint
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
import com.storyspots.cache.StoryCache
import com.storyspots.caption.DismissibleStoryStack
import com.storyspots.caption.MapLoader
import com.storyspots.caption.StoryData
import com.storyspots.caption.fetchAllStories
import com.storyspots.caption.toStoryData
import com.storyspots.core.AppComponents
import com.storyspots.core.managers.LocationsManager
import com.storyspots.location.RecenterButton
import com.storyspots.pin.SimpleClustering
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("Lifecycle")
@Composable
fun MapScreen() {
    // Use rememberSaveable to preserve state across navigation
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var mapReady by remember { mutableStateOf(false) }
    var showStoryStack by remember { mutableStateOf(false) }
    var selectedStories by remember { mutableStateOf<List<StoryData>>(emptyList()) }
    var storyStackOffset by remember { mutableStateOf(Offset.Zero) }
    var storiesLoaded by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Load stories when map is ready and stories haven't been loaded yet
    LaunchedEffect(mapReady) {
        if (mapReady && !storiesLoaded) {
            withContext(Dispatchers.IO) {
                val cachedStories = AppComponents.storyCache.getCachedStories()

                withContext(Dispatchers.Main) {
                    if (cachedStories.isNotEmpty()) {
                        Log.d("MapScreen", "Using ${cachedStories.size} cached stories")
                        updateMapWithStories(mapView, cachedStories)
                        storiesLoaded = true
                    }
                }

                // Always try to load fresh data
                loadStoriesFromNetwork(mapView) {
                    storiesLoaded = true
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val mapInitOptions = MapInitOptions(context = ctx)
                MapView(ctx, mapInitOptions).also { map ->
                    mapView = map
                    map.getMapboxMap().loadStyleUri("mapbox://styles/jordana-gc/cmad3b95m00oo01sdbs0r2rag") { style ->
                        AppComponents.appScope.launch {
                            if (!map.isDestroyed) {
                                initializeMapAsync(map, style) { storiesAtPin, offset ->
                                    selectedStories = storiesAtPin
                                    storyStackOffset = offset
                                    showStoryStack = true
                                }
                                mapReady = true
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
        withContext(Dispatchers.Main) {
            AppComponents.locationManager.setupLocationComponent(
                mapView = mapView,
                onLocationUpdate = { /* Handle updates */ },
                centerOnFirstUpdate = true
            )
        }

        // Initialize the map manager
        AppComponents.mapManager.initializeMap(mapView)

        // Initialize MapLoader with the mapView
        withContext(Dispatchers.Main) {
            MapLoader.initialize(mapView)

            // Set up the pin click listener
            MapLoader.setOnPinClickListener(onPinClick)

            // Load stories
            MapLoader.loadAllStories { allStories ->
                Log.d("MapScreen", "Loaded ${allStories.size} stories via MapLoader")
            }
        }

    } catch (e: Exception) {
        Log.e("MapScreen", "Map initialization failed", e)
    }
}

private suspend fun loadStoriesFromNetwork(mapView: MapView?, onComplete: () -> Unit = {}) = withContext(Dispatchers.IO) {
    try {
        AppComponents.firestore?.let { firestore ->
            // Get stories from Firestore
            firestore.collection("story")
                .orderBy("created_at", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener { snapshot ->
                    val fetchedStories = snapshot.documents.mapNotNull { it.toStoryData() }
                    Log.d("MapScreen", "Loaded ${fetchedStories.size} stories from Firestore")

                    // Cache the stories
                    AppComponents.appScope.launch {
                        AppComponents.storyCache.cacheStories(fetchedStories)

                        // Update map with stories - this will trigger pin updates via MapLoader
                        withContext(Dispatchers.Main) {
                            updateMapWithStories(mapView, fetchedStories)
                            onComplete()
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("MapScreen", "Failed to load stories from Firestore", e)
                    onComplete()
                }
        }
    } catch (e: Exception) {
        Log.e("MapScreen", "Failed to load stories", e)
        onComplete()
    }
}

private fun updateMapWithStories(mapView: MapView?, stories: List<StoryData>) {
    mapView?.let { map ->
        Log.d("MapScreen", "Updating map with ${stories.size} stories")

        // Clear existing pins
        SimpleClustering.clearPins()

        // Add story pins to map
        stories.forEach { story ->
            story.location?.let { geoPoint ->
                val point = Point.fromLngLat(geoPoint.longitude, geoPoint.latitude)
                SimpleClustering.addClusterPin(point)
                Log.d("MapScreen", "Added pin at: ${geoPoint.latitude}, ${geoPoint.longitude}")
            }
        }

        Log.d("MapScreen", "Map updated with ${stories.size} story pins")
    }
}