package com.storyspots.model

data class NotificationItem(
    val id: String = "",
    val userName: String,
    val message: String,
    val imageUrl: String = "",
)

