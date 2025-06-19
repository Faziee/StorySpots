package com.storyspots.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapbox.maps.MapView
import com.storyspots.caption.MapLoader
import com.storyspots.services.cloudinary.CloudinaryService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import com.storyspots.core.AppComponents
import com.storyspots.login.LoginActivity

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

    private val _isUploadingImage = MutableStateFlow(false)

    private val _uploadedImageUrl = MutableStateFlow<String?>(null)


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

                val credential = EmailAuthProvider.getCredential(user.email!!, password)

                user.reauthenticate(credential).await()

                user.verifyBeforeUpdateEmail(newEmail).await()

                firestore.collection("users").document(user.uid)
                    .update("email", newEmail)
                    .await()

                Toast.makeText(context, "Email updated successfully.", Toast.LENGTH_SHORT).show()

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

                val credential = EmailAuthProvider.getCredential(user.email!!, oldPassword)

                user.reauthenticate(credential).await()

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

                val credential = EmailAuthProvider.getCredential(user.email!!, password)

                user.reauthenticate(credential).await()

                firestore.collection("users").document(user.uid).delete().await()

                deleteUserRelatedData(user.uid)

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
            val storiesQuery = firestore.collection("story").whereEqualTo("user", userId).get().await()
            for (document in storiesQuery.documents) {
                document.reference.delete()
            }

            val notificationsQuery = firestore.collection("notification").whereEqualTo("from", userId).get().await()
            for (document in notificationsQuery.documents) {
                document.reference.delete()
            }

        } catch (e: Exception) {
            Log.e("SettingsViewModel", "Error deleting user related data", e)
        }
    }

    suspend fun updateProfileImage(imageUri: Uri, userId: String, context: Context): String? {
        return try {

            if (_isUploadingImage.value) {
                Toast.makeText(context, "Please wait for current upload to complete", Toast.LENGTH_SHORT).show()
                return null
            }

            uploadImageToCloudinary(imageUri)

            val uploadResult = suspendCancellableCoroutine<String?> { continuation ->
                val job = viewModelScope.launch {
                    cloudinaryService?.uploadState?.collectLatest { uploadState ->
                        when (uploadState) {
                            is CloudinaryService.UploadState.Success -> {
                                Log.d("ProfileUpdate", "Upload successful: ${uploadState.url}")

                                try {
                                    firestore.collection("users").document(userId)
                                        .update("profileImageUrl", uploadState.url)
                                        .await()

                                    Toast.makeText(context, "Profile picture updated successfully", Toast.LENGTH_SHORT).show()

                                    continuation.resume(uploadState.url)
                                } catch (e: Exception) {
                                    Log.e("ProfileUpdate", "Error updating Firestore", e)
                                    Toast.makeText(context, "Failed to save profile picture", Toast.LENGTH_LONG).show()
                                    continuation.resume(null)
                                }
                            }
                            is CloudinaryService.UploadState.Error -> {
                                Log.e("ProfileUpdate", "Upload failed: ${uploadState.message}")
                                Toast.makeText(context, "Failed to update profile picture: ${uploadState.message}", Toast.LENGTH_LONG).show()

                                continuation.resume(null)
                            }
                            else -> {
                                // Continue waiting for completion
                            }
                        }
                    }
                }

                continuation.invokeOnCancellation {
                    job.cancel()
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

    fun logout(context: Context, mainActivity: Activity) {
        try {
            auth.signOut()
            AppComponents.mapManager.cleanup()
            Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()

            val intent = Intent(context, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)

            mainActivity.finish()

        } catch (e: Exception) {
            Log.e("SettingsViewModel", "Error logging out", e)
            Toast.makeText(context, "Failed to logout: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

}