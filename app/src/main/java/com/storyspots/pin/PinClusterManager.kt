package com.storyspots.pin

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.ui.geometry.Offset
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
import com.mapbox.maps.RenderedQueryGeometry
import com.mapbox.maps.RenderedQueryOptions
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.storyspots.caption.StoryData

class SimpleClustering {

    companion object {
        private const val TAG = "SimpleClustering"
        private var instance: SimpleClustering? = null
        private val pinData = mutableListOf<PinData>()
        private const val SOURCE_ID = "clustering-pins"
        private var isInitialized = false
        private var onPinClickListener: ((Point) -> Unit)? = null
        private var onSmallClusterClickListener: ((List<StoryData>, Offset) -> Unit)? = null

        fun setupClustering(mapView: MapView, pointAnnotationManager: PointAnnotationManager, pinBitmap: Bitmap) {
            Log.d(TAG, "Setting up clustering...")
            Log.d(TAG, "Current isInitialized state: $isInitialized")
            Log.d(TAG, "Current pin data count: ${pinData.size}")

            if (isInitialized && instance?.mapView == mapView) {
                Log.d(TAG, "Clustering already initialized for this MapView, skipping")
                return
            }

            isInitialized = false

            if (instance == null) {
                instance = SimpleClustering()
                Log.d(TAG, "Created new SimpleClustering instance")
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

        fun setOnPinClickListener(listener: (Point) -> Unit) {
            onPinClickListener = listener
        }

        fun setOnSmallClusterClickListener(listener: (List<StoryData>, Offset) -> Unit) {
            onSmallClusterClickListener = listener
        }

        fun clearPins() {
            Log.d(TAG, "Clearing all pins")
            pinData.clear()
            instance?.updateData()
        }

        fun refreshPins() {
            Log.d(TAG, "Refreshing pins with existing data: ${pinData.size}")
            instance?.updateData()
        }

        fun isClusteringInitialized(): Boolean = isInitialized
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

                var sourceRemoved = false
                try {
                    val existingSource = style.getSource(SOURCE_ID)
                    if (existingSource != null) {
                        listOf("cluster-circles", "cluster-text", "unclustered-pins").forEach { layerId ->
                            try {
                                style.removeStyleLayer(layerId)
                                Log.d(TAG, "Removed existing layer: $layerId")
                            } catch (e: Exception) {
                                Log.d(TAG, "Layer $layerId doesn't exist: ${e.message}")
                            }
                        }

                        style.removeStyleSource(SOURCE_ID)
                        Log.d(TAG, "Removed existing source")
                        sourceRemoved = true
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "No existing source to remove: ${e.message}")
                }

                val source = GeoJsonSource.Builder(SOURCE_ID)
                    .data(emptyGeoJson)
                    .cluster(true)
                    .clusterMaxZoom(16)
                    .clusterRadius(50)
                    .build()

                style.addSource(source)
                Log.d(TAG, "Source added successfully")

                // Remove existing layers if they exist
                listOf("cluster-circles", "cluster-text", "unclustered-pins").forEach { layerId ->
                    try {
                        style.removeStyleLayer(layerId)
                        Log.d(TAG, "Removed remaining layer: $layerId")
                    } catch (e: Exception) {
                        Log.d(TAG, "Layer $layerId doesn't exist: ${e.message}")
                    }
                }

                val clusterLayer = CircleLayer("cluster-circles", SOURCE_ID)
                clusterLayer.filter(Expression.has(Expression.literal("point_count")))

                clusterLayer.circleColor(
                    Expression.step(
                        Expression.get(Expression.literal("point_count")),
                        Expression.literal("#FF69B4"),
                        Expression.literal(5), Expression.literal("#FF1493"),
                        Expression.literal(10), Expression.literal("#FF9CC7"),
                        Expression.literal(20), Expression.literal("#FF2D87"),
                        Expression.literal(50), Expression.literal("#D91A6B"),
                        Expression.literal(100), Expression.literal("#B8155A")
                    )
                )
                clusterLayer.circleRadius(
                    Expression.step(
                        Expression.get(Expression.literal("point_count")),
                        Expression.literal(20.0),
                        Expression.literal(10), Expression.literal(25.0),
                        Expression.literal(20), Expression.literal(30.0),
                        Expression.literal(50), Expression.literal(35.0),
                        Expression.literal(100), Expression.literal(40.0)
                    )
                )

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
                pinLayer.iconImage(Expression.literal("pin-marker"))
                pinLayer.iconSize(Expression.literal(0.1))
                pinLayer.iconAllowOverlap(Expression.literal(true))

                style.addLayer(pinLayer)
                Log.d(TAG, "Pin layer added")

                try {
                    style.removeStyleImage("pin-marker")
                    Log.d(TAG, "Removed existing pin-marker image")
                } catch (e: Exception) {
                    Log.d(TAG, "No existing pin-marker image to remove: ${e.message}")
                }

                style.addImage("pin-marker", pinBitmap)
                Log.d(TAG, "Pin bitmap added to style")

                setupPinClickListener(mapView)

                isInitialized = true
                Log.d(TAG, "Clustering initialization complete! Initialized: $isInitialized")

                if (pinData.isNotEmpty()) {
                    Log.d(TAG, "Updating with existing ${pinData.size} pins")
                    updateData()
                } else {
                    Log.d(TAG, "No existing pin data to update")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error initializing clustering", e)
                isInitialized = false
            }
        }
    }

    private fun setupPinClickListener(mapView: MapView) {
        Log.d(TAG, "Setting up pin click listener")

        mapView.mapboxMap.addOnMapClickListener { point ->
            Log.d(TAG, "Map clicked, checking for pin...")

            val screenCoordinate = mapView.mapboxMap.pixelForCoordinate(point)

            mapView.mapboxMap.queryRenderedFeatures(
                RenderedQueryGeometry(screenCoordinate),
                RenderedQueryOptions(listOf("unclustered-pins"), null)
            ) { result ->
                if (!result.value.isNullOrEmpty()) {
                    Log.d(TAG, "Individual pin clicked at: $point")
                    onPinClickListener?.invoke(point)
                } else {
                    Log.d(TAG, "No pin found at click location")
                }
            }
            false
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