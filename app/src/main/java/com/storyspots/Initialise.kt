package com.storyspots

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.locationcomponent.location
import androidx.core.graphics.toColorInt

class Initialise : PermissionsListener {
    private lateinit var permissionsManager: PermissionsManager
    private val locationPermissionGranted = mutableStateOf(false)

    fun arePermissionsGranted(): Boolean {
        if (PermissionsManager.areLocationPermissionsGranted(this))
        {
            Log.d("PermissionsManagement", "Location permission already granted")
            locationPermissionGranted.value = true
            return true
        }
        else
        {
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(this)
            return false
        }
    }

    //TODO: Permissions Management
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        FirebaseApp.initializeApp(this)
//        FirebaseFirestore.getInstance().firestoreSettings =
//            FirebaseFirestoreSettings.Builder()
//                .setPersistenceEnabled(true)
//                .build()

        this.arePermissionsGranted();
    }

    //TODO: Permissions Management ---Move to UI class
    @Composable
    fun PermissionRequestScreen() {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Requesting location permissions...")
        }
    }

    //TODO: Permissions Management -- Move to UI class
    override fun onExplanationNeeded(permissionsToExplain: List<String>) {
        Toast.makeText(
            this,
            "This app needs location permission to show your location on the map.",
            Toast.LENGTH_LONG
        ).show()
    }

    //TODO: Permissions Management
    override fun onPermissionResult(granted: Boolean, mapView: MapView) {
        locationPermissionGranted.value = granted

        if (granted) {
            // TODO -- Move to UI class
            Toast.makeText(this, "Location permission granted!", Toast.LENGTH_SHORT).show()
            mapView.getMapboxMap().getStyle { style ->
                enableLocationComponent(mapView)
            }
            if (!contentInitialized) {
                initializeContent()
            } else if (::mapView.isInitialized) {
                mapView.getMapboxMap().getStyle { style ->
                    enableLocationComponent(mapView)
                }
            }
        }
        else
        {
            // TODO -- Move to UI class
            Toast.makeText(this, "Location permission not granted :(", Toast.LENGTH_SHORT).show()

            locationPermissionGranted.value = false

            if (!contentInitialized)
            {
                initializeContent()
            }
        }
    }

    //DONE!
    fun mapLocationSettings(mapView: MapView)  //--> RENAMED FROM ENABLELOCATIONCOMPONENT
    {
        mapView.location.updateSettings {
            enabled = true
            puckBearingEnabled = true
            puckBearing = PuckBearing.COURSE
            locationPuck = LocationPuck2D()

            pulsingEnabled = true
            pulsingColor = Color.BLUE
            pulsingMaxRadius = 40f
            showAccuracyRing = true
            accuracyRingColor = "#4d89cff0".toColorInt()
            accuracyRingBorderColor = "#80ffffff".toColorInt()
        }
    }
}