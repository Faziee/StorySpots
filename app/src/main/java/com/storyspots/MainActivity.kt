package com.storyspots

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.createBitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.style.MapStyle
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.compose.MapEffect
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.addOnMapClickListener

@OptIn(MapboxExperimental::class)
class MainActivity : ComponentActivity(), PermissionsListener {

    private val TAG = "MainActivity"
    private lateinit var permissionsManager: PermissionsManager

    private val locationPermissionGranted = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)
        testFirestoreConnection()

        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            Log.d(TAG, "Location permission already granted")
            locationPermissionGranted.value = true
        } else {
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(this)
        }

        setContent {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    MapView(context).apply {
                        getMapboxMap().loadStyleUri(
                            "mapbox://styles/jordana-gc/cmad3b95m00oo01sdbs0r2rag"
                        ) {
                            if (locationPermissionGranted.value) {
                                enableLocationComponent(this)
                            }
                        }
                    }
                }
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onExplanationNeeded(permissionsToExplain: List<String>) {
        Toast.makeText(this, "This app needs location permission to show your location on the map.", Toast.LENGTH_LONG).show()
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            Toast.makeText(this, "Location permission granted!", Toast.LENGTH_SHORT).show()
            // Update the state to enable location on the map
            enableLocationOnMap()
        } else {
            Toast.makeText(this, "Location permission not granted :(", Toast.LENGTH_SHORT).show()
            locationPermissionGranted.value = false
        }
    }

    private fun enableLocationOnMap() {
        // This is the non-Compose version you can call from your activity
        runOnUiThread {
            locationPermissionGranted.value = true
            Log.d(TAG, "Location features enabled on map")
        }
    }

    private fun enableLocationComponent(mapView: com.mapbox.maps.MapView) {

        val locationComponentPlugin = mapView.location

        locationComponentPlugin.updateSettings {
            enabled = true
            pulsingEnabled = true
            pulsingColor = android.graphics.Color.BLUE
            pulsingMaxRadius = 40f
            showAccuracyRing = true
            accuracyRingColor = android.graphics.Color.parseColor("#4d89cff0")
            accuracyRingBorderColor = android.graphics.Color.parseColor("#80ffffff")
        }

        locationComponentPlugin.locationPuck = LocationPuck2D(
            bearingImage = null,  // You can add a custom bearing image
            shadowImage = null,   // You can add a custom shadow
            scaleExpression = null
        )

        locationComponentPlugin.enabled = true
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

//    @Composable
//    fun MapViewScreen(context: Context){
//        AndroidView(factory = {
//            val mapView = MapView(context).apply {
//                getMapboxMap().loadStyleUri("mapbox://styles/jordana-gc/cmad3b95m00oo01sdbs0r2rag"){
//                    val annotationManager = annotations.createPointAnnotationManager()
//
//                    getMapboxMap().addOnMapClickListener { point ->
//                        val bitmap = getBitmapFromDrawable(context, R.drawable.pin_marker)
//
//                        if(bitmap != null){
//                            val annotation = PointAnnotationOptions().withPoint(point).withIconImage(bitmap)
//                            annotationManager.deleteAll()
//                            annotationManager.create(annotation)
//                        }
//
//                        true
//                    }
//                }
//            }
//            mapView
//        })
//    }

//    fun getBitmapFromDrawable(context: Context, resourceID: Int): Bitmap?{
//        val drawable = ContextCompat.getDrawable(context, resourceID) ?: return null
//
//        if (drawable is BitmapDrawable) return drawable.bitmap
//        val bitmap = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
//        val canvas = android.graphics.Canvas(bitmap)
//
//        drawable.setBounds(0, 0, canvas.width, canvas.height)
//        drawable.draw(canvas)
//
//        return bitmap
//    }
}