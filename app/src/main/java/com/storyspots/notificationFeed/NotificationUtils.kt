package com.storyspots.notificationFeed

import com.storyspots.model.NotificationWithUser
import java.util.concurrent.TimeUnit

object NotificationUtils {

    fun categorizeNotifications(
        notifications: List<NotificationWithUser>
    ): Triple<List<NotificationWithUser>, List<NotificationWithUser>, List<NotificationWithUser>> {

        val currentTime = System.currentTimeMillis()
        val oneDayAgo = currentTime - TimeUnit.DAYS.toMillis(1)
        val sevenDaysAgo = currentTime - TimeUnit.DAYS.toMillis(7)
        val thirtyDaysAgo = currentTime - TimeUnit.DAYS.toMillis(30)

        val newNotifications = notifications.filter {
            it.notification.created_at.toDate().time > oneDayAgo
        }

        val lastWeekNotifications = notifications.filter {
            val time = it.notification.created_at.toDate().time
            time <= oneDayAgo && time > sevenDaysAgo
        }

        val lastMonthNotifications = notifications.filter {
            val time = it.notification.created_at.toDate().time
            time <= sevenDaysAgo && time > thirtyDaysAgo
        }

        return Triple(newNotifications, lastWeekNotifications, lastMonthNotifications)
    }
}
