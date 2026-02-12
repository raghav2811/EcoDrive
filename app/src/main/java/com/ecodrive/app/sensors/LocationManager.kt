package com.ecodrive.app.sensors

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.ecodrive.app.data.LocationPoint
import com.ecodrive.app.utils.Constants
import com.google.android.gms.location.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class LocationManager(private val context: Context) {
    
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    
    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        Constants.LOCATION_UPDATE_INTERVAL
    ).apply {
        setMinUpdateIntervalMillis(Constants.LOCATION_UPDATE_INTERVAL / 2)
        setWaitForAccurateLocation(false)
    }.build()
    
    @SuppressLint("MissingPermission")
    fun getLocationUpdates(): Flow<LocationPoint> = callbackFlow {
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { location ->
                    android.util.Log.d("LocationManager", "Location update: ${location.latitude}, ${location.longitude}")
                    trySend(
                        LocationPoint(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            }
            
            override fun onLocationAvailability(availability: LocationAvailability) {
                android.util.Log.d("LocationManager", "Location availability: ${availability.isLocationAvailable}")
            }
        }
        
        android.util.Log.d("LocationManager", "Requesting location updates")
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            callback,
            Looper.getMainLooper()
        )
        
        awaitClose {
            android.util.Log.d("LocationManager", "Removing location updates")
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }
    
    @SuppressLint("MissingPermission")
    suspend fun getLastKnownLocation(): LocationPoint? {
        return try {
            val location = fusedLocationClient.lastLocation.result
            location?.let {
                LocationPoint(
                    latitude = it.latitude,
                    longitude = it.longitude,
                    timestamp = System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            null
        }
    }
    
    companion object {
        fun calculateDistance(start: LocationPoint, end: LocationPoint): Float {
            val results = FloatArray(1)
            Location.distanceBetween(
                start.latitude, start.longitude,
                end.latitude, end.longitude,
                results
            )
            return results[0] / 1000f // Convert to km
        }
        
        fun calculateSpeed(distance: Float, durationSeconds: Float): Float {
            if (durationSeconds <= 0) return 0f
            return (distance / durationSeconds) * 3600f // km/h
        }
    }
}
