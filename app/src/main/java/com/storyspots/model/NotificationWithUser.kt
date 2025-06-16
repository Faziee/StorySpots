package com.storyspots.model

data class NotificationWithUser(
    val notification: NotificationItem,
    val username: String,
    val profileImageUrl: String? = null
)