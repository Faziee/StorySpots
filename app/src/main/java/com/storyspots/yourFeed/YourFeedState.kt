package com.storyspots.yourFeed

import com.storyspots.caption.model.StoryData

sealed class YourFeedUiState {
    object Loading : YourFeedUiState()
    data class Success(val stories: List<StoryData>) : YourFeedUiState()
    data class Error(val message: String) : YourFeedUiState()
    object Empty : YourFeedUiState()
}

sealed class DeleteState {
    object Idle : DeleteState()
    data class Deleting(val storyId: String) : DeleteState()
    object Success : DeleteState()
    data class Error(val message: String) : DeleteState()
}