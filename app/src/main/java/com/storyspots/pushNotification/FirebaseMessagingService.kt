package com.storyspots.pushNotification

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.storyspots.R

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class FirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val title = remoteMessage.notification?.title ?: "New Notification!"
        val message = remoteMessage.notification?.body ?: ""
        val data = remoteMessage.data
        val storyId = data["storyId"]
        val fromUserId = data["fromUserId"]?: FirebaseAuth.getInstance().uid.orEmpty()
        val imageUrl = data["imageUrl"]

        saveNotificationToFirestore(title, message, storyId.toString(), fromUserId, imageUrl)

        showNotification(title, message)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token: $token")
    }

    private fun showNotification(title: String?, message: String?){
        val channelId ="default_channel_id"
        val notificationBuilder = NotificationCompat.Builder(this, channelId).setContentTitle(title).setContentText(message).setSmallIcon(R.drawable.ic_notification).setAutoCancel(true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val channel = NotificationChannel(channelId, "Default Channel", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0, notificationBuilder.build())
    }

    private fun saveNotificationToFirestore(
        title: String,
        message: String?,
        storyId: String,
        fromUserId: String,
        imageUrl: String?
    ) {
        val db = FirebaseFirestore.getInstance()

        val storyRef = db.collection("story").document(storyId)
        val fromRef = db.collection("user").document(fromUserId)

        val notificationDoc = hashMapOf(
            "id" to storyId,
            "title" to title,
            "message" to message,
            "created_at" to Timestamp.now(),
            "read" to false,
            "fromUserId" to fromUserId,
            "story" to storyRef,
            "from" to fromRef,
            "imageUrl" to imageUrl
        )

        db.collection("notification")
            .add(notificationDoc)
            .addOnSuccessListener {
                Log.d("FCM", "Notification stored successfully")
            }
            .addOnFailureListener {
                Log.e("FCM", "Failed to store notification", it)
            }
    }
}