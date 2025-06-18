package com.storyspots.register

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.core.net.toUri
import com.onesignal.OneSignal
import com.storyspots.services.cloudinary.CloudinaryService
import com.storyspots.utils.OneSignalManager
import kotlinx.coroutines.flow.collectLatest

data class RegisterUiState(
    val email: String = "",
    val username: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val selectedImageUri: Uri? = null,
    val isLoading: Boolean = false,
    val showPasswordHints: Boolean = false,
    val isUploadingImage: Boolean = false
)

data class PasswordValidation(
    val lengthValid: Boolean = false,
    val uppercaseValid: Boolean = false,
    val specialCharValid: Boolean = false
) {
    val isValid: Boolean get() = lengthValid && uppercaseValid && specialCharValid
}

class RegisterViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private var cloudinaryService: CloudinaryService? = null

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _passwordValidation = MutableStateFlow(PasswordValidation())
    val passwordValidation: StateFlow<PasswordValidation> = _passwordValidation.asStateFlow()

    private val _uploadedImageUrl = MutableStateFlow<String?>(null)
    val uploadedImageUrl: StateFlow<String?> = _uploadedImageUrl.asStateFlow()

    fun initializeCloudinaryService(context: Context) {
        cloudinaryService = CloudinaryService(context)

        // Observe upload state changes
        viewModelScope.launch {
            cloudinaryService?.uploadState?.collectLatest { uploadState ->
                when (uploadState) {
                    is CloudinaryService.UploadState.Loading,
                    is CloudinaryService.UploadState.Progress -> {
                        _uiState.value = _uiState.value.copy(isUploadingImage = true)
                    }
                    is CloudinaryService.UploadState.Success -> {
                        _uploadedImageUrl.value = uploadState.url
                        _uiState.value = _uiState.value.copy(isUploadingImage = false)
                    }
                    is CloudinaryService.UploadState.Error -> {
                        _uiState.value = _uiState.value.copy(isUploadingImage = false)
                        // Handle error - you might want to show a toast or update UI state
                    }
                    is CloudinaryService.UploadState.Idle -> {
                        _uiState.value = _uiState.value.copy(isUploadingImage = false)
                    }
                }
            }
        }
    }

    fun updateEmail(email: String) {
        _uiState.value = _uiState.value.copy(email = email)
    }

    fun updateUsername(username: String) {
        _uiState.value = _uiState.value.copy(username = username)
    }

    fun updatePassword(password: String) {
        _uiState.value = _uiState.value.copy(
            password = password,
            showPasswordHints = password.isNotEmpty()
        )
        validatePassword(password)
    }

    fun togglePasswordVisibility() {
        _uiState.value = _uiState.value.copy(
            passwordVisible = !_uiState.value.passwordVisible
        )
    }

    fun updateSelectedImage(uri: Uri?) {
        _uiState.value = _uiState.value.copy(selectedImageUri = uri)

        // Upload to Cloudinary when image is selected
        uri?.let {
            uploadImageToCloudinary(it)
        }
    }

    private fun uploadImageToCloudinary(uri: Uri) {
        cloudinaryService?.uploadImageToCloudinary(uri)
    }

    private fun validatePassword(password: String) {
        val lengthValid = password.length >= 6
        val uppercaseValid = password.any { it.isUpperCase() }
        val specialCharValid = password.any { "!@#\$%^&*()_+=<>?/".contains(it) }

        _passwordValidation.value = PasswordValidation(
            lengthValid = lengthValid,
            uppercaseValid = uppercaseValid,
            specialCharValid = specialCharValid
        )
    }

    fun registerUser(context: Context, onSuccess: () -> Unit) {
        val currentState = _uiState.value

        if (currentState.email.isEmpty() || currentState.password.isEmpty() || currentState.username.isEmpty()) {
            Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (!_passwordValidation.value.isValid) {
            Toast.makeText(
                context,
                "Password must be at least 6 characters, include one uppercase letter and one special character.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // Check if image is still uploading
        if (currentState.isUploadingImage) {
            Toast.makeText(context, "Please wait for image upload to complete", Toast.LENGTH_SHORT).show()
            return
        }

        _uiState.value = currentState.copy(isLoading = true)

        viewModelScope.launch {
            try {
                val authResult = auth.createUserWithEmailAndPassword(
                    currentState.email,
                    currentState.password
                ).await()

                val user = authResult.user
                if (user != null) {
                    // Use the uploaded Cloudinary URL or null for default
                    val profileImageUrl = _uploadedImageUrl.value

                    updateUserProfile(user.uid, currentState.username, profileImageUrl)

                    OneSignalManager.registerUser(userId = user.uid)
                    OneSignalManager.logCurrentStatus()

                    Toast.makeText(context, "Registration Successful", Toast.LENGTH_SHORT).show()
                    onSuccess()
                }
            } catch (e: FirebaseAuthUserCollisionException) {
                Toast.makeText(context, "Email already in use", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Registration Failed: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    private fun getDefaultProfileImageUrl(): String? {
        return null
    }

    private suspend fun updateUserProfile(userId: String, username: String, profileImageUrl: String?) {
        val user = auth.currentUser ?: return

        // Update Firebase Auth profile
        val profileUpdates = userProfileChangeRequest {
            displayName = username
            profileImageUrl?.let { photoUri = it.toUri() }
        }

        user.updateProfile(profileUpdates).await()

        // Save user data to Firestore
        val userData = hashMapOf(
            "username" to username,
            "email" to user.email,
            "profileImageUrl" to profileImageUrl,
            "createdAt" to com.google.firebase.Timestamp.now()
        )

        firestore.collection("users")
            .document(userId)
            .set(userData)
            .await()
    }
}