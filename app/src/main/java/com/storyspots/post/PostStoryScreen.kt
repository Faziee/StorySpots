package com.storyspots.post

import android.net.Uri
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.storyspots.R

@Composable
fun PostStoryScreen(
    onImageSelect: () -> Unit,
    onPostClick: (String, String, Uri?) -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val brightPink = Color(0xFFFF9CC7)
    val lightGray = Color(0xFFF5F5F5)
    val buttonGray = Color(0xFFE0E0E0)

    val isPostEnabled = title.isNotBlank() && description.isNotBlank()

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
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.Black
            ),
            modifier = Modifier.padding(bottom = 3.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .border(0.7.dp, brightPink, RoundedCornerShape(8.dp))
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
                .border(0.7.dp, brightPink, RoundedCornerShape(8.dp))
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
                singleLine = true
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(0.7.dp, brightPink, RoundedCornerShape(8.dp))
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
                shape = RoundedCornerShape(8.dp)
            )
        }
        
        Button(
            onClick = { onPostClick(title, description, selectedImageUri) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isPostEnabled) brightPink else buttonGray,
                contentColor = if (isPostEnabled) Color.White else Color.Gray
            ),
            enabled = isPostEnabled
        ) {
            Text("Post Story")
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
