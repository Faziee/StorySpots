package com.storyspots.utils

import android.content.Context
import android.util.Log
import com.onesignal.OneSignal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object OneSignalManager {

    fun initialize(context: Context, appId: String) {
        try {
            Log.d("OneSignalManager", "Initializing OneSignal with App ID: $appId")

            OneSignal.initWithContext(context, appId)

            OneSignal.Debug.logLevel = com.onesignal.debug.LogLevel.VERBOSE

            Log.d("OneSignalManager", "OneSignal initialization completed")

        } catch (e: Exception) {
            Log.e("OneSignalManager", "Failed to initialize OneSignal", e)
        }
    }

    fun registerUser(userId: String) {
        try {
            Log.d("OneSignalManager", "👤 Registering user with OneSignal: $userId")

            OneSignal.login(userId)

            CoroutineScope(Dispatchers.Main).launch {
                delay(1000)
                requestPushPermissions()

                delay(2000)
                logCurrentStatus()
            }

            Log.d("OneSignalManager", "User registration initiated for: $userId")

        } catch (e: Exception) {
            Log.e("OneSignalManager", "Failed to register user: $userId", e)
        }
    }

    private suspend fun requestPushPermissions() {
        try {
            Log.d("OneSignalManager", "Requesting push notification permissions...")

            val currentPermission = OneSignal.Notifications.permission
            Log.d("OneSignalManager", "Current permission status: $currentPermission")

            if (currentPermission) {
                Log.d("OneSignalManager", "Permissions already granted!")
                return
            }

            Log.d("OneSignalManager", "Opting in to push notifications...")
            OneSignal.User.pushSubscription.optIn()

            delay(500)

            Log.d("OneSignalManager", "Showing Android system permission dialog...")
            val accepted = OneSignal.Notifications.requestPermission(true)

            if (accepted) {
                Log.d("OneSignalManager", "Push notification permissions GRANTED!")
                Log.d("OneSignalManager", "User can now receive push notifications")
            } else {
                Log.w("OneSignalManager", "Push notification permissions DENIED")
                Log.w("OneSignalManager", "User will not receive push notifications")
            }

        } catch (e: Exception) {
            Log.e("OneSignalManager", "Failed to request push permissions", e)
        }
    }

    private fun getCurrentUserInfo(): String {
        return try {
            val onesignalId = OneSignal.User.onesignalId
            val externalId = OneSignal.User.externalId
            val pushSubscriptionId = OneSignal.User.pushSubscription.id
            val isOptedIn = OneSignal.User.pushSubscription.optedIn
            val hasPermission = OneSignal.Notifications.permission

            """
            CURRENT ONESIGNAL STATUS:
            OneSignal ID: $onesignalId
            External ID: $externalId  
            Push Subscription ID: $pushSubscriptionId
            Opted In: $isOptedIn
            Has Permission: $hasPermission
            """.trimIndent()

        } catch (e: Exception) {
            Log.e("OneSignalManager", "Failed to get user info", e)
            "Failed to get user info: ${e.message}"
        }
    }

    fun logCurrentStatus() {
        try {
            Log.d("OneSignalManager", "=== ONESIGNAL DEBUG STATUS ===")
            val info = getCurrentUserInfo()
            Log.d("OneSignalManager", info)
            
            val permissionStatus = OneSignal.Notifications.permission
            Log.d("OneSignalManager", "Permission Check: $permissionStatus")

            if (permissionStatus) {
                Log.d("OneSignalManager", "READY FOR PUSH NOTIFICATIONS!")
            } else {
                Log.w("OneSignalManager", "NOT READY - Need permission")
            }

            Log.d("OneSignalManager", "=== END STATUS ===")

        } catch (e: Exception) {
            Log.e("OneSignalManager", "Failed to log status", e)
        }
    }

    fun loginUser(userId: String, username: String, email: String) {
        try {
            OneSignal.login(userId)

            OneSignal.User.addTag("user_id", userId)
            OneSignal.User.addTag("username", username)
            OneSignal.User.addTag("email", email)

            Log.d("OneSignalManager", "User logged in successfully: $userId")

        } catch (e: Exception) {
            Log.e("OneSignalManager", "Failed to login user to OneSignal", e)
        }
    }

    fun logoutUser() {
        try {
            OneSignal.logout()
            Log.d("OneSignalManager", "User logged out from OneSignal")

        } catch (e: Exception) {
            Log.e("OneSignalManager", "Failed to logout from OneSignal", e)
        }
    }
}