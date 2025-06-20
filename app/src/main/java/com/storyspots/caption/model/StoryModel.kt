package com.storyspots.caption.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.serialization.Serializable
import java.io.Serializable as JavaSerializable

data class StoryData(
    val id: String,
    val title: String,
    val createdAt: Timestamp?,
    val location: GeoPoint?,
    val caption: String?,
    val imageUrl: String?,
    val mapRef: DocumentReference?,
    val authorRef: DocumentReference?,
    val userPath: String?
) {
    fun toCached(): CachedStoryData = CachedStoryData(
        id = id,
        title = title,
        createdAt = createdAt?.seconds,
        location = location?.let { Pair(it.latitude, it.longitude) },
        caption = caption,
        imageUrl = imageUrl,
        mapRefPath = mapRef?.path,
        authorRefPath = authorRef?.path,
        userPath = userPath
    )
}

@Serializable
data class CachedStoryData(
    val id: String,
    val title: String,
    val createdAt: Long?,
    val location: Pair<Double, Double>?,
    val caption: String?,
    val imageUrl: String?,
    val mapRefPath: String?,
    val authorRefPath: String?,
    val userPath: String?
) : JavaSerializable {

    companion object {
        private const val serialVersionUID: Long = 1L
    }

    fun toStoryData(): StoryData = StoryData(
        id = id,
        title = title,
        createdAt = createdAt?.let { Timestamp(it, 0) },
        location = location?.let { GeoPoint(it.first, it.second) },
        caption = caption,
        imageUrl = imageUrl,
        mapRef = mapRefPath?.let { FirebaseFirestore.getInstance().document(it) },
        authorRef = authorRefPath?.let { FirebaseFirestore.getInstance().document(it) },
        userPath = userPath
    )
}