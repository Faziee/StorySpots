package com.storyspots.pushNotification

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.storyspots.R

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class FirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage){
        val title = remoteMessage.notification?.title ?: "No Title"
        val body = remoteMessage.notification?.body ?: "No body"

        saveNotificationToFirestore(title, body)

        remoteMessage.notification?.let {
            showNotification(it.title, it.body)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token: $token")
    }

    private fun showNotification(title: String?, body: String?){
        val channelId ="default_channel_id"
        val notificationBuilder = NotificationCompat.Builder(this, channelId).setContentTitle(title).setContentText(body).setSmallIcon(R.drawable.ic_notifications).setAutoCancel(true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val channel = NotificationChannel(channelId, "Default Channel", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0, notificationBuilder.build())
    }

    private fun saveNotificationToFirestore(title: String, body: String){
        val notificationDoc = hashMapOf(

            "title" to title,
            "body" to body,
            "timestamp" to Timestamp.now(),
            "uid" to FirebaseAuth.getInstance().uid,
            "read" to false
        )

        FirebaseFirestore.getInstance().collection("notification").add(notificationDoc).addOnFailureListener { e -> Log.e("FCM", "Failed to save notification", e)
        }
    }

}