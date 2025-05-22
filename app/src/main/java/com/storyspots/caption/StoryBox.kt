package com.storyspots.caption

import StoryData
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import coil.compose.rememberAsyncImagePainter
import com.storyspots.R
import fetchAllStories

@Composable
fun StoryCard(
    story: StoryData,
    modifier: Modifier = Modifier,
    onLongPress: () -> Unit = {}
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            StoryImage(story, onLongPress = onLongPress)
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
fun StoryImage(
    story: StoryData,
    onLongPress: () -> Unit = {}
) {
    story.imageUrl?.let {
        Image(
            painter = rememberAsyncImagePainter(
                model = story.imageUrl,
                placeholder = painterResource(R.drawable.placeholder_image),
                error = painterResource(R.drawable.error_image)
            ),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            onLongPress()
                        }
                    )
                }
        )
    }
}

//For now, this loads all the stories from the database. TODO: Load by geo-point
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun StoryStack() {
    var stories by remember { mutableStateOf<List<StoryData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showFullscreenOverlay by remember { mutableStateOf(false) }
    var currentIndex by remember { mutableStateOf(0) }

    // Start listening once
    DisposableEffect(Unit) {
        val listener = fetchAllStories(limit = 50) { fetchedStories ->
            stories = fetchedStories
            isLoading = false
        }

        onDispose {
            listener.remove()
        }
    }

    when (isLoading) {
        Text("Loading stories...")
    }

    stories.isNotEmpty() && showFullscreenOverlay -> {
            FullscreenStoryOverlay(
                stories = stories,
                onClose = { showFullscreenOverlay = false }
            )
     }

     stories.isNotEmpty() -> {
            val story = stories[currentIndex]

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        currentIndex = (currentIndex + 1) % stories.size
                    },
                contentAlignment = Alignment.Center
            ) {
                StoryCard(
                    story = story,
                    onLongPress = { showFullscreenOverlay = true },
                    modifier = Modifier
                        .zIndex(1f)
                        .size(width = 150.dp, height = 200.dp)
                )
            }
        }
     else -> {
           Text("No stories found.")
        }
    }
}