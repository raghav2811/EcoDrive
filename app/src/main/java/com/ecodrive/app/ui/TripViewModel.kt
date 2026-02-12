package com.ecodrive.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ecodrive.app.data.EcoDriveResult
import com.ecodrive.app.data.LocationPoint
import com.ecodrive.app.data.TripData
import com.ecodrive.app.ml.EcoScoreInference
import com.ecodrive.app.sensors.AccelerometerManager
import com.ecodrive.app.sensors.LocationManager
import com.ecodrive.app.sensors.TripDataProcessor
import com.ecodrive.app.utils.Constants
import com.ecodrive.app.utils.EcoScoreUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TripViewModel(application: Application) : AndroidViewModel(application) {
    
    private val locationManager = LocationManager(application)
    private val accelerometerManager = AccelerometerManager(application)
    private val tripDataProcessor = TripDataProcessor()
    private val ecoScoreInference = EcoScoreInference(application)
    
    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()
    
    private val _currentLocation = MutableStateFlow<LocationPoint?>(null)
    val currentLocation: StateFlow<LocationPoint?> = _currentLocation.asStateFlow()
    
    private val _ecoResult = MutableStateFlow<EcoDriveResult?>(null)
    val ecoResult: StateFlow<EcoDriveResult?> = _ecoResult.asStateFlow()
    
    private val _tripData = MutableStateFlow(TripData())
    val tripData: StateFlow<TripData> = _tripData.asStateFlow()
    
    fun startTrip() {
        _isTracking.value = true
        tripDataProcessor.startTrip()
        
        android.util.Log.d("TripViewModel", "Trip started, requesting location updates")
        
        // Set initial state immediately
        updateEcoScore()
        
        // Start location updates
        viewModelScope.launch {
            locationManager.getLocationUpdates().collect { location ->
                android.util.Log.d("TripViewModel", "Location received: ${location.latitude}, ${location.longitude}")
                if (_isTracking.value) {
                    _currentLocation.value = location
                    tripDataProcessor.updateLocation(location)
                    updateEcoScore()
                }
            }
        }
        
        // Start accelerometer updates
        viewModelScope.launch {
            accelerometerManager.getAccelerometerUpdates().collect { data ->
                if (_isTracking.value) {
                    tripDataProcessor.updateAccelerometer(data)
                    updateEcoScore()
                }
            }
        }
        
        // Periodic eco score updates
        viewModelScope.launch {
            while (_isTracking.value) {
                kotlinx.coroutines.delay(2000) // Update every 2 seconds
                updateEcoScore()
            }
        }
    }
    
    fun stopTrip() {
        _isTracking.value = false
        updateEcoScore() // Final update
    }
    
    private fun updateEcoScore() {
        val currentTripData = tripDataProcessor.getCurrentTripData()
        _tripData.value = currentTripData
        
        android.util.Log.d("TripViewModel", "Updating eco score - distance: ${currentTripData.tripDistance} km, moving: ${currentTripData.tripDistance > 0}")
        
        // Always show initial state immediately with current data
        if (currentTripData.tripDistance < 0.001f) {
            // Show starting state - user hasn't moved yet
            _ecoResult.value = EcoDriveResult(
                ecoScore = 0f,  // No score yet
                fuelUsed = EcoScoreUtils.calculateFuelConsumption(50f, 0f, currentTripData.waitTime),
                co2Emitted = 0f,
                tripData = currentTripData
            )
            return
        }
        
        try {
            val ecoScore = ecoScoreInference.predictEcoScore(currentTripData)
            val fuelUsed = EcoScoreUtils.calculateFuelConsumption(
                ecoScore,
                currentTripData.tripDistance,
                currentTripData.waitTime
            )
            val co2Emitted = EcoScoreUtils.calculateCO2(fuelUsed)
            
            _ecoResult.value = EcoDriveResult(
                ecoScore = ecoScore,
                fuelUsed = fuelUsed,
                co2Emitted = co2Emitted,
                tripData = currentTripData
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        ecoScoreInference.close()
    }
}
