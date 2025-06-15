package com.storyspots.yourFeed

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.storyspots.R
import com.storyspots.caption.StoryData
import com.storyspots.yourFeed.YourFeedViewModel.Companion.formatFirebaseTimestamp
import com.storyspots.yourFeed.components.DeleteConfirmationDialog
import com.storyspots.yourFeed.components.StoryCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YourFeedScreen(
    viewModel: YourFeedViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val deleteState by viewModel.deleteState.collectAsState()
    val context = LocalContext.current

    var showDeleteDialog by remember { mutableStateOf(false) }
    var storyToDelete by remember { mutableStateOf<StoryData?>(null) }

    // Handle delete state changes
    LaunchedEffect(deleteState) {
        when (deleteState) {
            is DeleteState.Success -> {
                showDeleteDialog = false
                storyToDelete = null
            }
            is DeleteState.Error -> { /* Error handled in UI */}
            else -> { /* No action needed */ }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog && storyToDelete != null) {
        DeleteConfirmationDialog(
            onDismiss = {
                showDeleteDialog = false
                storyToDelete = null
                viewModel.clearDeleteError()
            },
            onConfirm = {
                storyToDelete?.let { story ->
                    viewModel.deleteStory(story)
                }
            },
            isDeleting = deleteState is DeleteState.Deleting
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
            actions = {
                // Refresh button for error states
                if (uiState is YourFeedUiState.Error) {
                    IconButton(onClick = { viewModel.retry() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry",
                            tint = Color(0xFFFC6C85)
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White
            )
        )

        // Show delete error as snackbar
        if (deleteState is DeleteState.Error) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFEBEE)
                )
            ) {
                Text(
                    text = "Failed to delete story: ${(deleteState as DeleteState.Error).message}",
                    modifier = Modifier.padding(16.dp),
                    color = Color(0xFFD32F2F)
                )
            }
        }

        // Main content based on UI state
        when (uiState) {
            is YourFeedUiState.Loading -> {
                LoadingContent()
            }
            is YourFeedUiState.Error -> {
                ErrorContent(
                    message = (uiState as YourFeedUiState.Error).message,
                    onRetry = { viewModel.retry() }
                )
            }
            is YourFeedUiState.Empty -> {
                EmptyContent()
            }
            is YourFeedUiState.Success -> {
                SuccessContent(
                    stories = (uiState as YourFeedUiState.Success).stories,
                    deleteState = deleteState,
                    onDeleteClick = { story ->
                        storyToDelete = story
                        showDeleteDialog = true
                    }
                )
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = Color(0xFFFC6C85)
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
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
                text = message,
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFC6C85)
                )
            ) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun EmptyContent() {
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

@Composable
private fun SuccessContent(
    stories: List<StoryData>,
    deleteState: DeleteState,
    onDeleteClick: (StoryData) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(stories) { story ->
            val isDeleting = deleteState is DeleteState.Deleting &&
                    deleteState.storyId == story.id

            StoryCard(
                story = story,
                onDeleteClick = { onDeleteClick(story) },
                isDeleting = isDeleting
            )
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