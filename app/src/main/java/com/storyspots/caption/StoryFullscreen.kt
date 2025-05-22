package com.storyspots.caption

import com.storyspots.ui.theme.DarkText
import com.storyspots.ui.theme.LightText
import com.storyspots.ui.theme.White
import com.storyspots.ui.theme.MediumText
import StoryData
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.storyspots.R

@Composable
fun FullscreenStoryOverlay(
    stories: List<StoryData>,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Stories",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText
                )

                Text(
                    text = "✕",
                    fontSize = 20.sp,
                    color = LightText,
                    modifier = Modifier.clickable { onClose() }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(LightText.copy(alpha = 0.3f))
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(stories) { story ->
                    FullscreenStoryCard(story = story)
                }
            }
        }
    }
}

@Composable
fun FullscreenStoryCard(
    story: StoryData,
    onLongPress: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(LightText)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "User Name",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = DarkText
                    )
                    story.createdAt?.let {
                        Text(
                            text = "3 hours ago",
                            fontSize = 12.sp,
                            color = LightText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            FullscreenStoryImage(story, onLongPress = onLongPress)

            Spacer(modifier = Modifier.height(12.dp))

            FullscreenTitle(story)

            Spacer(modifier = Modifier.height(8.dp))

            FullscreenCaption(story)
        }
    }
}

@Composable
fun FullscreenTitle(story: StoryData) {
    Text(
        text = story.title,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = DarkText,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun FullscreenCaption(story: StoryData) {
    story.caption?.let {
        Text(
            text = it,
            fontSize = 14.sp,
            color = MediumText,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun FullscreenStoryImage(
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
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .clip(RoundedCornerShape(8.dp))
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