package com.storyspots.caption

import android.util.Log
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.storyspots.caption.model.UserData
import kotlinx.coroutines.tasks.await

class StoryRepository
{
    suspend fun fetchUserData(userRef: DocumentReference): UserData? {
        return try {
            val userDoc = FirebaseFirestore.getInstance()
                .collection("users")
                .document(userRef.id)
                .get()
                .await()
            Log.d("UserData", "Fetched user data: $userDoc")

            UserData(
                id = userDoc.id,
                username = userDoc.getString("username") ?: "Deleted User",
                profileImageUrl = userDoc.getString("profile_picture_url")
                    ?: userDoc.getString("profileImageUrl")
                    ?: userDoc.getString("profile_picture")
                    ?: ""
            )
        } catch (e: Exception) {
            android.util.Log.e("UserData", "Error fetching user data", e)
            null
        }
    }
}