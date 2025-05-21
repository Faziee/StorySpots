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
        println("ViewModel initialized, starting fetch")
        fetchNotificationsFromFirestore()
    }

    fun fetchNotificationsFromFirestore() {
        println("Starting fetchNotificationsFromFirestore")

        viewModelScope.launch {
            _isLoading.value = true

            try {
                println("Fetching all notifications")
                val db = FirebaseFirestore.getInstance()
                val notificationsRef = db.collection("notification")

                val querySnapshot = notificationsRef
                    .orderBy("created_at", Query.Direction.DESCENDING)
                    .get()
                    .await()

                println("Query returned ${querySnapshot.documents.size} documents")

                val notifications = querySnapshot.documents.mapNotNull { doc ->
                    try {
                        val fromRef = doc.getDocumentReference("from") ?: return@mapNotNull null
                        val toRef = doc.getDocumentReference("to") ?: return@mapNotNull null
                        val storyRef = doc.getDocumentReference("story") ?: return@mapNotNull null
                        val userId = fromRef.id
                        val userDoc = db.collection("user").document(userId).get().await()
                        println("User doc for $userId: ${userDoc.data}")
                        val userName = userDoc.getString("username") ?: "Unknown User"
                        val imageUrl = userDoc.getString("profileImageUrl") ?: ""

                        val storyId = storyRef.id
                        val message = "$userName mentioned you in a story"

                        NotificationItem(
                            id = doc.id,
                            createdAt = doc.getTimestamp("created_at")?.toDate() ?: Date(),
                            from = userName,
                            read = doc.getBoolean("read") ?: false,
                            story = storyRef.path,
                            to = toRef.path,
                            imageUrl = imageUrl,
                            message = message
                        )
                    } catch (e: Exception) {
                        println("Error processing notification ${doc.id}: ${e.message}")
                        null
                    }
                }

                println("Processed ${notifications.size} notifications: $notifications")

                val (new, lastWeek, lastMonth) = NotificationUtils.categorizeNotifications(notifications)
                println("Categorized: New=${new.size}, LastWeek=${lastWeek.size}, LastMonth=${lastMonth.size}")
                _newNotifications.value = new
                _lastWeekNotifications.value = lastWeek
                _lastMonthNotifications.value = lastMonth
            } catch (e: Exception) {
                e.printStackTrace()
                println("Firestore fetch error: ${e.message}")
                _newNotifications.value = emptyList()
                _lastWeekNotifications.value = emptyList()
                _lastMonthNotifications.value = emptyList()
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