package com.ecodrive.app.routing

import com.google.gson.annotations.SerializedName

// OSRM API Response Models
data class OSRMResponse(
    val code: String,
    val routes: List<OSRMRoute>?,
    val waypoints: List<OSRMWaypoint>?
)

data class OSRMRoute(
    val distance: Double,      // meters
    val duration: Double,      // seconds
    val geometry: String,      // encoded polyline or GeoJSON
    val legs: List<OSRMLeg>?
)

data class OSRMLeg(
    val distance: Double,
    val duration: Double,
    val steps: List<OSRMStep>?
)

data class OSRMStep(
    val distance: Double,
    val duration: Double,
    val geometry: String,
    val name: String?,
    @SerializedName("maneuver") val maneuver: OSRMManeuver?
)

data class OSRMManeuver(
    val type: String?,
    val location: List<Double>?
)

data class OSRMWaypoint(
    val location: List<Double>,
    val name: String?
)

// App-level Route Model
data class EcoRoute(
    val id: Int,
    val distance: Double,           // km
    val duration: Double,           // minutes
    val geometry: List<LatLng>,     // decoded polyline
    val ecoScore: Float = 0f,       // calculated eco-efficiency score
    val fuelEstimate: Float = 0f,   // liters
    val co2Estimate: Float = 0f,    // kg
    val turnsCount: Int = 0,        // number of turns/maneuvers
    val isRecommended: Boolean = false
)

data class LatLng(
    val latitude: Double,
    val longitude: Double
)
