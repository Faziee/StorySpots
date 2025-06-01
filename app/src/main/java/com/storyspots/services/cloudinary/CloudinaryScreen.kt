package com.storyspots.services.cloudinary

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.storyspots.ui.DisplayProgressBar

//========================================== HOW TO USE==============================================//
/*                                        you're welcome ;)

@Composable
fun YourFunction() {
    var photoUrl: String? by remember { mutableStateOf(null) }

    CloudinaryScreen(
        onImageUploaded = { url ->
            photoUrl = url  // You get the image URL here.
        }
    )

    // Now photoUrl has the value and you can store it in the database as part of the post document.
    photoUrl?.let { url ->
        // Pass to any other class/function
        SomeOtherClass.doSomethingWithUrl(url)
    }
}

*/
//===================================================================================================//


@Composable
fun CloudinaryScreen(
    triggerImagePicker: Boolean = false,
    onImagePickerTriggered: () -> Unit = {},
    onImageUploaded: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val cloudinaryService = remember { CloudinaryService(context) }
    val uploadState by cloudinaryService.uploadState.collectAsStateWithLifecycle(
        initialValue = CloudinaryService.UploadState.Idle
    )

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        cloudinaryService.uploadImageToCloudinary(uri)
    }

    // Trigger image picker when requested from outside
    LaunchedEffect(triggerImagePicker) {
        if (triggerImagePicker) {
            imagePickerLauncher.launch("image/*")
            onImagePickerTriggered()
        }
    }

    // Show toast messages
    LaunchedEffect(uploadState) {
        when (val state = uploadState) {
            is CloudinaryService.UploadState.Success -> {
                Toast.makeText(context, "Image uploaded successfully!", Toast.LENGTH_SHORT).show()
                onImageUploaded(state.url)
            }
            is CloudinaryService.UploadState.Error -> {
                Toast.makeText(context, "Upload Error: ${state.message}", Toast.LENGTH_LONG).show()
            }
            else -> {}
        }
    }

    CloudinaryContent(uploadState = uploadState)
}

@Composable
private fun ShowProgressBar(text: String) {
    DisplayProgressBar()
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 16.dp)
    )
}

@Composable
private fun ShowErrorState(uploadState: CloudinaryService.UploadState.Error) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Text(
            text = "Error: ${uploadState.message}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(16.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CloudinaryContent(
    uploadState: CloudinaryService.UploadState
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (val state = uploadState) {
            is CloudinaryService.UploadState.Loading -> {
                ShowProgressBar("Uploading...")
            }
            is CloudinaryService.UploadState.Progress -> {
                val progress = (state.bytes * 100 / state.totalBytes).toInt()
                ShowProgressBar("Uploading... $progress%")
            }
            is CloudinaryService.UploadState.Success -> {
                // Do Nothing. PS: Success is handled in the LaunchedEffect above.
            }
            is CloudinaryService.UploadState.Error -> {
                ShowErrorState(state)
            }
            is CloudinaryService.UploadState.Idle -> {
                // Do Nothing.
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CloudinaryScreenPreview() {
    CloudinaryScreen(true)
}