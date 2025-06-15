package com.storyspots.notification

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await

class NotificationSender {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun sendNewPostNotification(
        postTitle: String,
        postId: String,
        authorName: String
    ) {
        val currentUserId = auth.uid ?: return
        Log.d("NOTIF", "Creating single notification for postId=$postId by user=$currentUserId")

        try {
            val notificationId = postId

            val notification = hashMapOf(
                "id" to notificationId,
                "title" to postTitle,
                "message" to "New story from $authorName",
                "created_at" to Timestamp.now(),
                "readBy" to listOf<String>(),
                "authorId" to currentUserId,
                "story" to db.collection("story").document(postId)
            )

            db.collection("notification")
                .document(notificationId)
                .set(notification)
                .await()

            Log.d("NOTIF", "Notification created for post $postId")

        } catch (e: Exception) {
            Log.e("NOTIF", "Failed to create notification", e)
        }
    }

}
