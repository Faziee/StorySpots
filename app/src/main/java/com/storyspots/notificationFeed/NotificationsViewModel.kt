package com.storyspots.notificationFeed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storyspots.model.NotificationItem
import com.storyspots.testSample.SampleNotificationData
import com.storyspots.notificationFeed.NotificationUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotificationsViewModel : ViewModel() {
    // State holders for different notification categories
    private val _newNotifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val newNotifications: StateFlow<List<NotificationItem>> = _newNotifications.asStateFlow()

    private val _lastWeekNotifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val lastWeekNotifications: StateFlow<List<NotificationItem>> = _lastWeekNotifications.asStateFlow()

    private val _lastMonthNotifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val lastMonthNotifications: StateFlow<List<NotificationItem>> = _lastMonthNotifications.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                // In a real app, you would fetch from Firestore here
                val allNotifications = SampleNotificationData.allNotifications

                // Categorize notifications
                val (new, lastWeek, lastMonth) = NotificationUtils.categorizeNotifications(allNotifications)

                // Update state
                _newNotifications.value = new
                _lastWeekNotifications.value = lastWeek
                _lastMonthNotifications.value = lastMonth
            } finally {
                _isLoading.value = false
            }
        }
    }

    // This will be used when integrating with Firestore
    fun fetchNotificationsFromFirestore() {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                // TODO: Implement Firestore fetching
                // Example pseudocode:
                // val notificationsRef = FirebaseFirestore.getInstance().collection("notifications")
                // val query = notificationsRef.whereEqualTo("userId", currentUserId)
                // val notifications = query.get().await().toObjects(NotificationItem::class.java)

                // For now, use sample data
                loadNotifications()
            } catch (e: Exception) {
                // Handle error
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Handle notification viewed
    fun markAsViewed(notificationId: String) {
        // TODO: Implement marking notification as viewed in Firestore
        // For now, just reload the notifications
        loadNotifications()
    }
}