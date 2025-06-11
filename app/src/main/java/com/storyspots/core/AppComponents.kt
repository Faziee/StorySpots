package com.storyspots.core

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.storyspots.cache.StoryCache
import com.storyspots.core.managers.*
import com.storyspots.services.cloudinary.CloudinaryService
import kotlinx.coroutines.*
import kotlin.getValue

object AppComponents {
    // Coroutine scope for app-wide async operations
    val appScope = CoroutineScope(
        Dispatchers.Main +
                SupervisorJob() +
                CoroutineExceptionHandler { _, throwable ->
                    Log.e("AppComponents", "Coroutine exception", throwable)
                }
    )

    // Core managers - lazy loaded for performance
    val locationManager: LocationsManager by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        LocationsManager(StorySpot.instance)
    }

    val navigationManager: CoreNavigationManager by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        CoreNavigationManager()
    }

    val imageSelectionManager: ImageSelectionManager by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        ImageSelectionManager()
    }

    val permissionManager: PermissionManager by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        PermissionManager()
    }

    val mapManager: MapManager by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        MapManager()
    }

    val storyCache: StoryCache by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        StoryCache(StorySpot.instance)
    }

    // Services - only created when needed
    val cloudinaryService: CloudinaryService
        get() = CloudinaryService(StorySpot.instance)

    // Firebase instances - wait for initialization
    val auth: FirebaseAuth?
        get() = if (StorySpot.isFirebaseReady) FirebaseAuth.getInstance() else null

    val firestore: FirebaseFirestore?
        get() = if (StorySpot.isFirebaseReady) FirebaseFirestore.getInstance() else null

    // Cleanup method
    fun cleanup() {
        appScope.cancel()
    }
}