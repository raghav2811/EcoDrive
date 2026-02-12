package com.ecodrive.app.sensors

import com.ecodrive.app.data.*
import com.ecodrive.app.utils.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

class TripDataProcessor {
    
    private val _tripData = MutableStateFlow(TripData())
    val tripData: StateFlow<TripData> = _tripData.asStateFlow()
    
    private var startTime: Long = 0
    private var previousLocation: LocationPoint? = null
    private var totalDistance: Float = 0f
    
    private val accelerationReadings = mutableListOf<Float>()
    private val speedReadings = mutableListOf<Float>()
    private var previousSpeed: Float = 0f
    private var stopCount: Int = 0
    
    private var waitTimeSeconds: Float = 0f
    private var isMoving: Boolean = false
    private var lastAccelerometerMagnitude: Float = 0f
    private val MOVEMENT_THRESHOLD = 0.5f  // Accelerometer threshold to detect movement
    private val GPS_SPEED_THRESHOLD = 1.0f  // km/h - minimum speed to consider as moving
    
    fun startTrip() {
        startTime = System.currentTimeMillis()
        previousLocation = null
        totalDistance = 0f
        accelerationReadings.clear()
        speedReadings.clear()
        previousSpeed = 0f
        stopCount = 0
        waitTimeSeconds = 0f
        isMoving = false
        lastAccelerometerMagnitude = 0f
        _tripData.value = TripData()
    }
    
    fun updateLocation(location: LocationPoint) {
        previousLocation?.let { prev ->
            val segmentDistance = LocationManager.calculateDistance(prev, location)
            val durationSeconds = (location.timestamp - prev.timestamp) / 1000f
            val gpsSpeed = LocationManager.calculateSpeed(segmentDistance, durationSeconds)
            
            // Determine if actually moving using accelerometer + GPS
            val actuallyMoving = isMoving && gpsSpeed > GPS_SPEED_THRESHOLD
            
            if (actuallyMoving) {
                // Only add distance when actually moving
                totalDistance += segmentDistance
                speedReadings.add(gpsSpeed)
                
                // Detect stop events (was moving, now stopped)
                if (previousSpeed > Constants.STOP_SPEED_THRESHOLD && gpsSpeed <= GPS_SPEED_THRESHOLD) {
                    stopCount++
                }
            } else {
                // Phone is stationary or GPS drift
                waitTimeSeconds += durationSeconds
                // Add zero speed to maintain accurate average
                speedReadings.add(0f)
            }
            
            previousSpeed = gpsSpeed
            updateTripData()
        }
        
        previousLocation = location
    }
    
    fun updateAccelerometer(data: AccelerometerData) {
        // Use magnitude as acceleration indicator
        val magnitude = abs(data.magnitude)
        accelerationReadings.add(magnitude)
        
        // Detect movement based on accelerometer change
        // If magnitude changes significantly, phone is moving
        val magnitudeChange = abs(magnitude - lastAccelerometerMagnitude)
        isMoving = magnitudeChange > MOVEMENT_THRESHOLD || magnitude > 9.8f + MOVEMENT_THRESHOLD
        
        lastAccelerometerMagnitude = magnitude
        
        // Keep only recent readings (last 100)
        if (accelerationReadings.size > 100) {
            accelerationReadings.removeAt(0)
        }
        
        updateTripData()
    }
    
    private fun updateTripData() {
        val durationMinutes = ((System.currentTimeMillis() - startTime) / 60000f)
        
        val avgSpeed = if (speedReadings.isNotEmpty()) {
            speedReadings.average().toFloat()
        } else 0f
        
        val avgAcceleration = if (accelerationReadings.isNotEmpty()) {
            accelerationReadings.average().toFloat()
        } else 0f
        
        val accelerationVariation = if (accelerationReadings.size > 1) {
            calculateStandardDeviation(accelerationReadings)
        } else 0f
        
        // Simple heuristic for road type based on speed
        val roadType = if (avgSpeed < 50f) RoadType.URBAN else RoadType.RURAL
        
        // Simple heuristic for traffic based on stop events
        val trafficCondition = if (stopCount > 5) TrafficCondition.MODERATE else TrafficCondition.LIGHT
        
        _tripData.value = TripData(
            tripDistance = totalDistance,
            tripDuration = durationMinutes,
            averageSpeed = avgSpeed,
            acceleration = avgAcceleration,
            accelerationVariation = accelerationVariation,
            stopEvents = stopCount,
            roadType = roadType,
            trafficCondition = trafficCondition,
            waitTime = waitTimeSeconds / 60f  // Convert to minutes
        )
    }
    
    fun getCurrentTripData(): TripData {
        return _tripData.value
    }
    
    private fun calculateStandardDeviation(values: List<Float>): Float {
        if (values.isEmpty()) return 0f
        
        val mean = values.average()
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return kotlin.math.sqrt(variance).toFloat()
    }
}
