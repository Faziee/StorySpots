package com.storyspots.model

import com.google.firebase.Timestamp

data class NotificationItem(
    val id: String = "",
    val userName: String,
    val message: String,
    val imageUrl: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

