// file: ui/components/DismissibleStoryStack.kt
package com.storyspots.caption

import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@Composable
fun DismissibleStoryStack(
    stories: List<StoryData>,
    offset: Offset,
    onDismiss: () -> Unit
) {
    var storyStackBounds by remember { mutableStateOf<Rect?>(null) }

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
        StoryStackForPin(
            stories = stories,
            screenOffset = offset,
            onDismiss = onDismiss,
            onPositioned = { layoutCoordinates ->
                storyStackBounds = layoutCoordinates.boundsInWindow()
            }
        )
    }
}

// And update StoryStackForPin to accept onPositioned
@Composable
fun StoryStackForPin(
    stories: List<StoryData>,
    screenOffset: Offset,
    onDismiss: () -> Unit = {},
    onPositioned: (androidx.compose.ui.layout.LayoutCoordinates) -> Unit = {}
) {
    var currentIndex by remember { mutableStateOf(0) }
    var showFullscreenOverlay by remember { mutableStateOf(false) }

    // Reset index when stories change
    LaunchedEffect(stories) {
        currentIndex = 0
    }

    val storyBoxWidth = 180.dp
    val storyBoxHeight = 200.dp

    val density = LocalDensity.current
    val offsetPx = with(density) {
        val heightPx = storyBoxHeight.toPx()
        Offset(
            x = screenOffset.x,
            y = screenOffset.y - heightPx
        )
    }

    if (showFullscreenOverlay && stories.isNotEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(2f)
        ) {
            FullscreenStoryOverlay(
                stories = stories,
                onClose = { showFullscreenOverlay = false }
            )
        }
        return
    }

    if (stories.isNotEmpty()) {
        val visibleStories = stories.drop(currentIndex).take(3)
        val remainingStoriesCount = stories.size - currentIndex

        Box(
            modifier = Modifier
                .onGloballyPositioned { coords -> onPositioned(coords) }
                .offset { IntOffset(offsetPx.x.toInt(), offsetPx.y.toInt()) }
                .size(storyBoxWidth, storyBoxHeight)
                .zIndex(1f)
        ) {
            visibleStories.forEachIndexed { index, stackedStory ->
                val stackIndex = visibleStories.size - 1 - index

                StoryCard(
                    story = stackedStory,
                    modifier = Modifier
                        .zIndex(stackIndex.toFloat())
                        .size(storyBoxWidth, storyBoxHeight)
                        .offset(
                            x = (stackIndex * 5).dp,
                            y = -(stackIndex * 5).dp
                        ),
                    onClick = {
                        currentIndex = (currentIndex + 1) % stories.size
                    },
                    onLongPress = { showFullscreenOverlay = true },
                    stackCount = if (index == 0) remainingStoriesCount else null
                )
            }
        }
    }
}