package com.storyspots.caption

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.ranges.contains

class StoryUtils {
    fun formatRelativeTime(timestamp: com.google.firebase.Timestamp): String {
        val now = Date()
        val date = timestamp.toDate()
        val diffInMillis = now.time - date.time

        val seconds = TimeUnit.MILLISECONDS.toSeconds(diffInMillis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis)
        val hours = TimeUnit.MILLISECONDS.toHours(diffInMillis)
        val days = TimeUnit.MILLISECONDS.toDays(diffInMillis)

        return when {
            seconds < 60 -> "Just now"
            minutes < 60 -> "$minutes minute${if (minutes != 1L) "s" else ""} ago"
            hours < 24 -> "$hours hour${if (hours != 1L) "s" else ""} ago"
            days == 1L -> "Yesterday"
            days in 2..6 -> "$days days ago"
            days in 7..13 -> "Last week"
            else -> {
                val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                sdf.format(date)
            }
        }
    }
}