package com.storyspots.model

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.Timestamp

data class NotificationItem(
    val id: String,
    val created_at: Timestamp,
    val title: String,
    val message: String? = null,
    val authorId: String,
    val story: DocumentReference,
    val imageUrl: String? = null
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "title" to title,
            "message" to message,
            "created_at" to created_at,
            "authorId" to authorId,
            "story" to story,
            "imageUrl" to imageUrl
        )
    }
}
