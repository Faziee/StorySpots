package com.storyspots.settings

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storyspots.services.cloudinary.CloudinaryService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class UserData(
    val username: String = "",
    val email: String = "",
    val profileImageUrl: String = "",
    val userId: String = ""
)

sealed class SettingsResult {
    object Success : SettingsResult()
    data class Error(val message: String) : SettingsResult()
}

class SettingsViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    //private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) : ViewModel(){

    private var cloudinaryService: CloudinaryService? = null

    private val _isUploadingImage = MutableStateFlow(false)
    val isUploadingImage: StateFlow<Boolean> = _isUploadingImage.asStateFlow()

    private val _uploadedImageUrl = MutableStateFlow<String?>(null)
    val uploadedImageUrl: StateFlow<String?> = _uploadedImageUrl.asStateFlow()

    fun initializeCloudinaryService(context: Context) {
        cloudinaryService = CloudinaryService(context)

        viewModelScope.launch {
            cloudinaryService?.uploadState?.collectLatest { uploadState ->
                when (uploadState) {
                    is CloudinaryService.UploadState.Loading,
                    is CloudinaryService.UploadState.Progress -> {
                        _isUploadingImage.value = true
                    }
                    is CloudinaryService.UploadState.Success -> {
                        _uploadedImageUrl.value = uploadState.url
                        _isUploadingImage.value = false
                    }
                    is CloudinaryService.UploadState.Error -> {
                        _isUploadingImage.value = false
                    }
                    is CloudinaryService.UploadState.Idle -> {
                        _isUploadingImage.value = false
                    }
                }
            }
        }
    }

    suspend fun loadUserData(): UserData {
        return try {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                val userDoc = firestore.collection("users").document(currentUser.uid).get().await()
                UserData(
                    username = userDoc.getString("username") ?: "User",
                    email = currentUser.email ?: "",
                    profileImageUrl = userDoc.getString("profileImageUrl") ?: "",
                    userId = currentUser.uid
                )
            } else {
                UserData()
            }
        } catch (e: Exception) {
            Log.e("SettingsViewModel", "Error loading user data", e)
            UserData()
        }
    }

    suspend fun changeUsername(userId: String, newUsername: String, context: Context): SettingsResult {
        return try {
            firestore.collection("users").document(userId)
                .update("username", newUsername)
                .await()

            Toast.makeText(context, "Username updated successfully", Toast.LENGTH_SHORT).show()
            SettingsResult.Success
        } catch (e: Exception) {
            Log.e("SettingsViewModel", "Error changing username", e)
            val errorMessage = "Failed to update username: ${e.message}"
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            SettingsResult.Error(errorMessage)
        }
    }

    suspend fun changeEmail(newEmail: String, password: String, context: Context): SettingsResult {
        return try {
            val user = auth.currentUser
            if (user != null && user.email != null) {
                // Re-authenticate user
                val credential = EmailAuthProvider.getCredential(user.email!!, password)
                user.reauthenticate(credential).await()

                // Update email in Firebase Auth
                user.updateEmail(newEmail).await()

                // Update email in Firestore
                firestore.collection("users").document(user.uid)
                    .update("email", newEmail)
                    .await()

                Toast.makeText(context, "Email updated successfully", Toast.LENGTH_SHORT).show()
                SettingsResult.Success
            } else {
                val errorMessage = "User not found"
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                SettingsResult.Error(errorMessage)
            }
        } catch (e: Exception) {
            Log.e("SettingsViewModel", "Error changing email", e)
            val errorMessage = "Failed to update email: ${e.message}"
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            SettingsResult.Error(errorMessage)
        }
    }

    suspend fun changePassword(oldPassword: String, newPassword: String, context: Context): SettingsResult {
        return try {
            val user = auth.currentUser
            if (user != null && user.email != null) {
                // Re-authenticate user
                val credential = EmailAuthProvider.getCredential(user.email!!, oldPassword)
                user.reauthenticate(credential).await()

                // Update password
                user.updatePassword(newPassword).await()

                Toast.makeText(context, "Password updated successfully", Toast.LENGTH_SHORT).show()
                SettingsResult.Success
            } else {
                val errorMessage = "User not found"
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                SettingsResult.Error(errorMessage)
            }
        } catch (e: Exception) {
            Log.e("SettingsViewModel", "Error changing password", e)
            val errorMessage = "Failed to update password: ${e.message}"
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            SettingsResult.Error(errorMessage)
        }
    }

    suspend fun deleteAccount(password: String, context: Context): SettingsResult {
        return try {
            val user = auth.currentUser
            if (user != null && user.email != null) {
                // Re-authenticate user
                val credential = EmailAuthProvider.getCredential(user.email!!, password)
                user.reauthenticate(credential).await()

                // Delete user data from Firestore
                firestore.collection("users").document(user.uid).delete().await()

                // Delete profile image from Storage
                try {
                    //storage.reference.child("profile_images/${user.uid}").delete().await()
                } catch (e: Exception) {
                    Log.w("SettingsViewModel", "No profile image to delete or error deleting", e)
                }

                // Delete user stories, notifications, etc.
                deleteUserRelatedData(user.uid)

                // Delete user account
                user.delete().await()

                Toast.makeText(context, "Account deleted successfully", Toast.LENGTH_SHORT).show()
                SettingsResult.Success

            } else {
                val errorMessage = "User not found"
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                SettingsResult.Error(errorMessage)
            }
        } catch (e: Exception) {
            Log.e("SettingsViewModel", "Error deleting account", e)
            val errorMessage = "Failed to delete account: ${e.message}"
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            SettingsResult.Error(errorMessage)
        }
    }

    private suspend fun deleteUserRelatedData(userId: String) {
        try {
            // Delete user's stories
            val storiesQuery = firestore.collection("story").whereEqualTo("user", userId).get().await()
            for (document in storiesQuery.documents) {
                document.reference.delete()
            }

            // Delete user's notifications
            val notificationsQuery = firestore.collection("notification").whereEqualTo("userId", userId).get().await()
            for (document in notificationsQuery.documents) {
                document.reference.delete()
            }

        } catch (e: Exception) {
            Log.e("SettingsViewModel", "Error deleting user related data", e)
        }
    }

    suspend fun uploadProfileImage(imageUri: Uri, userId: String, context: Context): String? {
        return try {
            // Check if image is currently uploading
            if (_isUploadingImage.value) {
                Toast.makeText(context, "Please wait for current upload to complete", Toast.LENGTH_SHORT).show()
                return null
            }

            // Start upload to Cloudinary
            uploadImageToCloudinary(imageUri)

            // Wait for upload to complete
            var uploadResult: String? = null
            cloudinaryService?.uploadState?.collectLatest { uploadState ->
                when (uploadState) {
                    is CloudinaryService.UploadState.Success -> {
                        uploadResult = uploadState.url

                        // Update Firestore with new Cloudinary image URL
                        firestore.collection("users").document(userId)
                            .update("profileImageUrl", uploadResult)
                            .await()

                        Toast.makeText(context, "Profile picture updated successfully", Toast.LENGTH_SHORT).show()
                        return@collectLatest
                    }
                    is CloudinaryService.UploadState.Error -> {
                        Toast.makeText(context, "Failed to update profile picture", Toast.LENGTH_LONG).show()
                        return@collectLatest
                    }
                    else -> {
                        // Continue waiting for completion
                    }
                }
            }

            uploadResult
        } catch (e: Exception) {
            Log.e("SettingsViewModel", "Error uploading profile image", e)
            Toast.makeText(context, "Failed to update profile picture: ${e.message}", Toast.LENGTH_LONG).show()
            null
        }
    }

    fun uploadImageToCloudinary(uri: Uri) {
        cloudinaryService?.uploadImageToCloudinary(uri)
    }


    fun updateProfileImage(uri: Uri, userId: String, context: Context) {
        viewModelScope.launch {
            uploadProfileImage(uri, userId, context)
        }
    }

    fun logout(context: Context): SettingsResult {
        return try {
            auth.signOut()
            Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
            SettingsResult.Success
        } catch (e: Exception) {
            Log.e("SettingsViewModel", "Error logging out", e)
            val errorMessage = "Failed to logout: ${e.message}"
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            SettingsResult.Error(errorMessage)
        }
    }
}