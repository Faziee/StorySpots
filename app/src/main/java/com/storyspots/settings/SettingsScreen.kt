package com.storyspots.settings

import android.util.Log
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.storyspots.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    var showChangeUsernameDialog by remember { mutableStateOf(false) }
    var showChangeEmailDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    // Mock user data - replace with actual user data from your auth system
    var username by remember { mutableStateOf("Pookies") }
    var userEmail by remember { mutableStateOf("pookies@gmail.com") }
    var profileImageUrl by remember { mutableStateOf("") }

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
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Pink80)
                            .border(3.dp, Pink80, CircleShape)
                            .clickable { /* Handle profile picture change */ },
                        contentAlignment = Alignment.Center
                    ) {
                        if (profileImageUrl.isNotEmpty()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(profileImageUrl)
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

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = username,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkText
                    )

                    Text(
                        text = userEmail,
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
                        subtitle = username,
                        onClick = { showChangeUsernameDialog = true }
                    )

                    Divider(color = LightGray, thickness = 1.dp)

                    SettingsItem(
                        icon = Icons.Default.Email,
                        title = "Change Email",
                        subtitle = userEmail,
                        onClick = { showChangeEmailDialog = true }
                    )

                    Divider(color = LightGray, thickness = 1.dp)

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
                        subtitle = "Update your avatar",
                        onClick = {
                            // Handle profile picture change
                            // You can implement image picker here
                        }
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
                            // Handle logout
                            FirebaseAuth.getInstance().signOut()
                        },
                        textColor = Pink40
                    )

                    Divider(color = LightGray, thickness = 1.dp)

                    SettingsItem(
                        icon = Icons.Default.Delete,
                        title = "Delete Account",
                        subtitle = "Permanently delete your account",
                        onClick = { showDeleteAccountDialog = true },
                        textColor = DarkText
                    )
                }
            }
        }

        item {
            // Test button (keep your friend's test functionality)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = LightPink),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Send Test Notification",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = DarkText
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { sendTestNotification() },
                        colors = ButtonDefaults.buttonColors(containerColor = Pink),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Send Test Notification", color = White)
                    }
                }
            }
        }
    }

    // Dialogs
    if (showChangeUsernameDialog) {
        ChangeUsernameDialog(
            currentUsername = username,
            onDismiss = { showChangeUsernameDialog = false },
            onConfirm = { newUsername ->
                username = newUsername
                showChangeUsernameDialog = false
                // Implement username change logic
            }
        )
    }

    if (showChangeEmailDialog) {
        ChangeEmailDialog(
            currentEmail = userEmail,
            onDismiss = { showChangeEmailDialog = false },
            onConfirm = { newEmail ->
                userEmail = newEmail
                showChangeEmailDialog = false
                // Implement email change logic
            }
        )
    }

    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showChangePasswordDialog = false },
            onConfirm = { oldPassword, newPassword ->
                showChangePasswordDialog = false
                // Implement password change logic
            }
        )
    }

    if (showDeleteAccountDialog) {
        DeleteAccountDialog(
            onDismiss = { showDeleteAccountDialog = false },
            onConfirm = {
                showDeleteAccountDialog = false
                // Implement account deletion logic
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
    onConfirm: (String) -> Unit
) {
    var newEmail by remember { mutableStateOf(currentEmail) }

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
                    "Enter your new email address:",
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
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(newEmail) },
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
    onConfirm: () -> Unit
) {
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
            Text(
                "Are you sure you want to delete your account? This action cannot be undone and all your data will be permanently lost.",
                color = DarkText
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
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

// Keep your original test notification function
fun sendTestNotification() {
    val userId = FirebaseAuth.getInstance().uid.orEmpty()
    val db = FirebaseFirestore.getInstance()

    val fromRef = db.collection("user").document(userId)
    val storyId = "test_story_123"
    val storyRef = db.collection("story").document(storyId)

    val notificationId = db.collection("notification").document().id

    val testNotification = hashMapOf(
        "id" to notificationId,
        "title" to "Dev Test",
        "message" to "This is a test from button",
        "created_at" to Timestamp.now(),
        "read" to false,
        "from" to fromRef,
        "story" to storyRef
    )

    db.collection("notification").document(notificationId)
        .set(testNotification)
        .addOnSuccessListener { Log.d("SettingsScreen", "Test notification sent.") }
        .addOnFailureListener { e -> Log.e("SettingsScreen", "Failed to send test notification", e) }
}