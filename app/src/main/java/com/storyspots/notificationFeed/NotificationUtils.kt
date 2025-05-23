package com.storyspots.notificationFeed

import com.storyspots.model.NotificationItem
import java.util.concurrent.TimeUnit

object NotificationUtils {

    /**
     * Groups notifications into different time periods
     * @return Triple of (new, last7Days, last30Days) notifications
     */
    fun categorizeNotifications(notifications: List<NotificationItem>): Triple<List<NotificationItem>, List<NotificationItem>, List<NotificationItem>> {
        val currentTime = System.currentTimeMillis()
        val oneDayAgo = currentTime - TimeUnit.DAYS.toMillis(1)
        val sevenDaysAgo = currentTime - TimeUnit.DAYS.toMillis(7)
        val thirtyDaysAgo = currentTime - TimeUnit.DAYS.toMillis(30)

        val newNotifications = notifications.filter { it.createdAt.time > oneDayAgo }
        val lastWeekNotifications = notifications.filter {
            it.createdAt.time <= oneDayAgo && it.createdAt.time > sevenDaysAgo
        }
        val lastMonthNotifications = notifications.filter {
            it.createdAt.time <= sevenDaysAgo && it.createdAt.time > thirtyDaysAgo
        }

        return Triple(newNotifications, lastWeekNotifications, lastMonthNotifications)
    }
}