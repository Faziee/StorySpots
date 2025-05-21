//package com.storyspots.notificationFeed
//
//import android.util.Log
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.ViewModelProvider
//
//class NotificationsViewModelFactory(private val currentUserId: String) : ViewModelProvider.Factory {
//    override fun <T : ViewModel> create(modelClass: Class<T>): T {
//        Log.d("ViewModelFactory", "Creating ViewModel with currentUserId: $currentUserId")
//        if (modelClass.isAssignableFrom(NotificationsViewModel::class.java)) {
//            @Suppress("UNCHECKED_CAST")
//            return NotificationsViewModel(currentUserId) as T
//        }
//        throw IllegalArgumentException("Unknown ViewModel class")
//    }
//}