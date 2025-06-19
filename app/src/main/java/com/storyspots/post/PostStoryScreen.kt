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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.firestore.GeoPoint
import com.storyspots.R
import com.storyspots.ui.theme.*

@Composable
fun PostStoryScreen(
    onImageSelect: () -> Unit,
    selectedImageUri: Uri? = null,
    onPostSuccess: () -> Unit = {},
    modifier: Modifier = Modifier,
    getLocation: () -> GeoPoint? = { null }
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    val context = LocalContext.current
    val postHandler = remember { PostStoryHandler(context) }
    val postState = postHandler.postState.collectAsState().value

    val isPostEnabled = title.isNotBlank() && description.isNotBlank() &&
            postState !is PostStoryHandler.PostState.UploadingImage &&
            postState !is PostStoryHandler.PostState.SavingToFirestore

    LaunchedEffect(postState) {
        when (postState) {
            is PostStoryHandler.PostState.Success -> {
                Log.d("PostStoryScreen", "Post successful!")
                onPostSuccess()
                postHandler.resetState()
            }

            is PostStoryHandler.PostState.Error -> {
                val errorState = postState
                Log.e("PostStoryScreen", "Post error: ${errorState.message}")
            }

            else -> {
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
            .imePadding()
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Share Your Story",
                fontSize = 28.sp,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = DarkText
                ),
                modifier = Modifier.padding(bottom = 3.dp)
            )

            Box(
                modifier = Modifier
                    .shadow(4.dp, RoundedCornerShape(8.dp), clip = true)
                    .background(White, shape = RoundedCornerShape(8.dp))
                    .border(1.dp, Authentication, RoundedCornerShape(8.dp))
                    .fillMaxWidth()
                    .height(200.dp)
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
                            colorFilter = ColorFilter.tint(LightGray)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Tap to select photo", color = LightGray)
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Story Title") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Authentication,
                    unfocusedBorderColor = HintGray,
                    focusedLabelColor = Authentication,
                    cursorColor = Authentication
                ),
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                enabled = postState !is PostStoryHandler.PostState.UploadingImage &&
                        postState !is PostStoryHandler.PostState.SavingToFirestore
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Story Description") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                maxLines = 4,
                shape = RoundedCornerShape(8.dp),
                enabled = postState !is PostStoryHandler.PostState.UploadingImage &&
                        postState !is PostStoryHandler.PostState.SavingToFirestore,
                modifier = Modifier.fillMaxSize().heightIn(min = 120.dp, max = 200.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Authentication,
                    unfocusedBorderColor = HintGray,
                    focusedLabelColor = Authentication,
                    cursorColor = Authentication
                )
            )

            when (postState) {
                is PostStoryHandler.PostState.UploadingImage -> {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = Authentication
                    )
                    Text(
                        text = "Uploading image...",
                        color = LightGray,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                is PostStoryHandler.PostState.ImageUploadProgress -> {
                    val progress = postState.bytes.toFloat() / postState.totalBytes.toFloat()
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier.fillMaxWidth(),
                        color = Authentication
                    )
                    Text(
                        text = "Uploading image: ${(progress * 100).toInt()}%",
                        color = LightGray,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                is PostStoryHandler.PostState.SavingToFirestore -> {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = Authentication
                    )
                    Text(
                        text = "Saving your story...",
                        color = LightGray,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                is PostStoryHandler.PostState.Error -> {
                    Text(
                        text = postState.message,
                        color = ErrorColour,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                else -> {

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
                    containerColor = if (isPostEnabled) Authentication else LightGray,
                    contentColor = if (isPostEnabled) White else LightGray
                ),
                enabled = isPostEnabled
            ) {
                when (postState) {
                    is PostStoryHandler.PostState.UploadingImage,
                    is PostStoryHandler.PostState.ImageUploadProgress,
                    is PostStoryHandler.PostState.SavingToFirestore -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    else -> {
                    }
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
}