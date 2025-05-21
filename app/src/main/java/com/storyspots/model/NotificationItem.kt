package com.storyspots.model
import java.util.Date

data class NotificationItem(
    val id: String,
    val createdAt: Date,
    val from: String,
    val fromUserId :String,
    val read: Boolean,
    val story: String,
    val to: String,
    val imageUrl: String? = null,
    val message: String? = null
)

