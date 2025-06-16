package com.storyspots.notificationFeed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import com.storyspots.model.NotificationItem
import com.storyspots.model.NotificationWithUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class NotificationsViewModel : ViewModel() {
    private val _newNotifications = MutableStateFlow<List<NotificationWithUser>>(emptyList())
    val newNotifications: StateFlow<List<NotificationWithUser>> = _newNotifications.asStateFlow()

    private val _lastWeekNotifications = MutableStateFlow<List<NotificationWithUser>>(emptyList())
    val lastWeekNotifications: StateFlow<List<NotificationWithUser>> = _lastWeekNotifications.asStateFlow()

    private val _lastMonthNotifications = MutableStateFlow<List<NotificationWithUser>>(emptyList())
    val lastMonthNotifications: StateFlow<List<NotificationWithUser>> = _lastMonthNotifications.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        fetchNotificationsFromFirestore()
    }

    fun fetchNotificationsFromFirestore() {
        viewModelScope.launch {
            _isLoading.value = true
            val db = FirebaseFirestore.getInstance()

            try {
                val querySnapshot = db.collection("notification")
                    .orderBy("created_at", Query.Direction.DESCENDING)
                    .get()
                    .await()

                val enrichedNotifications = mutableListOf<NotificationWithUser>()

                for (doc in querySnapshot.documents) {
                    try {
                        val notificationId = doc.id
                        val createdAt = doc.getTimestamp("created_at") ?: Timestamp.now()
                        val title = doc.getString("title") ?: "Notification"
                        val message = doc.getString("message")
                        val authorId = doc.getString("authorId") ?: continue
                        val storyRef = doc.getDocumentReference("story") ?: continue
                        val imageUrl = doc.getString("imageUrl")

                        val baseNotification = NotificationItem(
                            id = notificationId,
                            created_at = createdAt,
                            title = title,
                            message = message,
                            authorId = authorId,
                            story = storyRef,
                            imageUrl = imageUrl
                        )

                        // Fetch the author (user) data
                        val userDoc = db.collection("users").document(authorId).get().await()
                        val username = userDoc.getString("username") ?: "Unknown"
                        val profileImageUrl = userDoc.getString("profileImageUrl")

                        enrichedNotifications.add(
                            NotificationWithUser(
                                notification = baseNotification,
                                username = username,
                                profileImageUrl = profileImageUrl
                            )
                        )
                    } catch (e: Exception) {
                        println("Error processing notification ${doc.id}: ${e.message}")
                    }
                }

                val (new, lastWeek, lastMonth) = NotificationUtils.categorizeNotifications(enrichedNotifications)

                _newNotifications.value = new
                _lastWeekNotifications.value = lastWeek
                _lastMonthNotifications.value = lastMonth

            } catch (e: Exception) {
                println("Firestore fetch error: ${e.message}")
                _newNotifications.value = emptyList()
                _lastWeekNotifications.value = emptyList()
                _lastMonthNotifications.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshNotifications() {
        fetchNotificationsFromFirestore()
    }
}
