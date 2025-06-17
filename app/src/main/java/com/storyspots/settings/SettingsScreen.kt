package com.storyspots.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.storyspots.ui.theme.*
import kotlinx.coroutines.launch
import com.google.firebase.Timestamp
import com.storyspots.login.LoginActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(settingsViewModel: SettingsViewModel = SettingsViewModel() )  {
    var showChangeUsernameDialog by remember { mutableStateOf(false) }
    var showChangeEmailDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showChangeProfilePictureDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var imageRefreshTrigger by remember { mutableIntStateOf(0) }
    val isUploading by settingsViewModel.isUploadingImage.collectAsState()
    val uploadedUrl by settingsViewModel.uploadedImageUrl.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    settingsViewModel.initializeCloudinaryService(context)

    // User data state
    var userData by remember { mutableStateOf(UserData()) }

    // Load user data on composition
    LaunchedEffect(Unit) {
        isLoading = true
        userData = settingsViewModel.loadUserData()
        isLoading = false
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Header with profile section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = UltraLightPink),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile Picture
                    key("profile_image_$imageRefreshTrigger") { // Key based on trigger
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Pink80)
                                .border(3.dp, Pink80, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (userData.profileImageUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data("${userData.profileImageUrl}?refresh=${imageRefreshTrigger}")
                                        .crossfade(true)
                                        .memoryCachePolicy(CachePolicy.DISABLED)
                                        .diskCachePolicy(CachePolicy.DISABLED)
                                        .build(),
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = "Default Profile",
                                    modifier = Modifier.size(40.dp),
                                    tint = Pink40
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = userData.username,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkText
                    )

                    Text(
                        text = userData.email,
                        fontSize = 14.sp,
                        color = MediumText
                    )
                }
            }
        }

        item {
            // Settings Options
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SettingsItem(
                        icon = Icons.Default.Person,
                        title = "Change Username",
                        subtitle = userData.username,
                        onClick = { showChangeUsernameDialog = true }
                    )

                    Divider(color = LightGray, thickness = 1.dp)

//                    SettingsItem(
//                        icon = Icons.Default.Email,
//                        title = "Change Email",
//                        subtitle = userData.email,
//                        onClick = { showChangeEmailDialog = true }
//                    )

//                    Divider(color = LightGray, thickness = 1.dp)

                    SettingsItem(
                        icon = Icons.Default.Lock,
                        title = "Change Password",
                        subtitle = "••••••••",
                        onClick = { showChangePasswordDialog = true }
                    )

                    Divider(color = LightGray, thickness = 1.dp)

                    SettingsItem(
                        icon = Icons.Default.AccountBox,
                        title = "Change Profile Picture",
                        subtitle = "Update your picture",
                        onClick = { showChangeProfilePictureDialog = true }
                    )
                }
            }
        }

        item {
            // Danger Zone
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SettingsItem(
                        icon = Icons.Default.ExitToApp,
                        title = "Logout",
                        subtitle = "Sign out of your account",
                        onClick = {
                            scope.launch {
                                try {
                                    isLoading = true
                                    settingsViewModel.logout(context)

                                    val intent = Intent(context, LoginActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    }
                                    context.startActivity(intent)

                                    (context as? Activity)?.finish()

                                } catch (e: Exception) {
                                    Log.e("LogOut", e.toString())
                                    isLoading = false
                                }
                            }
                        },
                        textColor = Pink40
                    )

                    Divider(color = LightGray, thickness = 1.dp)

                    SettingsItem(
                        icon = Icons.Default.Delete,
                        title = "Delete Account",
                        subtitle = "Permanently delete your account",
                        textColor = DarkText,
                        onClick = {
                            showDeleteAccountDialog = true
                        }
                    )
                }
            }
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Pink)
        }
    }

    // Dialogs

    if (showChangeProfilePictureDialog) {
        ChangeProfilePictureDialog(
            currentImageUrl = userData.profileImageUrl,
            onDismiss = { showChangeProfilePictureDialog = false },
            onConfirm = { selectedUri ->
                scope.launch {
                    isLoading = true
                    try {
                        val newImageUrl = settingsViewModel.updateProfileImage(selectedUri, userData.userId, context)
                        newImageUrl?.let { url ->
                            userData = userData.copy(profileImageUrl = url.toString())
                            imageRefreshTrigger++ // Trigger immediate refresh
                            Log.d("ProfileUpdate", "Updated image URL: $url, trigger: $imageRefreshTrigger")
                        }
                    } catch (e: Exception) {
                        Log.e("ProfileUpdate", "Error updating profile image", e)
                    } finally {
                        isLoading = false
                        showChangeProfilePictureDialog = false
                    }
                }
            }
        )
    }


    if (showChangeUsernameDialog) {
        ChangeUsernameDialog(
            currentUsername = userData.username,
            onDismiss = { showChangeUsernameDialog = false },
            onConfirm = { newUsername ->
                scope.launch {
                    isLoading = true
                    val result = settingsViewModel.changeUsername(userData.userId, newUsername, context)
                    if (result is SettingsResult.Success) {
                        userData = userData.copy(username = newUsername)
                    }
                    isLoading = false
                    showChangeUsernameDialog = false
                }
            }
        )
    }

