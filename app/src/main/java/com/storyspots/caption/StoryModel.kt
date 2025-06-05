package com.storyspots.caption

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.GeoPoint

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