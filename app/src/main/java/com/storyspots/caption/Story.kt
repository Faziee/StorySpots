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
            userPath = userField as? String
        )
    } catch (e: Exception) {
        Log.e("StoryBox", "Error converting document to StoryData: ${e.message}")
        Log.e("StoryBox", "Document data: ${data}")
        null
    }
}

fun fetchAllStories(limit: Long = 50, onResult: (List<StoryData>) -> Unit): ListenerRegistration {
    val db = FirebaseFirestore.getInstance()
    Log.d("Story", "Starting to fetch stories from Firestore...")

    return db.collection("story")
        .orderBy("created_at", Query.Direction.DESCENDING)
        .limit(limit)
        .addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("StoryBox", "Real-time fetch failed", e)
                onResult(emptyList())
                return@addSnapshotListener
            }

            Log.d("Story", "Firestore returned ${snapshot?.size()} documents")
            val stories = snapshot?.documents?.mapNotNull { doc ->
                Log.d("Story", "Processing document: ${doc.id}")
                doc.toStoryData()?.also {
                    Log.d("Story", "Successfully converted: ${it.title} at ${it.location}")
                }
            } ?: emptyList()

            Log.d("Story", "Final stories list: ${stories.size} stories")
            onResult(stories)
        }
}