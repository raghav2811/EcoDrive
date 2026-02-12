package com.ecodrive.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecodrive.app.data.EcoDriveResult
import com.ecodrive.app.data.LocationPoint
import com.ecodrive.app.map.MapLibreMap
import com.ecodrive.app.routing.LatLng
import com.ecodrive.app.ui.components.EcoScoreGauge
import com.ecodrive.app.ui.components.EcoTipsCard
import com.ecodrive.app.ui.components.TripStats
import com.ecodrive.app.utils.EcoScoreUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveDriveScreen(
    currentLocation: LocationPoint?,
    ecoResult: EcoDriveResult?,
    isTracking: Boolean,
    onStartTrip: () -> Unit,
    onStopTrip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    
    // Auto-start trip when screen opens if not already tracking
    LaunchedEffect(Unit) {
        if (!isTracking) {
            onStartTrip()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (isTracking) "Trip in Progress" else "Trip Paused",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                tonalElevation = 0.dp
            ) {
                Button(
                    onClick = onStopTrip,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Stop Trip",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            // Map Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            ) {
                if (currentLocation != null) {
                    MapLibreMap(
                        modifier = Modifier.fillMaxSize(),
                        currentLocation = LatLng(
                            latitude = currentLocation.latitude,
                            longitude = currentLocation.longitude
                        )
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            
            // Results Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                if (ecoResult != null) {
                    // Eco Score Gauge - show score only if user has moved
                    val displayScore = if (ecoResult.tripData.tripDistance < 0.001f) {
                        0f  // Show 0 until user starts moving
                    } else {
                        ecoResult.ecoScore
                    }
                    
                    EcoScoreGauge(
                        score = displayScore,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Trip Stats
                    TripStats(
                        distance = String.format("%.3f km", ecoResult.tripData.tripDistance),
                        duration = String.format("%.1f min", ecoResult.tripData.tripDuration),
                        avgSpeed = String.format("%.1f km/h", ecoResult.tripData.averageSpeed),
                        fuelUsed = String.format("%.3f L", ecoResult.fuelUsed),
                        co2Emitted = String.format("%.3f kg", ecoResult.co2Emitted),
                        waitTime = String.format("%.1f min", ecoResult.tripData.waitTime)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Eco Tips
                    val tips = EcoScoreUtils.getEcoTips(ecoResult.ecoScore)
                    EcoTipsCard(tips = tips)
                } else {
                    // Show basic info while waiting for eco result
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Starting trip tracking...",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (currentLocation != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Location: ${String.format("%.5f", currentLocation.latitude)}, ${String.format("%.5f", currentLocation.longitude)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
