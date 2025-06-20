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