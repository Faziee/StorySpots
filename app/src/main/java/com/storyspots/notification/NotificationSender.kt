package com.storyspots.notification

import android.content.Context
import android.util.Log
import android.Manifest
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import com.storyspots.model.NotificationItem
import com.storyspots.utils.Constants
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import org.json.JSONArray
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.storyspots.MainActivity
import com.storyspots.R

class NotificationSender (private val context: Context){
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val client = OkHttpClient()
    
    private val oneSignalAppId = Constants.ONESIGNAL_APP_ID
    private val oneSignalRestApiKey = Constants.ONESIGNAL_REST_API_KEY

    suspend fun sendNewPostNotification(
        postTitle: String,
        postId: String,
        authorName: String,
        authorProfileImageUrl: String? = null
    ) {
        val currentUserId = auth.uid ?: return
        Log.d("NotificationSender", "Creating notification for postId=$postId by user=$currentUserId")

        try {
            // 1. Save notification to Firestore (for in-app notification feed)
            saveNotificationToFirestore(postTitle, postId, authorName, currentUserId, authorProfileImageUrl)

            // 2. Send push notification via OneSignal
            sendOneSignalPush(postTitle, authorName, currentUserId)

        } catch (e: Exception) {
            Log.e("NotificationSender", "Failed to send notification for post $postId", e)
        }
    }

    private suspend fun saveNotificationToFirestore(
        postTitle: String,
        postId: String,
        authorName: String,
        currentUserId: String,
        authorProfileImageUrl: String?
    ) {
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

        Log.d("NotificationSender", "Notification saved to Firestore for post $postId")
    }

    private suspend fun sendOneSignalPush(
        postTitle: String,
        authorName: String,
        excludeUserId: String
    ) = withContext(Dispatchers.IO) {
        try {
            val jsonData = JSONObject().apply {
                put("app_id", oneSignalAppId)
                put("included_segments", JSONArray().put("Subscribed Users"))

                // Exclude the author from receiving their own notification
                val filters = JSONArray().apply {
                    put(JSONObject().apply {
                        put("field", "tag")
                        put("key", "user_id")
                        put("relation", "!=")
                        put("value", excludeUserId)
                    })
                }
                put("filters", filters)

                put("headings", JSONObject().put("en", "New Story Posted! 📖"))
                put("contents", JSONObject().put("en", "$authorName posted: \"$postTitle\""))

                // Add custom data for handling notification clicks
                put("data", JSONObject().apply {
                    put("type", "new_story")
                    put("story_title", postTitle)
                    put("author_name", authorName)
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonData.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url("https://onesignal.com/api/v1/notifications")
                .header("Authorization", "Basic $oneSignalRestApiKey")
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful) {
                Log.d("NotificationSender", "OneSignal push sent successfully: $responseBody")
            } else {
                Log.e("NotificationSender", "OneSignal push failed: ${response.code} - $responseBody")
            }

        } catch (e: Exception) {
            Log.e("NotificationSender", "Failed to send OneSignal push", e)
        }
    }

    suspend fun sendProximityNotification(
        postTitle: String,
        postId: String,
        authorName: String,
        distance: Int, // distance in meters
        authorProfileImageUrl: String? = null
    ) {
        val currentUserId = auth.uid ?: return
        Log.d("NotificationSender", "Sending proximity notification for postId=$postId, distance=${distance}m")

        try {
            // 1. Save notification to Firestore (for in-app notification feed)
            saveProximityNotificationToFirestore(postTitle, postId, authorName, currentUserId, distance, authorProfileImageUrl)

            // 2. Send local notification (no OneSignal needed for proximity)
            sendLocalProximityNotification(postTitle, authorName, distance, postId)

        } catch (e: Exception) {
            Log.e("NotificationSender", "Failed to send proximity notification for post $postId", e)
        }
    }

    private suspend fun saveProximityNotificationToFirestore(
        postTitle: String,
        postId: String,
        authorName: String,
        currentUserId: String,
        distance: Int,
        authorProfileImageUrl: String?
    ) {
        val storyRef = db.collection("story").document(postId)

        val notification = NotificationItem(
            id = "${postId}_proximity_${System.currentTimeMillis()}", // Unique ID for proximity notifications
            title = "Story nearby!",
            message = "\"$postTitle\" by $authorName is ${distance}m away",
            created_at = Timestamp.now(),
            authorId = currentUserId,
            story = storyRef,
            imageUrl = authorProfileImageUrl
        )

        db.collection("notification")
            .document(notification.id)
            .set(notification.toMap())
            .await()

        Log.d("NotificationSender", "Proximity notification saved to Firestore for post $postId")
    }

    private fun sendLocalProximityNotification(
        postTitle: String,
        authorName: String,
        distance: Int,
        postId: String
    ) {
        // 🔐 Check for notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w("NotificationSender", "Notification permission not granted")
                return
            }
        }

        val notificationManager = NotificationManagerCompat.from(context)

        createNotificationChannel()

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("story_id", postId)
            putExtra("notification_type", "proximity")
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            postId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.pin_marker)
            .setContentTitle("Story nearby! 📍")
            .setContentText("\"$postTitle\" by $authorName is ${distance}m away")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("\"$postTitle\" by $authorName is ${distance}m away. Tap to explore!")
            )
            .build()

        notificationManager.notify(postId.hashCode(), notification)
        Log.d("NotificationSender", "Local proximity notification sent for post $postId")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Story Proximity",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications when you're near a story"
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "story_proximity_channel"
    }
}