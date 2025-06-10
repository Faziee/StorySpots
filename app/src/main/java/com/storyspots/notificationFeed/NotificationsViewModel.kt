package com.storyspots.notificationFeed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
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

                val notifications = mutableListOf<NotificationItem>()

                for (doc in querySnapshot.documents) {
                    try {
                        println("Processing document: ${doc.id}")
                        println("Document data: ${doc.data}")

                        // Use document ID as the notification ID
                        val notificationId = doc.id

                        // Get basic fields
                        val createdAt = doc.getTimestamp("created_at")?.toDate() ?: Date()
                        val title = doc.getString("title") ?: "Notification"
                        val message = doc.getString("message") ?: ""
                        val read = doc.getBoolean("read") ?: false

                        // Handle fromUserId - could be from a reference or direct field
                        var fromUserId = ""
                        var imageUrl = ""

                        val fromRef = doc.getDocumentReference("from")
                        if (fromRef != null) {
                            try {
                                val userDoc = fromRef.get().await()
                                fromUserId = userDoc.id
                                imageUrl = userDoc.getString("profile_picture_url")
                                    ?: userDoc.getString("profileImageUrl")
                                            ?: userDoc.getString("profile_picture")
                                            ?: ""
                            } catch (e: Exception) {
                                println("Error fetching user data: ${e.message}")
                                fromUserId = doc.getString("fromUserId") ?: ""
                            }
                        } else {
                            fromUserId = doc.getString("fromUserId") ?: ""
                            imageUrl = doc.getString("imageUrl") ?: ""
                        }

                        // Handle story - could be from a reference or direct field
                        var storyId = ""
                        val storyRef = doc.getDocumentReference("story")
                        if (storyRef != null) {
                            storyId = storyRef.id
                        } else {
                            storyId = doc.getString("story") ?: ""
                        }

                        val notificationItem = NotificationItem(
                            id = notificationId,
                            createdAt = createdAt,
                            title = title,
                            message = message,
                            read = read,
                            fromUserId = fromUserId,
                            story = storyId,
                            imageUrl = imageUrl
                        )

                        notifications.add(notificationItem)
                        println("Successfully processed notification: $notificationItem")

                    } catch (e: Exception) {
                        println("Error processing notification ${doc.id}: ${e.message}")
                        e.printStackTrace()
                    }
                }

                println("Processed ${notifications.size} notifications total")

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