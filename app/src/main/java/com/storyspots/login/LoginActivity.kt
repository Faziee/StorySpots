package com.storyspots.login

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.storyspots.MainActivity
import com.storyspots.register.RegisterActivity

class LoginActivity : ComponentActivity() {
    private val intentClass = MainActivity::class.java

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val viewModel: LoginViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsState()

            // Comment out for always log in testing
            LaunchedEffect(uiState.isLoggedIn) {
                if (uiState.isLoggedIn) {
                    startActivity(Intent(this@LoginActivity, intentClass))
                    finish()
                }
            }

            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = {
                    startActivity(Intent(this, intentClass))
                    finish()
                },
                onNavigateToRegister = {
                    startActivity(Intent(this, RegisterActivity::class.java))
                }
            )
        }
    }
}