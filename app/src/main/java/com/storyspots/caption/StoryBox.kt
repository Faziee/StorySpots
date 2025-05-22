package com.storyspots.caption

import StoryData
import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import coil.compose.rememberAsyncImagePainter
import com.storyspots.R
import fetchAllStories

@Composable
fun StoryCard(story: StoryData,
              modifier: Modifier = Modifier,
              onLongPress: () -> Unit = {}
              ) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White))

    {
        Box (
            contentAlignment = Alignment.TopCenter
        )
        {
            StoryImage(story = story, onLongPress = onLongPress)
        }
        Column {
            Title(story)
            CreatedAt(story)
            Caption(story)
        }
    }
}

@Composable
fun Title(story: StoryData)
{
    Text(text = story.title)
}

@Composable
fun Caption(story: StoryData)
{
    story.caption?.let { Text(it) }
}

@Composable
fun CreatedAt(story: StoryData)
{
    story.createdAt?.let { Text(it.toDate().toString()) }
}

@Composable
fun StoryImage(story: StoryData, onLongPress: () -> Unit = {})
{
    story.imageUrl?.let {
        Image(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.45f)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            onLongPress()
                        }
                    )
                },
            painter = rememberAsyncImagePainter(
                model = story.imageUrl,
                placeholder = painterResource(R.drawable.placeholder_image),
                error = painterResource(R.drawable.error_image)
            ),
            contentDescription = null,
            contentScale = ContentScale.Crop
        )
    }
}

//For now, this loads all the stories from the database. TODO: Load by geo-point
@SuppressLint("UseOfNonLambdaOffsetOverload")
@Composable
fun StoryStack(screenOffset: Offset, onPositioned: (androidx.compose.ui.layout.LayoutCoordinates) -> Unit = {}) {
    var stories by remember { mutableStateOf<List<StoryData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showFullscreenOverlay by remember { mutableStateOf(false) }
    var currentIndex by remember { mutableStateOf(0) }

    // Offset to position bottom-left of box to the pin center
    val storyBoxWidth = 150.dp
    val storyBoxHeight = 160.dp

    val density = LocalDensity.current
    val offsetPx = with(density) {
        // Convert dp to pixels
        val widthPx = storyBoxWidth.toPx()
        val heightPx = storyBoxHeight.toPx()

        Offset(
            x = screenOffset.x,
            y = screenOffset.y - heightPx
        )
    }

    LaunchedEffect(Unit) {
        fetchAllStories { result ->
            stories = result
            isLoading = false
        }
    }

    if (isLoading) {
        Text("Loading stories...")

    } else if (stories.isNotEmpty() && showFullscreenOverlay) {
        FullscreenStoryOverlay(
            stories = stories,
            onClose = { showFullscreenOverlay = false }
        )
    } else if (stories.isNotEmpty()) {
        val story = stories[currentIndex]
        val visibleStories = stories.drop(currentIndex).take(3)

        Box(
            modifier = Modifier
                .onGloballyPositioned { coords -> onPositioned(coords) }
                .offset { IntOffset(offsetPx.x.toInt(), offsetPx.y.toInt()) }
                .size(storyBoxWidth, storyBoxHeight)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    currentIndex = (currentIndex + 1) % stories.size
                }
                .zIndex(1f),
            contentAlignment = Alignment.Center
        ) {
            visibleStories.forEachIndexed { index, story ->
                val stackIndex = visibleStories.size - 1 - index

                StoryCard(
                    story = story,
                    modifier = Modifier
                        .zIndex(stackIndex.toFloat())
                        .size(width = 150.dp, height = 160.dp)
                        .offset(
                            x = (stackIndex * 5).dp,
                            y = -(stackIndex * 5).dp
                        ),
                    onLongPress = { showFullscreenOverlay = true }
                )
            }
        }
    } else {
        Text("No stories found.")
    }
}