package com.storyspots.yourFeed

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
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
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var storyToDelete by remember { mutableStateOf<StoryData?>(null) }
    var isDeleting by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val listenerRegistration = fetchUserStories { userStories, error ->
            stories = userStories
            isLoading = false
            errorMessage = error
        }

        onDispose {
            listenerRegistration?.remove()
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog && storyToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                if (!isDeleting) {
                    showDeleteDialog = false
                    storyToDelete = null
                }
            },
            title = {
                Text(
                    text = "Delete Story",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Are you sure you want to delete this story? This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isDeleting = true
                        deleteStory(storyToDelete!!) { success, error ->
                            isDeleting = false
                            if (success) {
                                showDeleteDialog = false
                                storyToDelete = null
                                refreshMapPins()
                            } else {
                                Log.e("YourFeedScreen", "Failed to delete story: $error")
                            }
                        }
                    },
                    enabled = !isDeleting
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.Red
                        )
                    } else {
                        Text(
                            text = "Delete",
                            color = Color.Red,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (!isDeleting) {
                            showDeleteDialog = false
                            storyToDelete = null
                        }
                    },
                    enabled = !isDeleting
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
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
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFFFC6C85)
                    )
                }
            }
            errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error loading stories",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Red
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage ?: "Unknown error",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            stories.isEmpty() -> {
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(stories) { story ->
                        StoryCard(
                            story = story,
                            onDeleteClick = {
                                storyToDelete = story
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StoryCard(
    story: StoryData?,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current

    if (story == null) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = story?.title ?: "No Title",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )

                story?.caption?.let { caption ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = caption,
                        fontSize = 16.sp,
                        color = Color(0xFF666666),
                        lineHeight = 22.sp
                    )
                }

                story?.imageUrl?.let { imageUrl ->
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
                                    Log.e("StoryCard", "Failed to load image: $imageUrl", throwable.throwable)
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

                Spacer(modifier = Modifier.height(12.dp))

                story?.location?.let { geoPoint ->
                    Text(
                        text = "📍 ${String.format("%.4f", geoPoint.latitude)}, ${String.format("%.4f", geoPoint.longitude)}",
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                story?.createdAt?.let { timestamp ->
                    Text(
                        text = formatFirebaseTimestamp(timestamp),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
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

fun formatFirebaseTimestamp(timestamp: Timestamp): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
    return sdf.format(timestamp.toDate())
}

fun deleteStory(story: StoryData, onResult: (Boolean, String?) -> Unit) {
    val TAG = "DeleteStory"

    if (story.id.isNullOrEmpty()) {
        Log.e(TAG, "Story ID is null or empty")
        onResult(false, "Invalid story ID")
        return
    }

    val currentUser = FirebaseAuth.getInstance().currentUser
    if (currentUser == null) {
        Log.e(TAG, "No authenticated user")
        onResult(false, "User not authenticated")
        return
    }

    val db = FirebaseFirestore.getInstance()

    Log.d(TAG, "Attempting to delete story with ID: ${story.id}")

    db.collection("story")
        .document(story.id)
        .get()
        .addOnSuccessListener { document ->
            if (!document.exists()) {
                Log.e(TAG, "Story does not exist")
                onResult(false, "Story not found")
                return@addOnSuccessListener
            }

            val userField = document.get("user")
            val userPath = "/user/${currentUser.uid}"

            if (userField != userPath) {
                Log.e(TAG, "Story does not belong to current user")
                onResult(false, "Unauthorized to delete this story")
                return@addOnSuccessListener
            }

            db.collection("story")
                .document(story.id)
                .delete()
                .addOnSuccessListener {
                    Log.d(TAG, "Story deleted successfully")
                    onResult(true, null)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to delete story", e)
                    onResult(false, "Failed to delete: ${e.message}")
                }
        }
        .addOnFailureListener { e ->
            Log.e(TAG, "Failed to verify story ownership", e)
            onResult(false, "Failed to verify story: ${e.message}")
        }
}

fun fetchUserStories(onResult: (List<StoryData>, String?) -> Unit): ListenerRegistration? {
    val TAG = "YourFeedScreen"

    val currentUser = FirebaseAuth.getInstance().currentUser
    if (currentUser == null) {
        Log.e(TAG, "No authenticated user found")
        onResult(emptyList(), "User not authenticated")
        return null
    }

    Log.d(TAG, "Current user ID: ${currentUser.uid}")

    val db = FirebaseFirestore.getInstance()

    val userPath = "/user/${currentUser.uid}"
    Log.d(TAG, "Current authenticated user ID: ${currentUser.uid}")
    Log.d(TAG, "Looking for user path: $userPath")

    db.collection("story")
        .get()
        .addOnSuccessListener { allStories ->
            Log.d(TAG, "=== ALL STORIES DEBUG ===")
            Log.d(TAG, "Total stories in database: ${allStories.size()}")
            allStories.documents.forEach { doc ->
                Log.d(TAG, "Story ID: ${doc.id}")
                Log.d(TAG, "  User field: '${doc.get("user")}'")
                Log.d(TAG, "  Title: '${doc.get("title")}'")
                Log.d(TAG, "  Image URL: '${doc.get("imageUrl")}'")
                Log.d(TAG, "  Created: ${doc.get("created_at")}")
                Log.d(TAG, "  Matches current user: ${doc.get("user") == userPath}")
            }
            Log.d(TAG, "=== END ALL STORIES DEBUG ===")
        }

    return db.collection("story")
        .whereEqualTo("user", userPath)
        .limit(50)
        .addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e(TAG, "Error fetching user stories", e)
                onResult(emptyList(), "Error: ${e.message}")
                return@addSnapshotListener
            }

            if (snapshot == null) {
                Log.e(TAG, "Snapshot is null")
                onResult(emptyList(), "No data received")
                return@addSnapshotListener
            }

            Log.d(TAG, "Documents found: ${snapshot.size()}")

            snapshot.documents.forEachIndexed { index, document ->
                Log.d(TAG, "Document $index:")
                Log.d(TAG, "  ID: ${document.id}")
                Log.d(TAG, "  User field: ${document.get("user")}")
                Log.d(TAG, "  Image URL: ${document.get("imageUrl")}")
                Log.d(TAG, "  Matches current user: ${document.get("user") == userPath}")
            }

            val stories = snapshot.documents.mapNotNull { document ->
                try {
                    val storyData = document.toStoryData()
                    if (storyData != null) {
                        Log.d(TAG, "Successfully converted document ${document.id} to StoryData")
                        Log.d(TAG, "StoryData imageUrl: ${storyData.imageUrl}")
                        storyData
                    } else {
                        Log.w(TAG, "Failed to convert document ${document.id} - toStoryData returned null")
                        null
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error converting document ${document.id} to StoryData", e)
                    null
                }
            }.sortedByDescending { it.createdAt?.toDate() }

            Log.d(TAG, "Total stories after conversion: ${stories.size}")
            onResult(stories, null)
        }
}

// Alternative fetch function if the main one doesn't work
fun fetchUserStoriesAlternative(onResult: (List<StoryData>, String?) -> Unit): ListenerRegistration? {
    val TAG = "YourFeedScreen_Alt"

    val currentUser = FirebaseAuth.getInstance().currentUser
    if (currentUser == null) {
        Log.e(TAG, "No authenticated user found")
        onResult(emptyList(), "User not authenticated")
        return null
    }

    val db = FirebaseFirestore.getInstance()

    return db.collection("story")
        .whereEqualTo("userId", currentUser.uid)
        .orderBy("created_at", Query.Direction.DESCENDING)
        .limit(50)
        .addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e(TAG, "Error with alternative query", e)
                tryDirectUserIdQuery(currentUser.uid, onResult)
                return@addSnapshotListener
            }

            val stories = snapshot?.documents?.mapNotNull { it.toStoryData() } ?: emptyList()
            Log.d(TAG, "Alternative query found ${stories.size} stories")
            onResult(stories, null)
        }
}

fun tryDirectUserIdQuery(userId: String, onResult: (List<StoryData>, String?) -> Unit) {
    val TAG = "YourFeedScreen_Direct"
    val db = FirebaseFirestore.getInstance()

    db.collection("story")
        .get()
        .addOnSuccessListener { snapshot ->
            Log.d(TAG, "All stories count: ${snapshot.size()}")

            val userStories = snapshot.documents.filter { document ->
                val userField = document.get("user")
                val userIdField = document.get("userId")

                Log.d(TAG, "Document ${document.id}:")
                Log.d(TAG, "  user field: $userField")
                Log.d(TAG, "  userId field: $userIdField")

                when {
                    userField is DocumentReference && userField.id == userId -> true
                    userIdField == userId -> true
                    else -> false
                }
            }.mapNotNull { it.toStoryData() }

            Log.d(TAG, "User stories found: ${userStories.size}")
            onResult(userStories, null)
        }
        .addOnFailureListener { e ->
            Log.e(TAG, "Error with direct query", e)
            onResult(emptyList(), "Error: ${e.message}")
        }
}

fun refreshMapPins() {
    try {
        com.storyspots.core.AppComponents.refreshStories()
        Log.d("YourFeedScreen", "Map pins refreshed after story deletion")
    } catch (e: Exception) {
        Log.e("YourFeedScreen", "Failed to refresh map pins", e)
    }
}