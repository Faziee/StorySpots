package com.storyspots.testSample

import com.storyspots.model.NotificationItem
import java.util.concurrent.TimeUnit

/**
 * Sample data provider for notifications
 * This will be replaced with Firestore implementation later
 */
object SampleNotificationData {
    // Sample profile images
    private val profileImages = listOf(
        "https://randomuser.me/api/portraits/men/32.jpg",
        "https://randomuser.me/api/portraits/women/44.jpg",
        "https://randomuser.me/api/portraits/women/68.jpg"
    )

    // Create sample notifications
    val allNotifications = listOf(
        // New notifications (within last 24 hours)
        NotificationItem(
            id = "1",
            userName = "Motongo Rada",
            message = "Motongo saved your story to his private map",
            imageUrl = profileImages[0],
            timestamp = System.currentTimeMillis()
        ),
        NotificationItem(
            id = "2",
            userName = "Diệp Đình Nguyễn",
            message = "Diệp saved your story to her private map",
            imageUrl = profileImages[1],
            timestamp = System.currentTimeMillis()
        ),
        NotificationItem(
            id = "3",
            userName = "Sara Ntongnya",
            message = "Sara saved your story to her private map",
            imageUrl = profileImages[2],
            timestamp = System.currentTimeMillis()
        ),

        // Last 7 days notifications
        NotificationItem(
            id = "4",
            userName = "Motongo Rada",
            message = "Motongo saved your story to his private map",
            imageUrl = profileImages[0],
            timestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(5)
        ),
        NotificationItem(
            id = "5",
            userName = "Diệp Đình Nguyễn",
            message = "Diệp saved your story to her private map",
            imageUrl = profileImages[1],
            timestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(6)
        ),
        NotificationItem(
            id = "6",
            userName = "Sara Ntongnya",
            message = "Sara saved your story to her private map",
            imageUrl = profileImages[2],
            timestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(4)
        ),

        // Last 30 days notifications
        NotificationItem(
            id = "7",
            userName = "Motongo Rada",
            message = "Motongo saved your story to his private map",
            imageUrl = profileImages[0],
            timestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(20)
        ),
        NotificationItem(
            id = "8",
            userName = "Diệp Đình Nguyễn",
            message = "Diệp saved your story to her private map",
            imageUrl = profileImages[1],
            timestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(25)
        ),
        NotificationItem(
            id = "9",
            userName = "Sara Ntongnya",
            message = "Sara saved your story to her private map",
            imageUrl = profileImages[2],
            timestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(22)
        )
    )
}