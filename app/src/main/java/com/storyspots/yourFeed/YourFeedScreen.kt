package com.storyspots.yourFeed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.storyspots.R
import com.storyspots.caption.model.StoryData
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