package com.storyspots.notificationFeed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.storyspots.model.NotificationItem
import com.storyspots.notificationFeed.NotificationUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

class NotificationsViewModel(private val currentUserId: String) : ViewModel() {
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
        fetchNotificationsFromFirestore()
    }

    fun fetchNotificationsFromFirestore() {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                val db = FirebaseFirestore.getInstance()
                val notificationsRef = db.collection("notification")


                val querySnapshot = notificationsRef
                    .whereEqualTo("to", currentUserId)
                    .orderBy("created_at", Query.Direction.DESCENDING)
                    .get()
                    .await()

                // Map Firestore documents to NotificationItem
                val notifications = querySnapshot.documents.mapNotNull { doc ->
                    try {
                        val fromUserId = doc.getString("from") ?: return@mapNotNull null
                        val userDoc = db.collection("user").document(fromUserId.split("/").last()).get().await()
                        val userName = userDoc.getString("username") ?: "Unknown User"
                        val imageUrl = userDoc.getString("profileImageUrl") ?: "" // Adjust field name as per your DB
                        val storyId = doc.getString("story")?.split("/")?.last() ?: "a story"
                        val message = "$userName mentioned you in $storyId"

                        NotificationItem(
                            id = doc.id,
                            createdAt = doc.getTimestamp("created_at")?.toDate() ?: Date(),
                            from = doc.getString("from") ?: "",
                            read = doc.getBoolean("read") ?: false,
                            story = doc.getString("story") ?: "",
                            to = doc.getString("to") ?: ""
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }

                // Categorize notifications
                val (new, lastWeek, lastMonth) = NotificationUtils.categorizeNotifications(notifications)

                // Update state
                _newNotifications.value = new
                _lastWeekNotifications.value = lastWeek
                _lastMonthNotifications.value = lastMonth
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun markAsViewed(notificationId: String) {
        viewModelScope.launch {
            try {
                val db = FirebaseFirestore.getInstance()
                db.collection("notification")
                    .document(notificationId)
                    .update("read", true)
                    .await()
                fetchNotificationsFromFirestore()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}