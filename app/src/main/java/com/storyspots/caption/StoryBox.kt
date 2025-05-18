package com.storyspots.caption

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

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
        AsyncImage(model = it, contentDescription = null, modifier = Modifier.size(100.dp))
    }
}

//For now, this loads all the stories from the database. TODO: Load by geo-point
@Composable
fun StoryStack()
{
    var stories by remember { mutableStateOf<List<StoryData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        stories = fetchAllStories()
        isLoading = false
    }

    if (isLoading)
    {
        Text("Loading stories...")
    }
    else {
        stories.forEach { story ->
            StoryCard(story)
        }
    }
}

@Preview
@Composable
fun PreviewStoryBox()
{
    StoryStack()
}