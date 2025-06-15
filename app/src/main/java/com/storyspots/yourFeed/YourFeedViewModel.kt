package com.storyspots.yourFeed

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.storyspots.caption.StoryData
import com.storyspots.core.AppComponents
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class YourFeedViewModel(
    private val repository: YourFeedRepository = YourFeedRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<YourFeedUiState>(YourFeedUiState.Loading)
    val uiState: StateFlow<YourFeedUiState> = _uiState.asStateFlow()

    private val _deleteState = MutableStateFlow<DeleteState>(DeleteState.Idle)
    val deleteState: StateFlow<DeleteState> = _deleteState.asStateFlow()

    companion object {
        private const val TAG = "YourFeedViewModel"

        fun formatFirebaseTimestamp(timestamp: Timestamp): String {
            val sdf = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
            return sdf.format(timestamp.toDate())
        }
    }

    init {
        loadStories()
    }

    fun loadStories() {
        if (!repository.isUserAuthenticated()) {
            _uiState.value = YourFeedUiState.Error("User not authenticated")
            return
        }

        _uiState.value = YourFeedUiState.Loading

        viewModelScope.launch {
            repository.getUserStoriesFlow()
                .catch { error ->
                    Log.e(TAG, "Error loading stories", error)
                    _uiState.value = YourFeedUiState.Error(
                        error.message ?: "Failed to load stories"
                    )
                }
                .collect { stories ->
                    _uiState.value = if (stories.isEmpty()) {
                        YourFeedUiState.Empty
                    } else {
                        YourFeedUiState.Success(stories)
                    }
                }
        }
    }

    /**
     * @param story The story to delete
     */
    fun deleteStory(story: StoryData) {
        if (story.id.isNullOrEmpty()) {
            _deleteState.value = DeleteState.Error("Invalid story ID")
            return
        }

        viewModelScope.launch {
            _deleteState.value = DeleteState.Deleting(story.id)

            val result = repository.deleteStory(story.id)

            if (result.isSuccess) {
                Log.d(TAG, "Story deleted successfully: ${story.id}")
                _deleteState.value = DeleteState.Success
                refreshMapPins()

                kotlinx.coroutines.delay(1000)
                _deleteState.value = DeleteState.Idle
            } else {
                val error = result.exceptionOrNull()
                Log.e(TAG, "Failed to delete story: ${story.id}", error)
                _deleteState.value = DeleteState.Error(
                    error?.message ?: "Failed to delete story"
                )

                kotlinx.coroutines.delay(3000)
                _deleteState.value = DeleteState.Idle
            }
        }
    }

    fun retry() {
        loadStories()
    }

    fun clearDeleteError() {
        if (_deleteState.value is DeleteState.Error) {
            _deleteState.value = DeleteState.Idle
        }
    }

    private fun refreshMapPins() {
        try {
            AppComponents.refreshStories()
            Log.d(TAG, "Map pins refreshed after story deletion")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh map pins", e)
        }
    }
}