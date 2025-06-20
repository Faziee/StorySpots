package com.storyspots.caption.ui

import android.R
import android.R.attr.onClick
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.storyspots.ui.theme.Pink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.zIndex
import coil.compose.rememberAsyncImagePainter
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.storyspots.caption.model.StoryData
import com.storyspots.caption.StoryUtils
import com.storyspots.core.managers.LocationsManager
import com.storyspots.ui.theme.NeonGreen
import com.storyspots.ui.theme.Pink80
import com.storyspots.ui.theme.SuccessColour
import com.storyspots.ui.theme.White

@Composable
fun MapStoryCard(
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
                    .background(Pink, shape = RoundedCornerShape(12.dp))
                    .zIndex(3f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = it.toString(),
                    color = White
                )
            }
        }

        OuterCard(NeonGreen, story, onClick, onLongPress)
    }
}

@Composable
fun OuterCard(
    color: Color,
    story: StoryData,
    onClick: () -> Unit = {},
    onLongPress: () -> Unit = {},
)
{
    Card(
        modifier = Modifier.fillMaxSize(),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        InnerCard(story, onClick, onLongPress)
    }
}

@Composable
fun InnerCard(
    story: StoryData,
    onClick: () -> Unit = {},
    onLongPress: () -> Unit = {},
)
{
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 3.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongPress() }
                )
            },
        elevation = CardDefaults.cardElevation(0.dp),
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        colors = CardDefaults.cardColors(containerColor = White)
    ) {
        Column {
            if (story.imageUrl != "")
            {
                Image(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                    painter = rememberAsyncImagePainter(
                        model = story.imageUrl,
                    ),
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )

                ImageStoryColumn(story)
            }
            else {
                TextStoryColumn(story)
            }
        }
    }
}

@Composable
fun TextStoryColumn(
    story: StoryData
){
    Column(
        modifier = Modifier
            .padding(8.dp)
    ) {
        Text(
            text = story.title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Black
            )
        )
        story.createdAt?.let {
            Text(
                text = StoryUtils().formatRelativeTime(story.createdAt),
                color = Pink,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Light
                )
            )
        }
        story.caption?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Light
                )
            )
        }
    }
}

@Composable
fun ImageStoryColumn(
    story: StoryData
){
    Column(modifier = Modifier.padding(8.dp)) {
        Text(
            text = story.title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black
            )
        )
        story.createdAt?.let {
            Text(
                text = StoryUtils().formatRelativeTime(story.createdAt),
                color = Pink,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Light
                )
            )
        }
        story.caption?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Light
                )
            )
        }
    }
}