package com.storyspots.core

import android.app.Application
import android.util.Log
import com.cloudinary.android.MediaManager
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.onesignal.OneSignal
import com.storyspots.utils.Constants
import com.storyspots.utils.OneSignalManager
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

class StorySpot : Application() {
    companion object {
        @Volatile
        lateinit var instance: StorySpot
            private set

        private val firebaseInitialized = AtomicBoolean(false)
        private val cloudinaryInitialized = AtomicBoolean(false)

        val isFirebaseReady: Boolean
            get() = firebaseInitialized.get()

        val isFullyInitialized: Boolean
            get() = firebaseInitialized.get() && cloudinaryInitialized.get()
    }

    private val initScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        instance = this

        initializeCritical()

        initScope.launch {
            initializeNonCritical()
        }
    }

    private fun initializeCritical() {
        //----CRASH HANDLER----//
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            Log.e("StorySpot", "Uncaught exception on thread ${thread.name}", exception)
        }
    }

    private suspend fun initializeNonCritical() = coroutineScope {
        //----FIREBASE----//
        launch {
            try {
                FirebaseApp.initializeApp(this@StorySpot)

                val settings = FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                    .build()

                FirebaseFirestore.getInstance().firestoreSettings = settings
                firebaseInitialized.set(true)
                Log.d("StorySpot", "Firebase initialized")
            } catch (e: Exception) {
                Log.e("StorySpot", "Firebase initialization failed", e)
            }
        }

        //----CLOUDINARY----//
        launch {
            try {
                val config = HashMap<String, String>().apply {
                    put("cloud_name", Constants.CLOUDINARY_CLOUD_NAME)
                    put("api_key", Constants.CLOUDINARY_API_KEY)
                    put("api_secret", Constants.CLOUDINARY_API_SECRET)
                }
                MediaManager.init(this@StorySpot, config)
                cloudinaryInitialized.set(true)
                Log.d("StorySpot", "Cloudinary initialized")
            } catch (e: Exception) {
                Log.e("StorySpot", "Cloudinary initialization failed", e)
            }
        }

        //----ONESIGNAL----//
        launch {
            try {
                OneSignalManager.initialize(this@StorySpot, Constants.ONESIGNAL_APP_ID)
                Log.d("StorySpot", "OneSignal initialized")
            } catch (e: Exception) {
                Log.e("StorySpot", "OneSignal initialization failed", e)
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        initScope.cancel()
    }
}