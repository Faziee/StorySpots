package com.storyspots

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.style.MapStyle
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.addOnMapClickListener

public class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MapViewScreen(this)
        }
    }

    @Composable
    fun MapViewScreen(context: Context){
        AndroidView(factory = {
            val mapView = MapView(context).apply {
                getMapboxMap().loadStyleUri("mapbox://styles/jordana-gc/cmad3b95m00oo01sdbs0r2rag"){
                    val annotationManager = annotations.createPointAnnotationManager()

                    getMapboxMap().addOnMapClickListener { point ->
                        val bitmap = getBitmapFromDrawable(context, R.drawable.pin_marker)

                        if(bitmap != null){
                            val annotation = PointAnnotationOptions().withPoint(point).withIconImage(bitmap)
                            annotationManager.deleteAll()
                            annotationManager.create(annotation)
                        }

                        true
                    }
                }
            }
            mapView
        })
    }

    fun getBitmapFromDrawable(context: Context, resourceID: Int): Bitmap?{
        val drawable = ContextCompat.getDrawable(context, resourceID) ?: return null

        if (drawable is BitmapDrawable) return drawable.bitmap
        val bitmap = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
        val canvas = android.graphics.Canvas(bitmap)

        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return bitmap
    }
}