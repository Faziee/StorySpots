package com.storyspots.notification

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class NotificationCleanupService {
    private val db = FirebaseFirestore.getInstance()

    suspend fun deleteNotificationsForStory(storyId: String) {
        try {
            val storyRef = db.collection("story").document(storyId)

            val notificationsQuery = db.collection("notification")
                .whereEqualTo("story", storyRef)
                .get()
                .await()

            val batch = db.batch()
            for (doc in notificationsQuery.documents) {
                batch.delete(doc.reference)
            }

            batch.commit().await()

            println("Deleted ${notificationsQuery.documents.size} notifications for story: $storyId")
        } catch (e: Exception) {
            println("Error deleting notifications for story $storyId: ${e.message}")
            throw e
        }
    }
}