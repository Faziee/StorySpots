package com.storyspots.yourFeed.components

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.storyspots.R
import com.storyspots.caption.model.StoryData
import com.storyspots.yourFeed.YourFeedViewModel.Companion.formatFirebaseTimestamp

@Composable
fun StoryCard(
    story: StoryData,
    onDeleteClick: () -> Unit,
    isDeleting: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Title
                Text(
                    text = story.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )

                // Caption
                story.caption?.let { caption ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = caption,
                        fontSize = 16.sp,
                        color = Color(0xFF666666),
                        lineHeight = 22.sp
                    )
                }

                if (story.imageUrl != "") {
                    story.imageUrl.let { imageUrl ->
                        Log.d("StoryCard", "Loading image from URL: $imageUrl")

                        Spacer(modifier = Modifier.height(12.dp))

                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(imageUrl)
                                .crossfade(true)
                                .listener(
                                    onStart = {
                                        Log.d("StoryCard", "Started loading image: $imageUrl")
                                    },
                                    onSuccess = { _, _ ->
                                        Log.d("StoryCard", "Successfully loaded image: $imageUrl")
                                    },
                                    onError = { _, throwable ->
                                        Log.e(
                                            "StoryCard",
                                            "Failed to load image: $imageUrl",
                                            throwable.throwable
                                        )
                                    }
                                )
                                .build(),
                            contentDescription = "Story image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop,
                            placeholder = painterResource(R.drawable.placeholder_image),
                            error = painterResource(R.drawable.placeholder_image)
                        )
                    }
                }
                else { /* Do nothing */}

                Spacer(modifier = Modifier.height(12.dp))

                // Location
                story.location?.let { geoPoint ->
                    Text(
                        text = "📍 ${String.format("%.4f", geoPoint.latitude)}, ${String.format("%.4f", geoPoint.longitude)}",
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Timestamp
                story.createdAt?.let { timestamp ->
                    Text(
                        text = formatFirebaseTimestamp(timestamp),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer (modifier = Modifier.fillMaxWidth())

            // Delete button
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.Red
                    )
                } else {
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete story",
                            tint = Color(0xFF666666),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}