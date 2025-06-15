package com.storyspots.yourFeed

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.storyspots.caption.StoryData
import com.storyspots.caption.toStoryData
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class YourFeedRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    companion object {
        private const val TAG = "YourFeedRepository"
    }

    /**
     * Get current user's stories as a Flow that updates in real-time
     */
    fun getUserStoriesFlow(): Flow<List<StoryData>> = callbackFlow {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e(TAG, "No authenticated user found")
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val userPath = "/user/${currentUser.uid}"
        Log.d(TAG, "Fetching stories for user path: $userPath")

        val listenerRegistration = firestore.collection("story")
            .whereEqualTo("user", userPath)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error fetching user stories", error)
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    Log.e(TAG, "Snapshot is null")
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                Log.d(TAG, "Found ${snapshot.size()} documents")

                val stories = snapshot.documents
                    .mapNotNull { document ->
                        try {
                            document.toStoryData()?.also {
                                Log.d(TAG, "Successfully converted story: ${it.id}")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error converting document ${document.id}", e)
                            null
                        }
                    }
                    .sortedByDescending { it.createdAt?.toDate() }

                Log.d(TAG, "Sending ${stories.size} stories to flow")
                trySend(stories)
            }

        awaitClose {
            Log.d(TAG, "Removing Firestore listener")
            listenerRegistration.remove()
        }
    }

    /**
     * @param storyId The ID of the story to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteStory(storyId: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("User not authenticated"))

            Log.d(TAG, "Attempting to delete story: $storyId")

            val document = firestore.collection("story")
                .document(storyId)
                .get()
                .await()

            if (!document.exists()) {
                return Result.failure(Exception("Story not found"))
            }

            val userField = document.get("user")
            val expectedUserPath = "/user/${currentUser.uid}"

            if (userField != expectedUserPath) {
                Log.e(TAG, "Story does not belong to current user. Expected: $expectedUserPath, Got: $userField")
                return Result.failure(Exception("Unauthorized to delete this story"))
            }

            firestore.collection("story")
                .document(storyId)
                .delete()
                .await()

            Log.d(TAG, "Story deleted successfully: $storyId")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete story: $storyId", e)
            Result.failure(e)
        }
    }

    /**
     * Check if user is authenticated
     */
    fun isUserAuthenticated(): Boolean = auth.currentUser != null

    /**
     * Get current user ID
     */
    fun getCurrentUserId(): String? = auth.currentUser?.uid
}