package com.storyspots.ui

import com.storyspots.navigation.BottomNavBar
import NotificationFeedScreen
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.GeoPoint
import com.storyspots.core.StorySpot
import com.storyspots.core.AppComponents
import com.storyspots.core.managers.NavigationScreen
import com.storyspots.post.PostStoryScreen
import com.storyspots.settings.SettingsScreen
import com.storyspots.yourFeed.YourFeedScreen

@Composable
fun StorySpotsApp() {
    val isAppReady by remember {
        derivedStateOf { StorySpot.isFullyInitialized }
    }

    val hasLocationPermission by AppComponents.permissionManager.hasLocationPermission.collectAsState()
    val currentScreen by AppComponents.navigationManager.currentScreen.collectAsState()
    val imageSelectionState by AppComponents.imageSelectionManager.selectionState.collectAsState()

    val context = LocalContext.current

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        AppComponents.imageSelectionManager.setSelectedImage(uri)
        AppComponents.imageSelectionManager.setPendingSelection(false)
    }

    // Media permission launcher
    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Permission granted! Select your image.", Toast.LENGTH_SHORT).show()
            AppComponents.imageSelectionManager.setPendingSelection(true)
        } else {
            Toast.makeText(context, "Permission denied. Cannot access photos.", Toast.LENGTH_LONG).show()
            AppComponents.imageSelectionManager.setPendingSelection(false)
        }
    }

    // Auto-launch image picker when permission is granted
    LaunchedEffect(imageSelectionState.pendingSelection) {
        if (imageSelectionState.pendingSelection) {
            imagePickerLauncher.launch("image/*")
        }
    }

    // Show splash screen while initializing
    if (!isAppReady) {
        SplashScreen()
        return
    }

    Scaffold(
        bottomBar = {
            BottomNavBar(
                currentRoute = currentScreen,
                onItemClick = { item ->
                    AppComponents.navigationManager.navigate(item)
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentScreen) {
                NavigationScreen.HOME -> MapScreen()
                NavigationScreen.YOUR_FEED -> YourFeedScreen()
                NavigationScreen.NOTIFICATIONS -> NotificationFeedScreen()
                NavigationScreen.SETTINGS -> SettingsScreen()
                NavigationScreen.CREATE -> {
                    PostStoryScreen(
                        onImageSelect = {
                            // Check permission before opening gallery
                            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                Manifest.permission.READ_MEDIA_IMAGES
                            } else {
                                Manifest.permission.READ_EXTERNAL_STORAGE
                            }

                            when (PackageManager.PERMISSION_GRANTED) {
                                ContextCompat.checkSelfPermission(context, permission) -> {
                                    // Permission already granted, open gallery
                                    imagePickerLauncher.launch("image/*")
                                }
                                else -> {
                                    // Request permission
                                    mediaPermissionLauncher.launch(permission)
                                }
                            }
                        },
                        selectedImageUri = imageSelectionState.selectedImageUri,
                        onPostSuccess = {
                            Toast.makeText(
                                context,
                                "Story posted successfully!",
                                Toast.LENGTH_SHORT
                            ).show()
                            AppComponents.imageSelectionManager.clearSelection()
                            AppComponents.navigationManager.navigateToHome()
                            AppComponents.refreshStories()
                        },
                        getLocation = {
                            val currentLocation = AppComponents.locationManager.currentLocation.value
                            currentLocation?.let { point ->
                                GeoPoint(point.latitude(), point.longitude())
                            } ?: run {
                                Toast.makeText(context, "Location not available", Toast.LENGTH_SHORT).show()
                                null
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            DisplayProgressBar()

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Loading StorySpots...",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}