package com.storyspots.pushNotification

import android.Manifest
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import kotlin.contracts.contract

@Composable
fun NotificationPermissionHandler(){
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
        val context = LocalContext.current
        val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission(), onResult =  { isGranted: Boolean -> Log.d("Permission", "Notification permission granted: $isGranted")
            }
        )
        LaunchedEffect(Unit){
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}