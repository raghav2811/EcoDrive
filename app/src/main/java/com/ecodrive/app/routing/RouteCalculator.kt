package com.ecodrive.app.routing

import android.util.Log
import com.ecodrive.app.data.TripData
import com.ecodrive.app.data.RoadType
import com.ecodrive.app.data.TrafficCondition
import com.ecodrive.app.ml.EcoScoreInference
import com.ecodrive.app.utils.EcoScoreUtils
import com.ecodrive.app.utils.PolylineDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RouteCalculator(
    private val ecoScoreInference: EcoScoreInference
) {
    private val osrmApi = OSRMApiService.create()
    
    /**
     * Fetch multiple routes from OSRM and calculate eco-scores
     */
    suspend fun calculateEcoRoutes(
        startLat: Double,
        startLng: Double,
        destLat: Double,
        destLng: Double
    ): Result<List<EcoRoute>> = withContext(Dispatchers.IO) {
        try {
            // Format coordinates for OSRM: "lng,lat;lng,lat"
            val coordinates = "$startLng,$startLat;$destLng,$destLat"
            
            val response = osrmApi.getRoute(
                coordinates = coordinates,
                alternatives = "3",  // Request up to 3 alternative routes
                steps = "false",     // Disable steps to avoid geometry parsing issues
                geometries = "polyline",
                overview = "full"
            )
            
            if (!response.isSuccessful || response.body() == null) {
                return@withContext Result.failure(
                    Exception("OSRM API error: ${response.code()}")
                )
            }
            
            val osrmResponse = response.body()!!
            
            if (osrmResponse.code != "Ok" || osrmResponse.routes.isNullOrEmpty()) {
                return@withContext Result.failure(
                    Exception("No routes found")
                )
            }
            
            // Convert OSRM routes to EcoRoutes with eco-scores
            val ecoRoutes = osrmResponse.routes.mapIndexed { index, osrmRoute ->
                convertToEcoRoute(osrmRoute, index)
            }
            
            // Sort by LOWEST fuel consumption first (best eco option)
            // This ensures routes that use less total fuel are preferred
            val sortedRoutes = ecoRoutes.sortedBy { it.fuelEstimate }
            
            // Assign eco scores based on fuel ranking
            // Best fuel route gets highest score adjustment
            val routesWithAdjustedScores = sortedRoutes.mapIndexed { index, route ->
                // Bonus for better fuel efficiency ranking
                val fuelBonus = when (index) {
                    0 -> 10f  // Best route
                    1 -> 5f   // Second best
                    else -> 0f
                }
                val adjustedScore = (route.ecoScore + fuelBonus).coerceIn(0f, 100f)
                route.copy(
                    ecoScore = adjustedScore,
                    isRecommended = index == 0
                )
            }
            
            Result.success(routesWithAdjustedScores)
            
        } catch (e: Exception) {
            Log.e("RouteCalculator", "Error calculating routes", e)
            Result.failure(e)
        }
    }
    
    /**
     * Convert OSRM route to EcoRoute with eco-score calculation
     */
    private fun convertToEcoRoute(osrmRoute: OSRMRoute, id: Int): EcoRoute {
        // Decode polyline geometry
        val coordinates = PolylineDecoder.decode(osrmRoute.geometry)
        
        // Calculate distance and duration
        val distanceKm = osrmRoute.distance / 1000.0  // Convert meters to km
        val durationMin = osrmRoute.duration / 60.0   // Convert seconds to minutes
        
        // Estimate turns based on polyline complexity
        val turnsCount = estimateTurnsFromPolyline(coordinates)
        
        // Estimate eco-score for this route
        val ecoScore = estimateRouteEcoScore(distanceKm, durationMin, turnsCount)
        
        // Calculate fuel and CO2 based on eco-score
        val fuelEstimate = EcoScoreUtils.calculateFuelConsumption(ecoScore, distanceKm.toFloat())
        val co2Estimate = EcoScoreUtils.calculateCO2(fuelEstimate)
        
        return EcoRoute(
            id = id,
            distance = distanceKm,
            duration = durationMin,
            geometry = coordinates,
            ecoScore = ecoScore,
            fuelEstimate = fuelEstimate,
            co2Estimate = co2Estimate,
            turnsCount = turnsCount,
            isRecommended = false
        )
    }
    
    /**
     * Estimate number of turns from polyline complexity
     */
    private fun estimateTurnsFromPolyline(coordinates: List<LatLng>): Int {
        if (coordinates.size < 3) return 0
        
        var turns = 0
        for (i in 1 until coordinates.size - 1) {
            val prev = coordinates[i - 1]
            val curr = coordinates[i]
            val next = coordinates[i + 1]
            
            // Calculate bearing change
            val bearing1 = calculateBearing(prev, curr)
            val bearing2 = calculateBearing(curr, next)
            val bearingChange = kotlin.math.abs(bearing2 - bearing1)
            
            // If bearing changes > 30 degrees, consider it a turn
            if (bearingChange > 30 && bearingChange < 330) {
                turns++
            }
        }
        
        return turns
    }
    
    /**
     * Calculate bearing between two points
     */
    private fun calculateBearing(from: LatLng, to: LatLng): Double {
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val dLon = Math.toRadians(to.longitude - from.longitude)
        
        val y = kotlin.math.sin(dLon) * kotlin.math.cos(lat2)
        val x = kotlin.math.cos(lat1) * kotlin.math.sin(lat2) -
                kotlin.math.sin(lat1) * kotlin.math.cos(lat2) * kotlin.math.cos(dLon)
        
        return Math.toDegrees(kotlin.math.atan2(y, x))
    }
    
    /**
     * Estimate eco-score for a route based on fuel efficiency
     * Lower fuel consumption = higher eco score
     */
    private fun estimateRouteEcoScore(
        distanceKm: Double,
        durationMin: Double,
        turnsCount: Int
    ): Float {
        // Calculate average speed
        val avgSpeed = if (durationMin > 0) {
            ((distanceKm / durationMin) * 60).toFloat()  // km/h
        } else 50f
        
        // Calculate base fuel consumption rate (L/100km) based on route characteristics
        var fuelRate = 6.0f  // Base rate
        
        // Speed efficiency factor: 60-80 km/h is optimal (uses less fuel)
        val speedEfficiency = when {
            avgSpeed < 30f -> 1.5f   // Urban, stop-and-go (50% more fuel)
            avgSpeed < 50f -> 1.2f   // Slow speeds (20% more fuel)
            avgSpeed in 60f..80f -> 0.9f  // Optimal range (10% less fuel)
            avgSpeed > 100f -> 1.4f  // High speed (40% more fuel)
            avgSpeed > 90f -> 1.3f   // Fast (30% more fuel)
            else -> 1.0f  // Normal
        }
        
        // Turns efficiency: more turns = more acceleration/deceleration = more fuel
        // Calculate turns per 100km for normalization
        val turnsPerHundredKm = if (distanceKm > 0) (turnsCount / distanceKm) * 100 else 0.0
        val turnsEfficiency = when {
            turnsPerHundredKm > 20 -> 1.3f  // Very frequent turns
            turnsPerHundredKm > 15 -> 1.2f  // Frequent turns
            turnsPerHundredKm > 10 -> 1.1f  // Moderate turns
            else -> 1.0f  // Few turns
        }
        
        // Apply efficiency factors to base fuel rate
        fuelRate *= speedEfficiency * turnsEfficiency
        
        // Calculate total estimated fuel for this route
        val estimatedFuel = (fuelRate * distanceKm.toFloat()) / 100f
        
        // Calculate fuel per km as efficiency metric
        val fuelPerKm = estimatedFuel / distanceKm.toFloat()
        
        // Eco score based on fuel efficiency
        // Lower fuel/km = higher score
        // Typical range: 0.04-0.12 L/km
        // Map to 0-100 scale (inverted)
        val baseScore = when {
            fuelPerKm < 0.05f -> 95f  // Excellent efficiency
            fuelPerKm < 0.06f -> 85f  // Very good
            fuelPerKm < 0.07f -> 75f  // Good
            fuelPerKm < 0.08f -> 65f  // Average
            fuelPerKm < 0.09f -> 55f  // Below average
            fuelPerKm < 0.10f -> 45f  // Poor
            else -> 35f  // Very poor
        }
        
        // Apply time efficiency bonus/penalty
        // Faster routes (less time for same distance) are slightly better
        val timeEfficiency = if (durationMin > 0) {
            val speedRatio = avgSpeed / 60f  // Compare to 60 km/h baseline
            when {
                speedRatio > 1.3f -> -5f  // Too fast, speeding
                speedRatio > 1.1f -> 2f   // Good pace
                speedRatio > 0.9f -> 0f   // Normal
                speedRatio > 0.7f -> -3f  // Slower
                else -> -8f  // Very slow
            }
        } else 0f
        
        val finalScore = (baseScore + timeEfficiency).coerceIn(0f, 100f)
        
        Log.d("RouteCalculator", "Route score: $finalScore (fuel: $estimatedFuel L, fuel/km: $fuelPerKm, dist: $distanceKm km, speed: $avgSpeed km/h, turns: $turnsCount)")
        
        return finalScore
    }
}
