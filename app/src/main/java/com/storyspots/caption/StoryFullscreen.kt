package com.storyspots.caption

import android.util.Log
import com.storyspots.ui.theme.DarkText
import com.storyspots.ui.theme.LightText
import com.storyspots.ui.theme.White
import com.storyspots.ui.theme.Black
import com.storyspots.ui.theme.MediumText
import com.storyspots.caption.StoryData
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentReference
import com.storyspots.R
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

// User data class
data class UserData(
    val id: String = "",
    val username: String = "Unknown User",
    val profileImageUrl: String = ""
)

// Function to fetch user data by DocumentReference
suspend fun fetchUserData(userRef: DocumentReference): UserData? {
    return try {
        val userDoc = FirebaseFirestore.getInstance()
            .collection("users")
            .document(userRef.id)
            .get()
            .await()
        Log.d("UserData", "Fetched user data: $userDoc")

        UserData(
            id = userDoc.id,
            username = userDoc.getString("username") ?: "Deleted User",
            profileImageUrl = userDoc.getString("profile_picture_url")
                ?: userDoc.getString("profileImageUrl")
                ?: userDoc.getString("profile_picture")
                ?: ""
        )
    } catch (e: Exception) {
        android.util.Log.e("UserData", "Error fetching user data", e)
        null
    }
}

fun formatRelativeTime(timestamp: com.google.firebase.Timestamp): String {
    val now = Date()
    val date = timestamp.toDate()
    val diffInMillis = now.time - date.time

    val seconds = TimeUnit.MILLISECONDS.toSeconds(diffInMillis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis)
    val hours = TimeUnit.MILLISECONDS.toHours(diffInMillis)
    val days = TimeUnit.MILLISECONDS.toDays(diffInMillis)

    return when {
        seconds < 60 -> "Just now"
        minutes < 60 -> "$minutes minute${if (minutes != 1L) "s" else ""} ago"
        hours < 24 -> "$hours hour${if (hours != 1L) "s" else ""} ago"
        days == 1L -> "Yesterday"
        days in 2..6 -> "$days days ago"
        days in 7..13 -> "Last week"
        else -> {
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            sdf.format(date)
        }
    }
}

@Composable
fun FullscreenStoryOverlay(
    stories: List<StoryData>,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Black.copy(alpha = 0.3f))
            .clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .fillMaxHeight(0.70f)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {},
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
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
                        fontSize = 24.sp,
                        color = LightText,
                        modifier = Modifier
                            .clickable { onClose() }
                            .padding(4.dp)
                    )
                }

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
}

@Composable
fun FullscreenStoryCard(
    story: StoryData,
    onLongPress: () -> Unit = {}
) {
    var userData by remember { mutableStateOf<UserData?>(null) }
    var isLoadingUser by remember { mutableStateOf(true) }

    LaunchedEffect(story.authorRef) {
        if (story.authorRef != null) {
            userData = fetchUserData(story.authorRef)
        }
        isLoadingUser = false
    }

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
                if (isLoadingUser) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(LightText),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = DarkText
                        )
                    }
                } else {
                    if (userData?.profileImageUrl?.isNotEmpty() == true) {
                        Image(
                            painter = rememberAsyncImagePainter(
                                model = userData?.profileImageUrl,
                                placeholder = painterResource(R.drawable.placeholder_image),
                                error = painterResource(R.drawable.placeholder_image)
                            ),
                            contentDescription = "Profile picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(16.dp))
                        )
                    } else {
                        // Default profile picture with first letter of username
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(LightText),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = userData?.username?.firstOrNull()?.toString()?.uppercase() ?: "U",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = userData?.username ?: "Loading...",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = DarkText
                    )
                    story.createdAt?.let { timestamp ->
                        Text(
                            text = formatRelativeTime(timestamp),
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