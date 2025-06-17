package com.storyspots.notification

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import com.storyspots.model.NotificationItem
import kotlinx.coroutines.tasks.await

class NotificationSender {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun sendNewPostNotification(
        postTitle: String,
        postId: String,
        authorName: String,
        authorProfileImageUrl: String? = null
    ) {
        val currentUserId = auth.uid ?: return
        Log.d("NotificationSender", "Creating notification for postId=$postId by user=$currentUserId")

        try {
            val storyRef = db.collection("story").document(postId)

            val notification = NotificationItem(
                id = postId,
                title = postTitle,
                message = "New story from $authorName",
                created_at = Timestamp.now(),
                authorId = currentUserId,
                story = storyRef,
                imageUrl = authorProfileImageUrl
            )

            db.collection("notification")
                .document(notification.id)
                .set(notification.toMap())
                .await()

            Log.d("NotificationSender", "Notification created successfully for post $postId")

        } catch (e: Exception) {
            Log.e("NotificationSender", "Failed to create notification for post $postId", e)
        }
    }
}