//    if (showChangeEmailDialog) {
//        ChangeEmailDialog(
//            currentEmail = userData.email,
//            onDismiss = { showChangeEmailDialog = false },
//            onConfirm = { newEmail, password ->
//                scope.launch {
//                    isLoading = true
//                    val result = settingsViewModel.changeEmail(newEmail, password, context)
//                    if (result is SettingsResult.Success) {
//                        userData = userData.copy(email = newEmail)
//                    }
//                    isLoading = false
//                    showChangeEmailDialog = false
//                }
//            }
//        )
//    }

    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showChangePasswordDialog = false },
            onConfirm = { oldPassword, newPassword ->
                scope.launch {
                    isLoading = true
                    settingsViewModel.changePassword(oldPassword, newPassword, context)
                    isLoading = false
                    showChangePasswordDialog = false
                }
            }
        )
    }

    if (showDeleteAccountDialog) {
        DeleteAccountDialog(
            onDismiss = { showDeleteAccountDialog = false },
            onConfirm = { password ->
                scope.launch {
                    isLoading = true
                    val result = settingsViewModel.deleteAccount(password, context)
                    isLoading = false
                    showDeleteAccountDialog = false

                    val intent = Intent(context, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    context.startActivity(intent)

                    (context as? Activity)?.finish()
                }
            }
        )
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    textColor: Color = DarkText
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = textColor,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = textColor
            )

            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = LightText
            )
        }

        Icon(
            Icons.Default.KeyboardArrowRight,
            contentDescription = "Arrow",
            tint = LightText
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeProfilePictureDialog(
    currentImageUrl: String,
    onDismiss: () -> Unit,
    onConfirm: (Uri) -> Unit
) {
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = White,
        title = {
            Text(
                "Change Profile Picture",
                fontWeight = FontWeight.Bold,
                color = DarkText
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Select a new profile picture:",
                    color = MediumText
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Current or selected image preview
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Pink80)
                        .border(3.dp, Pink80, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        selectedImageUri != null -> {
                            // Show selected image
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(selectedImageUri)
                                    .build(),
                                contentDescription = "Selected Profile Picture",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        currentImageUrl.isNotEmpty() -> {
                            // Show current image
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(currentImageUrl)
                                    .build(),
                                contentDescription = "Current Profile Picture",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        else -> {
                            // Show default icon
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Default Profile",
                                modifier = Modifier.size(60.dp),
                                tint = Pink40
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Button to select image
                Button(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = Pink40),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.PhotoCamera,
                        contentDescription = "Select Photo",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedImageUri != null) "Change Selection" else "Select Photo",
                        color = White
                    )
                }

                // Show selected image info
                selectedImageUri?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "✓ New image selected",
                        color = Pink,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedImageUri?.let { uri ->
                        onConfirm(uri)
                    }
                },
                enabled = selectedImageUri != null,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (selectedImageUri != null) Pink else MediumText
                )
            ) {
                Text("Update Picture")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = MediumText)
            ) {
                Text("Cancel")
            }
        }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeUsernameDialog(
    currentUsername: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newUsername by remember { mutableStateOf(currentUsername) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = White,
        title = {
            Text(
                "Change Username",
                fontWeight = FontWeight.Bold,
                color = DarkText
            )
        },
        text = {
            Column {
                Text(
                    "Enter your new username:",
                    color = MediumText
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newUsername,
                    onValueChange = { newUsername = it },
                    label = { Text("New Username") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Pink,
                        focusedLabelColor = Pink
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(newUsername) },
                colors = ButtonDefaults.textButtonColors(contentColor = Pink)
            ) {
                Text("Change")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = MediumText)
            ) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeEmailDialog(
    currentEmail: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var newEmail by remember { mutableStateOf(currentEmail) }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = White,
        title = {
            Text(
                "Change Email",
                fontWeight = FontWeight.Bold,
                color = DarkText
            )
        },
        text = {
            Column {
                Text(
                    "Enter your new email address and current password:",
                    color = MediumText
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newEmail,
                    onValueChange = { newEmail = it },
                    label = { Text("New Email") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Pink,
                        focusedLabelColor = Pink
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Current Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Pink,
                        focusedLabelColor = Pink
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(newEmail, password) },
                enabled = newEmail.isNotBlank() && password.isNotBlank() && newEmail != currentEmail,
                colors = ButtonDefaults.textButtonColors(contentColor = Pink)
            ) {
                Text("Change")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = MediumText)
            ) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = White,
        title = {
            Text(
                "Change Password",
                fontWeight = FontWeight.Bold,
                color = DarkText
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = { oldPassword = it },
                    label = { Text("Current Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Pink,
                        focusedLabelColor = Pink
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Pink,
                        focusedLabelColor = Pink
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm New Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Pink,
                        focusedLabelColor = Pink
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(oldPassword, newPassword) },
                enabled = newPassword == confirmPassword && newPassword.isNotEmpty() && oldPassword.isNotEmpty(),
                colors = ButtonDefaults.textButtonColors(contentColor = Pink)
            ) {
                Text("Change")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = MediumText)
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DeleteAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = White,
        title = {
            Text(
                "Delete Account",
                fontWeight = FontWeight.Bold,
                color = DarkText
            )
        },
        text = {
            Column {
                Text(
                    "Are you sure you want to delete your account? This action cannot be undone and all your data will be permanently lost.",
                    color = DarkText
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Enter your password to confirm:",
                    color = MediumText
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Pink,
                        focusedLabelColor = Pink
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(password) },
                colors = ButtonDefaults.textButtonColors(contentColor = DarkText)
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = MediumText)
            ) {
                Text("Cancel")
            }
        }
    )
}