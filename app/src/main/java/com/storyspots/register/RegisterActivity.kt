// RegisterActivity.kt
package com.storyspots.register

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.storyspots.MainActivity

class RegisterActivity : ComponentActivity() {
    private val intentClass = MainActivity::class.java
//    private val intentClass = MainActivity::class.java

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val viewModel: RegisterViewModel = viewModel()

            RegisterScreen(
                viewModel = viewModel,
                onRegisterSuccess = {
                    startActivity(Intent(this, intentClass))
                    finish()
                },
                onNavigateToLogin = {
                    startActivity(Intent(this, intentClass))
                }
            )
        }
    }
}