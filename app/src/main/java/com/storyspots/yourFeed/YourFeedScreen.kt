package com.storyspots.yourFeed

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*
import com.storyspots.caption.StoryData
import com.storyspots.caption.toStoryData
import com.storyspots.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YourFeedScreen() {
    var stories by remember { mutableStateOf<List<StoryData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Fetch user's stories from Firebase
    DisposableEffect(Unit) {
        val listenerRegistration = fetchUserStories { userStories ->
            stories = userStories
            isLoading = false
        }

        onDispose {
            listenerRegistration?.remove()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        // Header
        TopAppBar(
            title = {
                Text(
                    text = "Your Stories",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White
            )
        )

        when {
            isLoading -> {
                // Loading state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFFFC6C85)
                    )
                }
            }
            stories.isEmpty() -> {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_post),
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No stories yet",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Start sharing your stories with the world!",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            else -> {
                // Stories list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(stories) { story ->
                        StoryCard(story = story)
                    }
                }
            }
        }
    }
}

@Composable
fun StoryCard(story: StoryData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Story title
            Text(
                text = story.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )

            // Story caption if available
            story.caption?.let { caption ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = caption,
                    fontSize = 16.sp,
                    color = Color(0xFF666666),
                    lineHeight = 22.sp
                )
            }

            // Image placeholder (you can implement image loading with Coil or Glide)
            story.imageUrl?.let { imageUrl ->
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF0F0F0)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Image: ${imageUrl.takeLast(20)}...",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    // TODO: Load actual image using Coil or Glide
                    // AsyncImage(model = imageUrl, contentDescription = "Story image")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Location info if available
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
    }
}

fun formatFirebaseTimestamp(timestamp: Timestamp): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
    return sdf.format(timestamp.toDate())
}

// Function to fetch user's stories from Firebase
fun fetchUserStories(onResult: (List<StoryData>) -> Unit): ListenerRegistration? {
    val currentUser = FirebaseAuth.getInstance().currentUser
    if (currentUser == null) {
        onResult(emptyList())
        return null
    }

    val db = FirebaseFirestore.getInstance()
    val userRef = db.collection("users").document(currentUser.uid)

    return db.collection("story")
        .whereEqualTo("user", userRef)
        .orderBy("created_at", Query.Direction.DESCENDING)
        .limit(50)
        .addSnapshotListener { snapshot, e ->
            if (e != null) {
                android.util.Log.e("YourFeedScreen", "Error fetching user stories", e)
                onResult(emptyList())
                return@addSnapshotListener
            }

            val stories = snapshot?.documents?.mapNotNull { it.toStoryData() } ?: emptyList()
            onResult(stories)
        }
}