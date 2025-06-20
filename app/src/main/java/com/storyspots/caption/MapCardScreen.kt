package com.storyspots.caption

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.zIndex
import coil.compose.rememberAsyncImagePainter
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

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
                    .background(Color(0xFFFF398F), shape = RoundedCornerShape(12.dp))
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
            colors = CardDefaults.cardColors(containerColor = Color(0xFF8FFF53))
        ) {
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
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column {
                    story.imageUrl?.let() //TODO: Make proper text story
                    {
                        Image(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                            painter = rememberAsyncImagePainter(
                                model = story.imageUrl,
//                                placeholder = painterResource(R.drawable.placeholder_image),
//                                error = painterResource(R.drawable.error_image)
                            ),
                            contentDescription = null,
                            contentScale = ContentScale.Crop
                        )
                    }

                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = story.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black
                            )
                        )
                        story.createdAt?.let {
                            Text(
                                text = formatRelativeTime(story.createdAt),
                                color = Color(0xFFFF398F),
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
            }
        }
    }
}
