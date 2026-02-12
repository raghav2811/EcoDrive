package com.ecodrive.app.utils

object Constants {
    // ML Model
    const val MODEL_PATH = "eco_score_model.tflite"
    const val SCALER_PATH = "eco_feature_scaler.json"
    
    // Feature order for ML model (CRITICAL - DO NOT CHANGE)
    const val FEATURE_COUNT = 10
    
    // Fuel and CO2 calculations
    const val BASE_FUEL_RATE = 6.0f  // L/100km
    const val CO2_PER_LITER = 2.31f  // kg CO2 per liter
    
    // Sensor update intervals
    const val LOCATION_UPDATE_INTERVAL = 1000L  // 1 second
    const val ACCELEROMETER_UPDATE_INTERVAL = 100000  // 100ms in microseconds
    
    // Thresholds
    const val STOP_SPEED_THRESHOLD = 5.0f  // km/h
    const val MIN_TRIP_DISTANCE = 0.1f  // km
}

object EcoScoreUtils {
    fun getEcoScoreColor(score: Float): Long {
        return when {
            score >= 80 -> 0xFF4CAF50  // Green
            score >= 60 -> 0xFFFFC107  // Yellow
            else -> 0xFFF44336         // Red
        }
    }
    
    fun getEcoTips(score: Float): List<String> {
        return when {
            score >= 80 -> listOf(
                "Excellent driving! Keep it up.",
                "Your smooth acceleration saves fuel.",
                "You're making a positive environmental impact."
            )
            score >= 60 -> listOf(
                "Good driving, but there's room for improvement.",
                "Try to avoid sudden accelerations.",
                "Maintain steady speed when possible.",
                "Anticipate traffic to reduce braking."
            )
            else -> listOf(
                "Your driving could be more eco-friendly.",
                "Avoid harsh acceleration and braking.",
                "Try to maintain a steady speed.",
                "Reduce aggressive driving to save fuel.",
                "Plan ahead to minimize stops."
            )
        }
    }
    
    fun calculateFuelConsumption(ecoScore: Float, distanceKm: Float, waitTimeMin: Float = 0f): Float {
        // Base fuel consumption for driving
        val multiplier = 1.0f + ((100 - ecoScore) / 100f)
        val drivingFuel = (Constants.BASE_FUEL_RATE * distanceKm / 100f) * multiplier
        
        // Idling fuel consumption (engine running while stationary)
        // Average car idles at ~0.6-1.0 L/hour, we'll use 0.8 L/hour
        val idlingFuelPerHour = 0.8f
        val idlingFuel = (waitTimeMin / 60f) * idlingFuelPerHour
        
        return drivingFuel + idlingFuel
    }
    
    fun calculateCO2(fuelLiters: Float): Float {
        return fuelLiters * Constants.CO2_PER_LITER
    }
}
