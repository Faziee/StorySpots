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
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import androidx.core.net.toUri

data class RegisterUiState(
    val email: String = "",
    val username: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val selectedImageUri: Uri? = null,
    val isLoading: Boolean = false,
    val showPasswordHints: Boolean = false
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
    private val storage = FirebaseStorage.getInstance()

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _passwordValidation = MutableStateFlow(PasswordValidation())
    val passwordValidation: StateFlow<PasswordValidation> = _passwordValidation.asStateFlow()

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

        _uiState.value = currentState.copy(isLoading = true)

        viewModelScope.launch {
            try {
                val authResult = auth.createUserWithEmailAndPassword(
                    currentState.email,
                    currentState.password
                ).await()

                val user = authResult.user
                if (user != null) {
                    val profileImageUrl = if (currentState.selectedImageUri != null) {
                        uploadProfileImage(currentState.selectedImageUri, user.uid)
                    } else {
                        getDefaultProfileImageUrl()
                    }

                    updateUserProfile(user.uid, currentState.username, profileImageUrl)

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

    private suspend fun uploadProfileImage(imageUri: Uri, userId: String): String? {
        return try {
            val imageRef = storage.reference.child("profile_images/$userId/${UUID.randomUUID()}")
            imageRef.putFile(imageUri).await()
            imageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            null
        }
    }

    private fun getDefaultProfileImageUrl(): String {
        // You can use a default image from your drawable resources
        // or a placeholder service like UI Avatars
        val username = _uiState.value.username
        return "https://ui-avatars.com/api/?name=${username}&background=6200ee&color=fff&size=200"
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
