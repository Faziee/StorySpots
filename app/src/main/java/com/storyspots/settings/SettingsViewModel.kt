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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlinx.coroutines.tasks.await
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

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
) : ViewModel(){

    private var cloudinaryService: CloudinaryService? = null

    // Add StateFlow for UserData
    private val _userData = MutableStateFlow(UserData())
    val userData: StateFlow<UserData> = _userData.asStateFlow()

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
                        // Don't set loading to false here - let uploadProfileImage handle it
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

    // Initialize and load user data
    fun loadUserData() {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    val userDoc = firestore.collection("users").document(currentUser.uid).get().await()
                    val userData = UserData(
                        username = userDoc.getString("username") ?: "User",
                        email = currentUser.email ?: "",
                        profileImageUrl = userDoc.getString("profileImageUrl") ?: "",
                        userId = currentUser.uid
                    )
                    _userData.value = userData
                } else {
                    _userData.value = UserData()
                }
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error loading user data", e)
                _userData.value = UserData()
            }
        }
    }

    suspend fun changeUsername(userId: String, newUsername: String, context: Context): SettingsResult {
        return try {
            firestore.collection("users").document(userId)
                .update("username", newUsername)
                .await()

            // Update local state
            _userData.value = _userData.value.copy(username = newUsername)

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

                // Update local state
                _userData.value = _userData.value.copy(email = newEmail)

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

    // Fixed upload method - simplified and more reliable
    suspend fun uploadProfileImage(imageUri: Uri, userId: String, context: Context): String? {
        return try {
            // Check if image is currently uploading
            if (_isUploadingImage.value) {
                Toast.makeText(context, "Please wait for current upload to complete", Toast.LENGTH_SHORT).show()
                return null
            }

            Log.d("SettingsViewModel", "Starting profile image upload")

            // Manually set loading state
            _isUploadingImage.value = true

            // Start upload to Cloudinary
            uploadImageToCloudinary(imageUri)

            // Wait for upload to complete with timeout
            val uploadResult = withTimeoutOrNull(30000L) {
                suspendCancellableCoroutine<String?> { continuation ->
                    val job = viewModelScope.launch {
                        cloudinaryService?.uploadState?.collectLatest { uploadState ->
                            when (uploadState) {
                                is CloudinaryService.UploadState.Success -> {
                                    Log.d("SettingsViewModel", "Upload successful: ${uploadState.url}")
                                    if (continuation.isActive) {
                                        continuation.resume(uploadState.url)
                                    }
                                }
                                is CloudinaryService.UploadState.Error -> {
                                    //Log.e("SettingsViewModel", "Upload failed: ${uploadState.error}")
                                    if (continuation.isActive) {
                                        continuation.resume(null)
                                    }
                                }
                                else -> {
                                    // Continue waiting
                                }
                            }
                        }
                    }

                    continuation.invokeOnCancellation {
                        job.cancel()
                    }
                }
            }

            if (uploadResult != null) {
                try {
                    // Update Firestore with new image URL
                    firestore.collection("users").document(userId)
                        .update("profileImageUrl", uploadResult)
                        .await()

                    // Update local state with new image URL
                    _userData.value = _userData.value.copy(profileImageUrl = uploadResult)

                    Log.d("SettingsViewModel", "Profile image updated successfully")
                    Toast.makeText(context, "Profile picture updated successfully", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e("SettingsViewModel", "Error updating Firestore", e)
                    Toast.makeText(context, "Failed to save profile picture", Toast.LENGTH_LONG).show()
                    return null
                } finally {
                    // Always reset loading state
                    _isUploadingImage.value = false
                }
            } else {
                Toast.makeText(context, "Upload timed out or failed", Toast.LENGTH_LONG).show()
            }

            // Ensure loading state is reset
            _isUploadingImage.value = false
            uploadResult

        } catch (e: Exception) {
            Log.e("SettingsViewModel", "Error uploading profile image", e)
            Toast.makeText(context, "Failed to update profile picture: ${e.message}", Toast.LENGTH_LONG).show()
            null
        } finally {
            // Always ensure loading state is reset
            _isUploadingImage.value = false
        }
    }

    fun uploadImageToCloudinary(uri: Uri) {
        cloudinaryService?.uploadImageToCloudinary(uri)
    }

    fun logout(context: Context): SettingsResult {
        return try {
            auth.signOut()
            // Clear user data on logout
            _userData.value = UserData()
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