package com.storyspots.location

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mapbox.maps.MapView
import com.storyspots.R

@Composable
fun RecenterButton(
    mapView: MapView,
    locationManager: LocationManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    FloatingActionButton(
        onClick = {
            try {
                locationManager.recenterOnUserLocation(mapView)
                Toast.makeText(context, "Recentering...", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("RecenterButton", "Failed to recenter", e)
                Toast.makeText(context, "Failed to recenter", Toast.LENGTH_SHORT).show()
            }
        },
        modifier = modifier.padding(16.dp),
        containerColor = MaterialTheme.colorScheme.primary
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_location_finder),
            contentDescription = "Recenter map"
        )
    }
}