// file: ui/components/DismissibleStoryStack.kt
package com.storyspots.ui.components

import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.zIndex
import com.storyspots.caption.StoryStack

@Composable
fun DismissibleStoryStack(
    offset: Offset,
    onDismiss: () -> Unit
) {
    var storyStackBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitFirstDown()
                        val clickedOffset = event.position

                        val isOutside = storyStackBounds?.contains(clickedOffset) == false
                        if (isOutside) {
                            onDismiss()
                        }
                    }
                }
            }
    ) {
        StoryStack(
            screenOffset = offset,
            onPositioned = { layoutCoordinates ->
                storyStackBounds = layoutCoordinates.boundsInWindow()
            }
        )
    }
}
