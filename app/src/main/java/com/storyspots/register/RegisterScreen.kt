package com.storyspots.register

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.storyspots.R
import com.storyspots.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel,
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val passwordValidation by viewModel.passwordValidation.collectAsState()
    val uploadedImageUrl by viewModel.uploadedImageUrl.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.initializeCloudinaryService(context)
    }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.updateSelectedImage(uri)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Logo
        Image(
            painter = painterResource(id = R.drawable.logo_ss),
            contentDescription = "Logo",
            modifier = Modifier.size(200.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Title
        Text(
            text = "Register",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = DarkText
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Subtitle
        Text(
            text = "Create your account to get started",
            fontSize = 16.sp,
            color = MediumText,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Profile Picture Selection

        ProfilePictureSelector(
            selectedImageUri = uiState.selectedImageUri,
            uploadedImageUrl = uploadedImageUrl,
            isUploadingImage = uiState.isUploadingImage,
            onImageSelect = { imagePickerLauncher.launch("image/*") }
        )

        // Upload status text
        if (uiState.isUploadingImage) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Uploading image...",
                color = Authentication,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        } else if (uploadedImageUrl != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Image uploaded successfully!",
                color = SuccessColour,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Username Field
        OutlinedTextField(
            value = uiState.username,
            onValueChange = viewModel::updateUsername,
            label = { Text("Username") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Authentication,
                unfocusedBorderColor = HintGray,
                focusedLabelColor = Authentication,
                cursorColor = Authentication
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Email Field
        OutlinedTextField(
            value = uiState.email,
            onValueChange = viewModel::updateEmail,
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Authentication,
                unfocusedBorderColor = HintGray,
                focusedLabelColor = Authentication,
                cursorColor = Authentication
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Password Field
        OutlinedTextField(
            value = uiState.password,
            onValueChange = viewModel::updatePassword,
            label = { Text("Password") },
            visualTransformation = if (uiState.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = viewModel::togglePasswordVisibility) {
                    Icon(
                        imageVector = if (uiState.passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (uiState.passwordVisible) "Hide password" else "Show password"
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Authentication,
                unfocusedBorderColor = HintGray,
                focusedLabelColor = Authentication,
                cursorColor = Authentication
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Password Hints
        if (uiState.showPasswordHints) {
            PasswordHints(passwordValidation = passwordValidation)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Register Button
        Button(
            onClick = {
                viewModel.registerUser(context, onRegisterSuccess)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(horizontal = 32.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Authentication),
            enabled = !uiState.isLoading && !uiState.isUploadingImage
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = when {
                        uiState.isLoading -> "Creating Account..."
                        uiState.isUploadingImage -> "Uploading Image..."
                        else -> "Register"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Login Redirect
        Text(
            text = "Already have an account? Login here",
            color = Color.Black,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .clickable { onNavigateToLogin() }
                .padding(24.dp)
        )
    }
}

@Composable
fun ProfilePictureSelector(
    selectedImageUri: Uri?,
    uploadedImageUrl: String?,
    isUploadingImage: Boolean,
    onImageSelect: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(130.dp)
            .clip(CircleShape)
            .background(White)
            .clickable { onImageSelect() },
        contentAlignment = Alignment.Center
    ) {
        when {
            isUploadingImage -> {
                // Show upload progress
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Authentication,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            uploadedImageUrl != null -> {
                // Show uploaded image from Cloudinary
                AsyncImage(
                    model = uploadedImageUrl,
                    contentDescription = "Profile Picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            selectedImageUri != null -> {
                // Show selected image (while uploading)
                AsyncImage(
                    model = selectedImageUri,
                    contentDescription = "Profile Picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            else -> {
                // Default profile picture placeholder
                Box(
                    modifier = Modifier
                        .size(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(LightGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Default Profile Picture",
                            tint = HintGray,
                            modifier = Modifier.size(60.dp)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .offset(x = 30.dp, y = 30.dp)
                        .zIndex(1f)
                        .clip(CircleShape)
                        .background(Authentication),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Photo",
                        tint = White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PasswordHints(passwordValidation: PasswordValidation) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
    ) {
        HintText(
            text = "At least 6 characters",
            isValid = passwordValidation.lengthValid
        )
        HintText(
            text = "One uppercase letter",
            isValid = passwordValidation.uppercaseValid
        )
        HintText(
            text = "One special character",
            isValid = passwordValidation.specialCharValid
        )
    }
}

@Composable
fun HintText(
    text: String,
    isValid: Boolean
) {
    Text(
        text = "• $text",
        color = if (isValid) SuccessColour else ErrorColour,
        fontSize = 12.sp,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}