package com.storyspots

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.style.MapStyle

public class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)

        testFirestoreConnection()

        setContent {
            MapboxMap(
                modifier = Modifier.fillMaxSize(),
                style = { MapStyle(style = "mapbox://styles/jordana-gc/cmad3b95m00oo01sdbs0r2rag") }
            )
        }
    }

    private fun testFirestoreConnection() {
        try {
            Log.d(TAG, "Attempting to initialize Firebase Firestore")
            val db = FirebaseFirestore.getInstance()
            Log.d(TAG, "Firestore instance obtained successfully")

            val testData = hashMapOf(
                "timestamp" to System.currentTimeMillis(),
                "message" to "Firebase connection test"
            )

            Log.d(TAG, "Attempting to write test data to Firestore")
            db.collection("connection_tests")
                .add(testData)
                .addOnSuccessListener { documentReference ->
                    Log.d(TAG, "Firebase connection successful! Document ID: ${documentReference.id}")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Firebase connection failed", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase", e)
        }
    }
}