package com.storyspots.caption

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.tasks.await

data class StoryData(
    val id: String,
    val title: String,
    val createdAt: Timestamp?,
    val location: GeoPoint?,
    val caption: String?,
    val imageUrl: String?,
    val mapRef: DocumentReference?,
    val authorRef: DocumentReference?
)

suspend fun fetchAllStories(): List<StoryData> {
    val db = FirebaseFirestore.getInstance()
    return try {
        val snapshot = db.collection("story").get().await()
        snapshot.documents.mapNotNull { doc ->
            StoryData(
                id = doc.id,
                title = doc.getString("title") ?: "Untitled",
                createdAt = doc.getTimestamp("created_at"),
                location = doc.getGeoPoint("location"),
                caption = doc.getString("caption"),
                imageUrl = doc.getString("image_url"),
                mapRef = doc.getDocumentReference("map"),
                authorRef = doc.getDocumentReference("user")
            )
        }
    } catch (e: Exception) {
        emptyList()
    }
}