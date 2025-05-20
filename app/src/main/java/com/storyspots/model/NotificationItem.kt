package com.storyspots.model

import com.google.firebase.Timestamp
import java.util.Date

data class NotificationItem(
    val id: String,
    val createdAt: Date,
    val from: String,
    val read: Boolean,
    val story: String,
    val to: String,
    val imageUrl: String? = null,
    val message: String? = null
)

