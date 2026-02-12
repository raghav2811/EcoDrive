package com.ecodrive.app.data

data class TripData(
    val tripDistance: Float = 0f,      // km
    val tripDuration: Float = 0f,      // minutes
    val averageSpeed: Float = 0f,      // km/h
    val acceleration: Float = 0f,      // m/s²
    val accelerationVariation: Float = 0f,
    val stopEvents: Int = 0,
    val roadType: RoadType = RoadType.URBAN,
    val trafficCondition: TrafficCondition = TrafficCondition.LIGHT,
    val waitTime: Float = 0f          // minutes spent stationary
)

enum class RoadType {
    URBAN,
    RURAL
}

enum class TrafficCondition {
    LIGHT,
    MODERATE
}

data class EcoDriveResult(
    val ecoScore: Float,
    val fuelUsed: Float,    // liters
    val co2Emitted: Float,  // kg
    val tripData: TripData
)

data class LocationPoint(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
)
