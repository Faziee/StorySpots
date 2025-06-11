package com.storyspots.post

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.firestore.GeoPoint
import com.storyspots.R
import com.storyspots.post.PostStoryHandler

@Composable
fun PostStoryScreen(
    onImageSelect: () -> Unit,
    selectedImageUri: Uri? = null,
    onPostSuccess: () -> Unit = {},
    modifier: Modifier = Modifier,
    getLocation: () -> GeoPoint? = { null },
    userId: String? = null
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    val context = LocalContext.current
    val postHandler = remember { PostStoryHandler(context) }
    val postState = postHandler.postState.collectAsState().value

    val brightPink = Color(0xFFFF9CC7)
    val lightGray = Color(0xFFF5F5F5)
    val buttonGray = Color(0xFFE0E0E0)

    val isPostEnabled = title.isNotBlank() && description.isNotBlank() &&
            postState !is PostStoryHandler.PostState.UploadingImage &&
            postState !is PostStoryHandler.PostState.SavingToFirestore

    // Handle post state changes
    LaunchedEffect(postState) {
        when (postState) {
            is PostStoryHandler.PostState.Success -> {
                Log.d("PostStoryScreen", "Post successful!")
                onPostSuccess()
                postHandler.resetState()
            }
            is PostStoryHandler.PostState.Error -> {
                val errorState = postState as? PostStoryHandler.PostState.Error
                Log.e("PostStoryScreen", "Post error: ${errorState?.message}")
            }
            else -> { /* Handle other states as needed */ }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(lightGray)
            .padding(horizontal = 32.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Share Your Story",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = Color.Black
            ),
            modifier = Modifier.padding(bottom = 3.dp)
        )

        Box(
            modifier = Modifier
                .shadow(4.dp, RoundedCornerShape(8.dp), clip = false)
                .background(Color.White, shape = RoundedCornerShape(8.dp))
                .border(1.dp, brightPink, RoundedCornerShape(8.dp))
                .fillMaxWidth()
                .height(180.dp)
        ) {
            if (selectedImageUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(selectedImageUri),
                    contentDescription = "Selected image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onImageSelect() }
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_image),
                        contentDescription = "Add photo",
                        colorFilter = ColorFilter.tint(Color.Gray),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Tap to select photo", color = Color.Gray)
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(8.dp))
                .border(1.dp, brightPink, RoundedCornerShape(8.dp))
        ) {
            TextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Story Title") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                enabled = postState !is PostStoryHandler.PostState.UploadingImage &&
                        postState !is PostStoryHandler.PostState.SavingToFirestore
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(8.dp))
                .border(1.dp, brightPink, RoundedCornerShape(8.dp))
        ) {
            TextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Story Description") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 4,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(8.dp),
                enabled = postState !is PostStoryHandler.PostState.UploadingImage &&
                        postState !is PostStoryHandler.PostState.SavingToFirestore
            )
        }

        // Progress indicator
        when (postState) {
            is PostStoryHandler.PostState.UploadingImage -> {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = brightPink
                )
                Text(
                    text = "Uploading image...",
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
            is PostStoryHandler.PostState.ImageUploadProgress -> {
                val progress = postState.bytes.toFloat() / postState.totalBytes.toFloat()
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth(),
                    color = brightPink
                )
                Text(
                    text = "Uploading image: ${(progress * 100).toInt()}%",
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
            is PostStoryHandler.PostState.SavingToFirestore -> {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = brightPink
                )
                Text(
                    text = "Saving your story...",
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
            is PostStoryHandler.PostState.Error -> {
                Text(
                    text = postState.message,
                    color = Color.Red,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
            else -> {
                // No progress indicator needed
            }
        }

        Button(
            onClick = {
                val currentLocation = getLocation()
                Log.d("PostStoryScreen", "Creating post with location: $currentLocation")

                postHandler.createPost(
                    title = title,
                    description = description,
                    imageUri = selectedImageUri,
                    location = currentLocation
                )
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isPostEnabled) brightPink else buttonGray,
                contentColor = if (isPostEnabled) Color.White else Color.Gray
            ),
            enabled = isPostEnabled
        ) {
            when (postState) {
                is PostStoryHandler.PostState.UploadingImage,
                is PostStoryHandler.PostState.ImageUploadProgress,
                is PostStoryHandler.PostState.SavingToFirestore -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                else -> { /* No loading indicator */ }
            }

            val buttonText = when (postState) {
                is PostStoryHandler.PostState.UploadingImage,
                is PostStoryHandler.PostState.ImageUploadProgress -> "Uploading..."
                is PostStoryHandler.PostState.SavingToFirestore -> "Saving..."
                else -> "Post Story"
            }
            Text(buttonText)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}