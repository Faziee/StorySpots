package com.storyspots.location

import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mapbox.maps.MapView
import com.storyspots.R

@Composable
fun RecenterButton(
    mapView: MapView?,
    locationManager: LocationManager,
    modifier: Modifier = Modifier,
    onRecenter: () -> Unit = {}
) {
    val context = LocalContext.current

    Surface(
        modifier = modifier
            .size(48.dp)
            .padding(4.dp),
        shape = CircleShape,
        color = Color(0xFFFF9CC7),
        shadowElevation = 4.dp
    ) {
        IconButton(
            onClick = {
                if (locationManager.recenterOnUserLocation(mapView)) {
                    onRecenter()
                } else {
                    Toast.makeText(context, "Getting location...", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.size(36.dp)
                .size(48.dp)
                .padding(6.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_location_finder),
                contentDescription = "Recenter map",
                tint = Color.White
            )
        }
    }
}