package com.storyspots.model
import java.util.Date

data class NotificationItem(
    val id: String,
    val createdAt: Date,
    val title: String,
    val message: String? = null,
    val read: Boolean,
    val fromUserId :String,
    val story: String,
    val imageUrl: String? = null
)

