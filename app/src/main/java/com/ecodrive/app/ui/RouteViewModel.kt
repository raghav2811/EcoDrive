package com.ecodrive.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ecodrive.app.data.LocationPoint
import com.ecodrive.app.ml.EcoScoreInference
import com.ecodrive.app.routing.EcoRoute
import com.ecodrive.app.routing.LatLng
import com.ecodrive.app.routing.RouteCalculator
import com.ecodrive.app.sensors.LocationManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RouteViewModel(application: Application) : AndroidViewModel(application) {
    
    private val locationManager = LocationManager(application)
    private val ecoScoreInference = EcoScoreInference(application)
    private val routeCalculator = RouteCalculator(ecoScoreInference)
    
    private val _currentLocation = MutableStateFlow<LocationPoint?>(null)
    val currentLocation: StateFlow<LocationPoint?> = _currentLocation.asStateFlow()
    
    private val _destinationLat = MutableStateFlow("")
    val destinationLat: StateFlow<String> = _destinationLat.asStateFlow()
    
    private val _destinationLng = MutableStateFlow("")
    val destinationLng: StateFlow<String> = _destinationLng.asStateFlow()
    
    private val _routes = MutableStateFlow<List<EcoRoute>>(emptyList())
    val routes: StateFlow<List<EcoRoute>> = _routes.asStateFlow()
    
    private val _isCalculating = MutableStateFlow(false)
    val isCalculating: StateFlow<Boolean> = _isCalculating.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    init {
        // Get current location on init
        viewModelScope.launch {
            android.util.Log.d("RouteViewModel", "Starting location collection")
            locationManager.getLocationUpdates().collect { location ->
                android.util.Log.d("RouteViewModel", "Location received: ${location.latitude}, ${location.longitude}")
                _currentLocation.value = location
            }
        }
    }
    
    fun updateDestinationLat(value: String) {
        _destinationLat.value = value
    }
    
    fun updateDestinationLng(value: String) {
        _destinationLng.value = value
    }
    
    fun calculateRoutes() {
        val current = _currentLocation.value
        val destLat = _destinationLat.value.toDoubleOrNull()
        val destLng = _destinationLng.value.toDoubleOrNull()
        
        if (current == null) {
            _errorMessage.value = "Waiting for current location..."
            return
        }
        
        if (destLat == null || destLng == null) {
            _errorMessage.value = "Invalid destination coordinates"
            return
        }
        
        _isCalculating.value = true
        _errorMessage.value = null
        
        viewModelScope.launch {
            val result = routeCalculator.calculateEcoRoutes(
                startLat = current.latitude,
                startLng = current.longitude,
                destLat = destLat,
                destLng = destLng
            )
            
            result.fold(
                onSuccess = { routes ->
                    _routes.value = routes
                    _isCalculating.value = false
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "Failed to calculate routes"
                    _isCalculating.value = false
                }
            )
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
    
    override fun onCleared() {
        super.onCleared()
        ecoScoreInference.close()
    }
}
