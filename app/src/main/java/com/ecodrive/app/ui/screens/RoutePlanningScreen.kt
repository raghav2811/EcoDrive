package com.ecodrive.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecodrive.app.data.LocationPoint
import com.ecodrive.app.map.MapLibreMap
import com.ecodrive.app.map.RoutePolyline
import com.ecodrive.app.routing.EcoRoute
import com.ecodrive.app.routing.LatLng

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutePlanningScreen(
    currentLocation: LocationPoint?,
    destinationLat: String,
    destinationLng: String,
    routes: List<EcoRoute>,
    isCalculating: Boolean,
    errorMessage: String?,
    onDestinationLatChange: (String) -> Unit,
    onDestinationLngChange: (String) -> Unit,
    onCalculateRoutes: () -> Unit,
    onStartTripWithRoute: (EcoRoute) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    
    // Convert routes to polylines for map display
    val routePolylines = routes.map { route ->
        RoutePolyline(
            points = route.geometry,
            color = if (route.isRecommended) "#4CAF50" else "#9E9E9E",
            width = if (route.isRecommended) 6.0f else 4.0f,
            isRecommended = route.isRecommended
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plan Eco Route") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Map Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                if (routes.isNotEmpty()) {
                    MapLibreMap(
                        modifier = Modifier.fillMaxSize(),
                        currentLocation = currentLocation?.let {
                            LatLng(it.latitude, it.longitude)
                        },
                        routes = routePolylines
                    )
                } else {
                    MapLibreMap(
                        modifier = Modifier.fillMaxSize(),
                        currentLocation = currentLocation?.let {
                            LatLng(it.latitude, it.longitude)
                        }
                    )
                }
            }
            
            // Input and Results Section
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp)
            ) {
                // Current Location Display
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.MyLocation,
                            contentDescription = "Current Location",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Current Location", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            currentLocation?.let {
                                Text(
                                    "%.5f, %.5f".format(it.latitude, it.longitude),
                                    fontSize = 14.sp
                                )
                            } ?: Text("Getting location...", fontSize = 14.sp)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Destination Input
                Text(
                    "Destination Coordinates",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = destinationLat,
                    onValueChange = onDestinationLatChange,
                    label = { Text("Latitude") },
                    placeholder = { Text("e.g., 40.7128") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = destinationLng,
                    onValueChange = onDestinationLngChange,
                    label = { Text("Longitude") },
                    placeholder = { Text("e.g., -74.0060") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onCalculateRoutes,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCalculating && currentLocation != null
                ) {
                    if (isCalculating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Calculating...")
                    } else {
                        Icon(Icons.Default.Route, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Find Eco Routes")
                    }
                }
                
                // Error Message
                errorMessage?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                error,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                
                // Routes List
                if (routes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        "Available Routes",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    routes.forEach { route ->
                        RouteCard(
                            route = route,
                            onClick = { onStartTripWithRoute(route) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun RouteCard(
    route: EcoRoute,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (route.isRecommended) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (route.isRecommended) 4.dp else 2.dp
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Route ${route.id + 1}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (route.isRecommended) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "RECOMMENDED",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
                
                // Eco Score
                Text(
                    "${route.ecoScore.toInt()}",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        route.ecoScore >= 80 -> Color(0xFF4CAF50)
                        route.ecoScore >= 60 -> Color(0xFFFFC107)
                        else -> Color(0xFFF44336)
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Stats Row 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                RouteStatItem(
                    icon = Icons.Default.Place,
                    label = "Distance",
                    value = "%.1f km".format(route.distance)
                )
                RouteStatItem(
                    icon = Icons.Default.Schedule,
                    label = "Duration",
                    value = "%.0f min".format(route.duration)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Stats Row 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                RouteStatItem(
                    icon = Icons.Default.LocalGasStation,
                    label = "Fuel",
                    value = "%.2f L".format(route.fuelEstimate)
                )
                RouteStatItem(
                    icon = Icons.Default.Cloud,
                    label = "CO₂",
                    value = "%.2f kg".format(route.co2Estimate)
                )
            }
            
            if (route.turnsCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.TurnRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "${route.turnsCount} turns",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun RouteStatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
