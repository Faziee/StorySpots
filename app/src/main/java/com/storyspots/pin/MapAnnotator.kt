package com.storyspots.pin

import android.graphics.BitmapFactory
import com.mapbox.geojson.Point
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.storyspots.R

class MapAnnotator {
    //TODO: Map pinning
    private var pointAnnotationManager: PointAnnotationManager? = null
    private var contentInitialized = false

    //TODO: Map pinning
    private fun addPin(point: Point) {
        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.pin_marker)

        val annotationOptions = PointAnnotationOptions()
            .withPoint(point)
            .withIconImage(bitmap)
            .withIconSize(0.1)
        pointAnnotationManager?.create(annotationOptions)
    }
}