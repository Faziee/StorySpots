package com.storyspots.core

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.storyspots.cache.StoryCache
import com.storyspots.core.managers.*
import com.storyspots.services.cloudinary.CloudinaryService
import com.storyspots.caption.toStoryData
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

    // Map state manager for persistent pin management
    val mapStateManager = MapStateManager

    val storyCache: StoryCache by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        StoryCache(StorySpot.instance)
    }

    // Services - lazy initialized singleton to prevent multiple initializations
    val cloudinaryService: CloudinaryService by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        CloudinaryService(StorySpot.instance)
    }

    val auth: FirebaseAuth?
        get() = if (StorySpot.isFirebaseReady) FirebaseAuth.getInstance() else null

    val firestore: FirebaseFirestore?
        get() = if (StorySpot.isFirebaseReady) FirebaseFirestore.getInstance() else null

    fun cleanup() {
        appScope.cancel()
    }

    // Helper method to refresh stories globally
    fun refreshStories() {
        appScope.launch {
            try {
                firestore?.let { db ->
                    db.collection("story")
                        .orderBy("created_at", com.google.firebase.firestore.Query.Direction.DESCENDING)
                        .limit(50)
                        .get()
                        .addOnSuccessListener { snapshot ->
                            val stories = snapshot.documents.mapNotNull { doc ->
                                try {
                                    doc.toStoryData()
                                } catch (e: Exception) {
                                    Log.e("AppComponents", "Error converting document", e)
                                    null
                                }
                            }

                            appScope.launch {
                                storyCache.cacheStories(stories)
                                mapStateManager.updateStories(stories)
                                mapStateManager.refreshPins(System.currentTimeMillis().toInt())

                                Log.d("AppComponents", "Stories refreshed: ${stories.size} stories")
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("AppComponents", "Failed to refresh stories", e)
                        }
                }
            } catch (e: Exception) {
                Log.e("AppComponents", "Error refreshing stories", e)
            }
        }
    }
}