package com.storyspots.caption

import android.util.Log
import androidx.compose.ui.geometry.Offset
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraState
import com.mapbox.maps.MapView
import com.storyspots.pin.SimpleClustering
import java.lang.Math.toRadians
import kotlin.math.*

class MapLoader {
    companion object {
        private const val TAG = "LightweightMapLoader"
        private var onPinClickListener: ((List<StoryData>, Offset) -> Unit)? = null

        fun setOnPinClickListener(listener: (List<StoryData>, Offset) -> Unit) {
            onPinClickListener = listener
            Log.d(TAG, "Pin click listener set")
        }

        fun initialize(mapView: MapView) {
            Log.d(TAG, "MapLoader initialized (lightweight mode)")
        }

        fun loadAllStories(onResult: (List<StoryData>) -> Unit) {
            Log.d(TAG, "loadAllStories called - delegating to MapStateManager")
        }

        fun refreshStories() {
            Log.d(TAG, "refreshStories called - no action needed")
        }

        fun cleanup() {
            onPinClickListener = null
            Log.d(TAG, "MapLoader cleaned up")
        }
    }
}