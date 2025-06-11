package com.storyspots.ui

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.storyspots.caption.DismissibleStoryStack
import com.storyspots.caption.StoryData
import com.storyspots.core.AppComponents
import com.storyspots.core.managers.LocationsManager
import com.storyspots.location.RecenterButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MapScreen() {
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var mapReady by remember { mutableStateOf(false) }
    var showStoryStack by remember { mutableStateOf(false) }
    var selectedStories by remember { mutableStateOf<List<StoryData>>(emptyList()) }
    var storyStackOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    val context = LocalContext.current

    // Load cached stories immediately
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val cachedStories = AppComponents.storyCache.getCachedStories()
            if (cachedStories.isNotEmpty()) {
                // Use cached data immediately
                withContext(Dispatchers.Main) {
                    updateMapWithStories(mapView, cachedStories)
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mapView?.onDestroy()
            mapView = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).also { map ->
                    mapView = map

                    // Basic map setup - minimal blocking
                    map.getMapboxMap().loadStyleUri(
                        Style.MAPBOX_STREETS,
                        { style ->
                            // Map is ready - do heavy initialization async
                            AppComponents.appScope.launch {
                                initializeMapAsync(map, style)
                                mapReady = true
                            }
                        }
                    )
                }
            }
        )

        // Recenter button
        if (mapReady) {
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

private fun BoxScope.RecenterButton(
    mapView: com.mapbox.maps.MapView?,
    locationManager: com.storyspots.core.managers.LocationsManager,
    modifier: androidx.compose.ui.Modifier
) {
}

private suspend fun initializeMapAsync(mapView: MapView, style: Style) = withContext(Dispatchers.Default) {
    try {
        withContext(Dispatchers.Main) {
            AppComponents.locationManager.setupLocationComponent(
                mapView = mapView,
                onLocationUpdate = { /* Handle updates */ },
                centerOnFirstUpdate = true
            )
        }

        AppComponents.mapManager.initializeMap(mapView)
        loadStoriesFromNetwork()

    } catch (e: Exception) {
        Log.e("MapScreen", "Map initialization failed", e)
    }
}

private suspend fun loadStoriesFromNetwork() = withContext(Dispatchers.IO) {
    try {
        AppComponents.firestore?.let { firestore ->
            // TODO: Load stories and update cache
        }
    } catch (e: Exception) {
        Log.e("MapScreen", "Failed to load stories", e)
    }
}

private fun updateMapWithStories(mapView: MapView?, stories: List<StoryData>) {
    // Update map with stories
    mapView?.let { map ->
        //TODO: Add pins, clusters, etc.
    }
}