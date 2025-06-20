package com.storyspots.core.managers

import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ImageSelectionManager {
    data class SelectionState(
        val selectedImageUri: Uri? = null,
        val pendingSelection: Boolean = false
    )

    private val _selectionState = MutableStateFlow(SelectionState())
    val selectionState: StateFlow<SelectionState> = _selectionState.asStateFlow()

    fun setSelectedImage(uri: Uri?) {
        _selectionState.value = _selectionState.value.copy(
            selectedImageUri = uri,
            pendingSelection = false
        )
    }

    fun setPendingSelection(pending: Boolean) {
        _selectionState.value = _selectionState.value.copy(pendingSelection = pending)
    }

    fun clearSelection() {
        _selectionState.value = SelectionState()
    }
}