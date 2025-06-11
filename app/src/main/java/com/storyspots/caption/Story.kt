package com.storyspots.caption

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

fun DocumentSnapshot.toStoryData(): StoryData? {
    return try {
        val userField = get("user")
        val authorRef: DocumentReference? = when (userField) {
            is DocumentReference -> userField
            is String -> {
                if (userField.startsWith("/user/")) {
                    val userId = userField.removePrefix("/user/")
                    FirebaseFirestore.getInstance().collection("users").document(userId)
                } else {
                    null
                }
            }
            else -> null
        }

        StoryData(
            id = id,
            title = getString("title") ?: "Untitled",
            createdAt = getTimestamp("created_at"),
            location = getGeoPoint("location"),
            caption = getString("caption"),
            imageUrl = getString("image_url"),
            mapRef = getDocumentReference("map"),
            authorRef = authorRef,
            userPath = userField as? String // Store the original string path too
        )
    } catch (e: Exception) {
        Log.e("StoryBox", "Error converting document to StoryData: ${e.message}")
        Log.e("StoryBox", "Document data: ${data}")
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

fun fetchStoriesByMap(mapId: String, limit: Long = 50, onResult: (List<StoryData>) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val mapRef = db.collection("map").document(mapId)

    db.collection("story")
        .whereEqualTo("map", mapRef)
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

fun fetchStoriesByGeoPoint(mapId: String, geoPoint: GeoPoint, limit: Long = 50, onResult: (List<StoryData>) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val mapRef = db.collection("map").document(mapId)

    db.collection("story")
        .whereEqualTo("map", mapRef)
        .whereEqualTo("location_lat", geoPoint.latitude)
        .whereEqualTo("location_lng", geoPoint.longitude)
        .limit(limit)
        .addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("StoryBox", "Real-time fetch by geopoint failed", e)
                onResult(emptyList())
                return@addSnapshotListener
            }

            val stories = snapshot?.documents?.mapNotNull { it.toStoryData() } ?: emptyList()
            onResult(stories)
        }
}