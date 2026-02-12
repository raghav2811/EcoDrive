package com.ecodrive.app

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.ecodrive.app.routing.EcoRoute
import com.ecodrive.app.ui.RouteViewModel
import com.ecodrive.app.ui.TripViewModel
import com.ecodrive.app.ui.screens.HomeScreen
import com.ecodrive.app.ui.screens.LiveDriveScreen
import com.ecodrive.app.ui.screens.RoutePlanningScreen
import com.ecodrive.app.ui.theme.EcoDriveTheme
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.WellKnownTileServer

enum class AppScreen {
    HOME,
    ROUTE_PLANNING,
    LIVE_DRIVE
}

class MainActivity : ComponentActivity() {
    
    private val tripViewModel: TripViewModel by viewModels()
    private val routeViewModel: RouteViewModel by viewModels()
    
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true -> {
                // Permission granted
                startTripAfterPermission()
            }
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                // Permission granted (coarse)
                startTripAfterPermission()
            }
            else -> {
                // Permission denied
            }
        }
    }
    
    private var pendingTripStart = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize MapLibre before creating any MapViews
        Mapbox.getInstance(this, "", WellKnownTileServer.MapLibre)
        
        // Request location permissions immediately
        requestLocationPermissions()
        
        setContent {
            EcoDriveTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EcoDriveApp(
                        tripViewModel = tripViewModel,
                        routeViewModel = routeViewModel,
                        onRequestPermission = { requestLocationPermissions() }
                    )
                }
            }
        }
    }
    
    private fun requestLocationPermissions() {
        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }
    
    private fun startTripAfterPermission() {
        if (pendingTripStart) {
            tripViewModel.startTrip()
            pendingTripStart = false
        }
    }
}

@Composable
fun EcoDriveApp(
    tripViewModel: TripViewModel,
    routeViewModel: RouteViewModel,
    onRequestPermission: () -> Unit
) {
    var currentScreen by remember { mutableStateOf(AppScreen.HOME) }
    var selectedRoute by remember { mutableStateOf<EcoRoute?>(null) }
    
    val isTracking by tripViewModel.isTracking.collectAsState()
    val currentLocation by tripViewModel.currentLocation.collectAsState()
    val ecoResult by tripViewModel.ecoResult.collectAsState()
    
    // Route planning state
    val routeCurrentLocation by routeViewModel.currentLocation.collectAsState()
    val destinationLat by routeViewModel.destinationLat.collectAsState()
    val destinationLng by routeViewModel.destinationLng.collectAsState()
    val routes by routeViewModel.routes.collectAsState()
    val isCalculating by routeViewModel.isCalculating.collectAsState()
    val errorMessage by routeViewModel.errorMessage.collectAsState()
    
    when (currentScreen) {
        AppScreen.HOME -> {
            HomeScreen(
                onStartTrip = {
                    currentScreen = AppScreen.ROUTE_PLANNING
                },
                onStartLiveTrip = {
                    currentScreen = AppScreen.LIVE_DRIVE
                }
            )
        }
        
        AppScreen.ROUTE_PLANNING -> {
            RoutePlanningScreen(
                currentLocation = routeCurrentLocation,
                destinationLat = destinationLat,
                destinationLng = destinationLng,
                routes = routes,
                isCalculating = isCalculating,
                errorMessage = errorMessage,
                onDestinationLatChange = { routeViewModel.updateDestinationLat(it) },
                onDestinationLngChange = { routeViewModel.updateDestinationLng(it) },
                onCalculateRoutes = {
                    routeViewModel.calculateRoutes()
                },
                onStartTripWithRoute = { route ->
                    selectedRoute = route
                    currentScreen = AppScreen.LIVE_DRIVE
                },
                onBack = { currentScreen = AppScreen.HOME }
            )
        }
        
        AppScreen.LIVE_DRIVE -> {
            LiveDriveScreen(
                currentLocation = currentLocation,
                ecoResult = ecoResult,
                isTracking = isTracking,
                onStartTrip = {
                    tripViewModel.startTrip()
                },
                onStopTrip = {
                    tripViewModel.stopTrip()
                    currentScreen = AppScreen.HOME
                }
            )
        }
    }
}
