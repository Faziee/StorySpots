package com.storyspots.caption

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.mapbox.maps.CoordinateBounds
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.toCameraOptions
import kotlin.math.abs

class Story {
    fun DocumentSnapshot.toStoryData(): StoryData? {
        return try {
            StoryData(
                id = id,
                title = getString("title") ?: "Untitled",
                createdAt = getTimestamp("created_at"),
                location = getGeoPoint("location"),
                caption = getString("caption"),
                imageUrl = getString("image_url"),
                mapRef = getDocumentReference("map"),
                authorRef = getDocumentReference("user")
            )
        } catch (e: Exception) {
            Log.e("StoryBox", "Error converting document to StoryData", e)
            null
        }
    }

    fun fetchAllStories(limit: Long = 50, onResult: (List<StoryData>) -> Unit): ListenerRegistration {
        val db = FirebaseFirestore.getInstance()

        return db.collection("story")
            .orderBy("created_at", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("StoryBox", "Real-time fetch failed", e)
                    onResult(emptyList())
                    return@addSnapshotListener
                }

                val stories = snapshot?.documents?.mapNotNull { it.toStoryData() } ?: emptyList()
                onResult(stories)
            }
    }

    fun fetchStoriesByLatitudeRange(
        bounds: CoordinateBounds,
        onResult: (List<StoryData>) -> Unit,
        onError: (Exception) -> Unit = {}
    ): ListenerRegistration {
        val db = FirebaseFirestore.getInstance()
        val south = bounds.southwest.latitude()
        val north = bounds.northeast.latitude()

        return db.collection("story")
            .whereGreaterThanOrEqualTo("location", GeoPoint(south, -180.0))
            .whereLessThanOrEqualTo("location", GeoPoint(north, 180.0))
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("StoryBox", "Latitude query failed", e)
                    onError(e)
                    return@addSnapshotListener
                }

                val roughStories = snapshot?.documents?.mapNotNull { it.toStoryData() } ?: emptyList()
                onResult(roughStories)
            }
    }

    fun filterStoriesByLongitude(
        stories: List<StoryData>,
        bounds: CoordinateBounds
    ): List<StoryData> {
        return stories.filter { story ->
            story.location?.let { geo ->
                geo.latitude >= bounds.southwest.latitude() &&
                        geo.latitude <= bounds.northeast.latitude() &&
                        geo.longitude >= bounds.southwest.longitude() &&
                        geo.longitude <= bounds.northeast.longitude()
            } ?: false
        }
    }

    private var cachedBounds: CoordinateBounds? = null
    private var cachedStories: List<StoryData> = emptyList()

    fun shouldUseCache(currentBounds: CoordinateBounds): Boolean {
        val bounds = cachedBounds ?: return false
        val threshold = 0.01
        return abs(bounds.center().latitude() - currentBounds.center().latitude()) < threshold &&
                abs(bounds.center().longitude() - currentBounds.center().longitude()) < threshold
    }

    fun loadStoriesForCurrentView(mapboxMap: MapboxMap) {
        val currentBounds: CoordinateBounds = mapboxMap.coordinateBoundsForCamera(mapboxMap.cameraState.toCameraOptions())

        if (shouldUseCache(currentBounds)) {
            updateMapWithStories(cachedStories)
            return
        }

        fetchStoriesByLatitudeRange(currentBounds, { roughStories ->
            val filteredStories = filterStoriesByLongitude(roughStories, currentBounds)
            cachedStories = filteredStories
            cachedBounds = currentBounds
            updateMapWithStories(filteredStories)
        }, { error ->
            Log.e("MapLoader", "Failed to load stories", error)
        })
    }

    fun updateMapWithStories(stories: List<StoryData>) {
        // Your logic here to add pins, markers, etc.
    }
}
