package com.storyspots.settings

import android.util.Log
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun SettingsScreen(){
    Button(onClick = { sendTestNotification() }) {
        Text("Send Test Notification")
    }
}
    fun sendTestNotification() {
        val userId = FirebaseAuth.getInstance().uid.orEmpty()
        val db = FirebaseFirestore.getInstance()

        val fromRef = db.collection("user").document(userId)
        val storyId = "test_story_123"
        val storyRef = db.collection("story").document(storyId)

        val notificationId = db.collection("notification").document().id

        val testNotification = hashMapOf(
            "id" to notificationId,
            "title" to "Dev Test",
            "message" to "This is a test from button",
            "created_at" to Timestamp.now(),
            "read" to false,
            "from" to fromRef,
            "story" to storyRef
        )

        db.collection("notification").document(notificationId)
            .set(testNotification)
            .addOnSuccessListener { Log.d("SettingsScreen", "Test notification sent.") }
            .addOnFailureListener { e -> Log.e("SettingsScreen", "Failed to send test notification", e) }

    }