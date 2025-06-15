package com.storyspots.yourFeed

import com.storyspots.caption.StoryData

/**
 * Represents the different states of the Your Feed screen
 */
sealed class YourFeedUiState {
    object Loading : YourFeedUiState()
    data class Success(val stories: List<StoryData>) : YourFeedUiState()
    data class Error(val message: String) : YourFeedUiState()
    object Empty : YourFeedUiState()
}

/**
 * Represents the state of delete operations
 */
sealed class DeleteState {
    object Idle : DeleteState()
    data class Deleting(val storyId: String) : DeleteState()
    object Success : DeleteState()
    data class Error(val message: String) : DeleteState()
}