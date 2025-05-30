package com.storyspots.pin

import android.graphics.Bitmap
import android.util.Log
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.CircleLayer
import com.mapbox.maps.extension.style.layers.generated.SymbolLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.getSource
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions

class SimpleClustering {

    companion object {
        private const val TAG = "SimpleClustering"
        private var instance: SimpleClustering? = null
        private val pinData = mutableListOf<PinData>()
        private const val SOURCE_ID = "clustering-pins"
        private var isInitialized = false

        fun setupClustering(mapView: MapView, pointAnnotationManager: PointAnnotationManager, pinBitmap: Bitmap) {
            Log.d(TAG, "Setting up clustering...")
            if (instance == null) {
                instance = SimpleClustering()
            }
            instance?.initialize(mapView, pointAnnotationManager, pinBitmap)
        }

        fun addClusterPin(point: Point) {
            Log.d(TAG, "Adding pin at: ${point.latitude()}, ${point.longitude()}")

            if (point.latitude().isNaN() || point.longitude().isNaN()) {
                Log.e(TAG, "Invalid coordinates: lat=${point.latitude()}, lng=${point.longitude()}")
                return
            }

            val feature = Feature.fromGeometry(point)
            pinData.add(PinData(point, feature, null))
            Log.d(TAG, "Total pins: ${pinData.size}")
            instance?.updateData()
        }
    }

    data class PinData(
        val point: Point,
        val feature: Feature,
        var annotation: PointAnnotation?
    )

    private var mapView: MapView? = null
    private var pointAnnotationManager: PointAnnotationManager? = null
    private var pinBitmap: Bitmap? = null

    private fun initialize(mapView: MapView, pointAnnotationManager: PointAnnotationManager, pinBitmap: Bitmap) {
        if (isInitialized) {
            Log.d(TAG, "Already initialized")
            return
        }

        this.mapView = mapView
        this.pointAnnotationManager = pointAnnotationManager
        this.pinBitmap = pinBitmap
        Log.d(TAG, "Initializing clustering...")

        mapView.getMapboxMap().getStyle { style ->
            try {
                Log.d(TAG, "Style loaded, creating source...")

                val emptyGeoJson = """
                {
                    "type": "FeatureCollection",
                    "features": []
                }
                """.trimIndent()

                val source = GeoJsonSource.Builder(SOURCE_ID)
                    .data(emptyGeoJson)
                    .cluster(true)
                    .clusterMaxZoom(14)
                    .clusterRadius(50)
                    .build()

                style.addSource(source)
                Log.d(TAG, "Source added")

                val clusterLayer = CircleLayer("cluster-circles", SOURCE_ID)
                clusterLayer.filter(Expression.has(Expression.literal("point_count")))
                clusterLayer.circleColor(Expression.literal("#FF0000"))
                clusterLayer.circleRadius(Expression.literal(25.0))
                clusterLayer.circleStrokeWidth(Expression.literal(2.0))
                clusterLayer.circleStrokeColor(Expression.literal("#FFFFFF"))

                style.addLayer(clusterLayer)
                Log.d(TAG, "Cluster layer added")

                val countLayer = SymbolLayer("cluster-text", SOURCE_ID)
                countLayer.filter(Expression.has(Expression.literal("point_count")))
                countLayer.textField(Expression.get(Expression.literal("point_count")))
                countLayer.textSize(Expression.literal(14.0))
                countLayer.textColor(Expression.literal("#FFFFFF"))

                style.addLayer(countLayer)
                Log.d(TAG, "Count layer added")

                val pinLayer = SymbolLayer("unclustered-pins", SOURCE_ID)
                pinLayer.filter(Expression.not(Expression.has(Expression.literal("point_count"))))
                pinLayer.iconImage(Expression.literal("pin-marker")) // We'll add this image
                pinLayer.iconSize(Expression.literal(0.1))
                pinLayer.iconAllowOverlap(Expression.literal(true))

                style.addLayer(pinLayer)
                Log.d(TAG, "Pin layer added")

                style.addImage("pin-marker", pinBitmap)
                Log.d(TAG, "Pin bitmap added to style")

                ClusterZoomHandler.setupClusterClickHandler(mapView, SOURCE_ID)

                isInitialized = true
                Log.d(TAG, "Clustering initialization complete!")

            } catch (e: Exception) {
                Log.e(TAG, "Error initializing clustering", e)
            }
        }
    }

    private fun updateData() {
        if (!isInitialized) {
            Log.d(TAG, "Not initialized yet, skipping update")
            return
        }

        Log.d(TAG, "Updating data with ${pinData.size} pins")
        mapView?.getMapboxMap()?.getStyle { style ->
            try {
                val source = style.getSource(SOURCE_ID) as? GeoJsonSource
                if (source != null) {
                    // Create GeoJSON manually
                    val features = pinData.map { pinData ->
                        val point = pinData.point
                        """
                        {
                            "type": "Feature",
                            "geometry": {
                                "type": "Point",
                                "coordinates": [${point.longitude()}, ${point.latitude()}]
                            },
                            "properties": {}
                        }
                        """.trimIndent()
                    }

                    val geoJsonString = """
                    {
                        "type": "FeatureCollection",
                        "features": [${features.joinToString(",")}]
                    }
                    """.trimIndent()

                    Log.d(TAG, "Generated GeoJSON: $geoJsonString")

                    source.data(geoJsonString)
                    Log.d(TAG, "Data updated successfully")

                    pointAnnotationManager?.deleteAll()
                    Log.d(TAG, "Cleared existing annotations")

                } else {
                    Log.e(TAG, "Source not found!")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating data", e)
            }
        }
    }
}