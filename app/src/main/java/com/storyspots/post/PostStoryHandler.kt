package com.storyspots.services.post

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.storyspots.services.cloudinary.CloudinaryService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import java.util.*

class PostStoryHandler(private val context: Context) {

    private val cloudinaryService = CloudinaryService(context)
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    sealed class PostState {
        object Idle : PostState()
        object UploadingImage : PostState()
        data class ImageUploadProgress(val bytes: Long, val totalBytes: Long) : PostState()
        object SavingToFirestore : PostState()
        object Success : PostState()
        data class Error(val message: String) : PostState()
    }

    private val _postState = MutableStateFlow<PostState>(PostState.Idle)
    val postState: StateFlow<PostState> = _postState.asStateFlow()

    data class PostData(
        val title: String,
        val description: String,
        val imageUri: Uri?,
        val location: GeoPoint? = null
    )

    fun createPost(
        title: String,
        description: String,
        imageUri: Uri?,
        location: GeoPoint? = null
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _postState.value = PostState.Error("User not logged in")
            return
        }

        val postData = PostData(title, description, imageUri, location)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                _postState.value = PostState.UploadingImage

                if (imageUri != null) {
                    // Upload image to Cloudinary first
                    uploadImageAndCreatePost(postData)
                } else {
                    // Create post without image
                    savePostToFirestore(postData, null)
                }

            } catch (e: Exception) {
                _postState.value = PostState.Error("Failed to create post: ${e.message}")
            }
        }
    }

    private suspend fun uploadImageAndCreatePost(postData: PostData) {
        cloudinaryService.uploadImageToCloudinary(postData.imageUri)

        cloudinaryService.uploadState.collect { uploadState ->
            when (uploadState) {
                is CloudinaryService.UploadState.Loading -> {
                    _postState.value = PostState.UploadingImage
                }

                is CloudinaryService.UploadState.Progress -> {
                    _postState.value = PostState.ImageUploadProgress(
                        uploadState.bytes,
                        uploadState.totalBytes
                    )
                }

                is CloudinaryService.UploadState.Success -> {
                    savePostToFirestore(postData, uploadState.url)
                }

                is CloudinaryService.UploadState.Error -> {
                    _postState.value = PostState.Error("Image upload failed: ${uploadState.message}")
                }

                else -> {
                    // Handle other states if needed
                }
            }
        }
    }

    private fun savePostToFirestore(postData: PostData, imageUrl: String?) {
        _postState.value = PostState.SavingToFirestore

        val currentUser = auth.currentUser
        if (currentUser == null) {
            _postState.value = PostState.Error("User not logged in")
            return
        }

        val documentId = generateUniqueId()

        val postDocument = hashMapOf(
            "caption" to postData.description,
            "created_at" to Date(),
            "image_url" to (imageUrl ?: ""),
            "location" to postData.location,
            "title" to postData.title,
            "user" to "/user/${currentUser.uid}"
        )

        firestore.collection("story_test")
            .document(documentId)
            .set(postDocument)
            .addOnSuccessListener {
                _postState.value = PostState.Success
            }
            .addOnFailureListener { exception ->
                _postState.value = PostState.Error("Failed to save post: ${exception.message}")
            }
    }

    private fun generateUniqueId(): String {
        return UUID.randomUUID().toString().replace("-", "")
    }

    fun resetState() {
        _postState.value = PostState.Idle
    }
}