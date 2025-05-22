package com.storyspots.caption

import StoryData
import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
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
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip

@Composable
fun StoryCard(
    story: StoryData,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongPress: () -> Unit = {},
    stackCount: Int? = null
) {
    Box(modifier = modifier) {
        stackCount?.let {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-6).dp, y = 6.dp)
                    .size(24.dp)
                    .background(Color(0xFFE91E63), shape = RoundedCornerShape(12.dp))
                    .zIndex(3f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = it.toString(),
                    color = Color.White
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxSize(),
            elevation = CardDefaults.cardElevation(4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Green)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 5.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onClick() },
                            onLongPress = { onLongPress() }
                        )
                    },
                elevation = CardDefaults.cardElevation(0.dp),
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column {
                    story.imageUrl?.let {
                        Image(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                            painter = rememberAsyncImagePainter(
                                model = it,
                                placeholder = painterResource(R.drawable.placeholder_image),
                                error = painterResource(R.drawable.error_image)
                            ),
                            contentDescription = null,
                            contentScale = ContentScale.Crop
                        )
                    }

                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = story.title,
                            style = MaterialTheme.typography.titleMedium
                        )
                        story.createdAt?.let {
                            Text(
                                text = it.toDate().toString(),
                                color = Color(0xFFE91E63),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        story.caption?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
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

//For now, this loads all the stories from the database. TODO: Load by geo-point
@SuppressLint("UseOfNonLambdaOffsetOverload")
@Composable
fun StoryStack(
    screenOffset: Offset,
    onPositioned: (androidx.compose.ui.layout.LayoutCoordinates) -> Unit = {}
) {
    var stories by remember { mutableStateOf<List<StoryData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showFullscreenOverlay by remember { mutableStateOf(false) }
    var currentIndex by remember { mutableStateOf(0) }

    // Offset to position bottom-left of box to the pin center
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

    LaunchedEffect(Unit) {
        fetchAllStories { result ->
            stories = result
            isLoading = false
        }
    }

    if (isLoading) {
        Text("Loading stories...")
        return
    }

    if (showFullscreenOverlay && stories.isNotEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {})
                }
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
        val story = stories[currentIndex]
        val visibleStories = stories.drop(currentIndex).take(3)

        Box(
            modifier = Modifier
                .onGloballyPositioned { coords -> onPositioned(coords) }
                .offset { IntOffset(offsetPx.x.toInt(), offsetPx.y.toInt()) }
                .size(storyBoxWidth, storyBoxHeight)
                .zIndex(1f)
                .pointerInput(Unit) {}
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
                    stackCount = if (index == 0) visibleStories.size else null
                )
            }
        }
    } else {
        Text("No stories found.")
    }
}