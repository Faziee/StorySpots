package com.storyspots.login

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import com.onesignal.OneSignal
import com.storyspots.utils.OneSignalManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false
)

class LoginViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            _uiState.value = _uiState.value.copy(isLoggedIn = true)
        }
    }

    fun updateEmail(email: String) {
        _uiState.value = _uiState.value.copy(email = email)
    }

    fun updatePassword(password: String) {
        _uiState.value = _uiState.value.copy(password = password)
    }

    fun togglePasswordVisibility() {
        _uiState.value = _uiState.value.copy(
            passwordVisible = !_uiState.value.passwordVisible
        )
    }

    fun loginUser(context: Context, onSuccess: () -> Unit) {
        val currentState = _uiState.value

        if (currentState.email.isEmpty() || currentState.password.isEmpty()) {
            Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        _uiState.value = currentState.copy(isLoading = true)

        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(currentState.email, currentState.password).await()

                // Setup OneSignal after successful Firebase login
                auth.currentUser?.let { user ->
                    try {
                        // Get username from Firestore
                        val userDoc = FirebaseFirestore.getInstance()
                            .collection("user")
                            .document(user.uid)
                            .get()
                            .await()

                        val username = userDoc.getString("username") ?: "Unknown"

                        // Use OneSignalManager to handle OneSignal login
                        OneSignalManager.loginUser(
                            userId = user.uid,
                            username = username,
                            email = user.email ?: ""
                        )

                        Log.d("LoginViewModel", "OneSignal setup successful for user: ${user.uid}")

                    } catch (e: Exception) {
                        Log.e("LoginViewModel", "Failed to setup OneSignal", e)
                        // Don't fail the entire login if OneSignal fails
                    }
                }

                Toast.makeText(context, "Login Successful", Toast.LENGTH_SHORT).show()
                _uiState.value = _uiState.value.copy(isLoggedIn = true)
                onSuccess()
            } catch (_: FirebaseAuthInvalidUserException) {
                Toast.makeText(context, "No account found with this email", Toast.LENGTH_LONG).show()
            } catch (_: FirebaseAuthInvalidCredentialsException) {
                Toast.makeText(context, "Invalid email or password", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Login Failed: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}