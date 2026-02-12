package com.ecodrive.app.map

import android.content.Context
import android.graphics.Color
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ecodrive.app.routing.LatLng
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.LineManager
import com.mapbox.mapboxsdk.plugins.annotation.LineOptions
import com.mapbox.mapboxsdk.plugins.annotation.CircleManager
import com.mapbox.mapboxsdk.plugins.annotation.CircleOptions

private const val TAG = "MapLibreCompose"

@Composable
fun MapLibreMap(
    modifier: Modifier = Modifier,
    currentLocation: LatLng? = null,
    routes: List<RoutePolyline> = emptyList(),
    onMapReady: (MapboxMap) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var mapView: MapView? by remember { mutableStateOf(null) }
    var mapboxMap: MapboxMap? by remember { mutableStateOf(null) }
    var lineManager: LineManager? by remember { mutableStateOf(null) }
    var circleManager: CircleManager? by remember { mutableStateOf(null) }
    var isMapReady by remember { mutableStateOf(false) }
    var mapError by remember { mutableStateOf<String?>(null) }
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color(0xFFE8E5DC)), // Beige background while loading
            factory = { ctx ->
                Log.d(TAG, "Creating MapView")
                MapView(ctx).apply {
                    mapView = this
                    onCreate(null)
                    
                    getMapAsync { map ->
                        Log.d(TAG, "Map async callback received")
                        mapboxMap = map
                        
                        // Use MapLibre demo tiles (known to work)
                        val styleUrl = "https://basemaps.cartocdn.com/gl/voyager-gl-style/style.json"
                        
                        Log.d(TAG, "Loading style from: $styleUrl")
                        map.setStyle(styleUrl, object : Style.OnStyleLoaded {
                            override fun onStyleLoaded(style: Style) {
                                Log.d(TAG, "Style loaded successfully")
                                lineManager = LineManager(this@apply, map, style)
                                circleManager = CircleManager(this@apply, map, style)
                                
                                // Set initial camera position if location available
                                currentLocation?.let { loc ->
                                    val cameraPosition = CameraPosition.Builder()
                                        .target(com.mapbox.mapboxsdk.geometry.LatLng(
                                            loc.latitude,
                                            loc.longitude
                                        ))
                                        .zoom(14.0)
                                        .build()
                                    map.cameraPosition = cameraPosition
                                    Log.d(TAG, "Camera positioned at: ${loc.latitude}, ${loc.longitude}")
                                }
                                
                                // Mark map as ready after everything is set up
                                isMapReady = true
                                onMapReady(map)
                            }
                        })
                    }
                }
            },
            update = { view ->
                // Only update if map is ready
                if (!isMapReady || mapboxMap == null || circleManager == null) return@AndroidView
                
                val map = mapboxMap!!
                val circleMgr = circleManager!!
                
                // Update current location marker
                currentLocation?.let { location ->
                    val latLng = com.mapbox.mapboxsdk.geometry.LatLng(
                        location.latitude,
                        location.longitude
                    )
                    
                    // Clear previous location marker and create new one
                    circleMgr.deleteAll()
                    circleMgr.create(
                        CircleOptions()
                            .withLatLng(latLng)
                            .withCircleRadius(8f)
                            .withCircleColor("#1E88E5")  // Blue marker
                            .withCircleStrokeWidth(2.5f)
                            .withCircleStrokeColor("#FFFFFF")  // White border
                            .withCircleOpacity(0.9f)
                    )
                    
                    // Move camera to current location
                    val cameraPosition = CameraPosition.Builder()
                        .target(latLng)
                        .zoom(if (routes.isEmpty()) 15.0 else map.cameraPosition.zoom)
                        .build()
                    
                    map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 800)
                }
                
                // Draw routes
                if (routes.isNotEmpty()) {
                    lineManager?.deleteAll()
                    
                    routes.forEach { route ->
                        if (route.points.isEmpty()) return@forEach
                        
                        val latLngs = route.points.map { latLng ->
                            com.mapbox.mapboxsdk.geometry.LatLng(
                                latLng.latitude,
                                latLng.longitude
                            )
                        }
                        
                        lineManager?.create(
                            LineOptions()
                                .withLatLngs(latLngs)
                                .withLineColor(route.color)
                                .withLineWidth(route.width)
                        )
                    }
                    
                    // Fit camera to show all routes
                    val allPoints = routes.flatMap { it.points }
                    if (allPoints.isNotEmpty()) {
                        val boundsBuilder = LatLngBounds.Builder()
                        allPoints.forEach { point ->
                            boundsBuilder.include(com.mapbox.mapboxsdk.geometry.LatLng(
                                point.latitude,
                                point.longitude
                            ))
                        }
                        
                        mapboxMap?.let { map ->
                            try {
                                val bounds = boundsBuilder.build()
                                map.animateCamera(
                                    CameraUpdateFactory.newLatLngBounds(bounds, 100)
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "Error fitting bounds", e)
                            }
                        }
                    }
                }
            }
        )
        
        // Show status text at bottom of map area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isMapReady && mapError == null) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Loading map tiles...")
                    }
                }
            }
            
            mapError?.let { error ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Map error: $error",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
    
    // Handle lifecycle events
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    Log.d(TAG, "Lifecycle: onStart")
                    mapView?.onStart()
                }
                Lifecycle.Event.ON_RESUME -> {
                    Log.d(TAG, "Lifecycle: onResume")
                    circleManager?.onDestroy()
                    mapView?.onResume()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    Log.d(TAG, "Lifecycle: onPause")
                    mapView?.onPause()
                }
                Lifecycle.Event.ON_STOP -> {
                    Log.d(TAG, "Lifecycle: onStop")
                    mapView?.onStop()
                }
                Lifecycle.Event.ON_DESTROY -> {
                    Log.d(TAG, "Lifecycle: onDestroy")
                    mapView?.onDestroy()
                    lineManager?.onDestroy()
                }
                else -> {}
            }
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

/**
 * Data class for route visualization
 */
data class RoutePolyline(
    val points: List<LatLng>,
    val color: String = "#4CAF50",  // Green for eco-friendly
    val width: Float = 5.0f,
    val isRecommended: Boolean = false
)
