package com.storyspots.caption

import StoryData
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.storyspots.R
import fetchAllStories

@Composable
fun StoryCard(story: StoryData) {
    Column {
        StoryImage(story)
        Title(story)
        CreatedAt(story)
        Caption(story)
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
fun StoryImage(story: StoryData)
{
    story.imageUrl?.let {
        Image(
            painter = rememberAsyncImagePainter(
                model = story.imageUrl,
                placeholder = painterResource(R.drawable.placeholder_image),
                error = painterResource(R.drawable.error_image)
            ),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().height(200.dp)
        )
    }
}

//For now, this loads all the stories from the database. TODO: Load by geo-point
@Composable
fun StoryStack() {
    var stories by remember { mutableStateOf<List<StoryData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

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

    if (isLoading) {
        Text("Loading stories...")
    }
    else
    {
        LazyRow {
            items(stories) { story ->
                StoryCard(story)
            }
        }
    }
}

@Preview
@Composable
fun PreviewStoryBox()
{
    StoryStack()
}