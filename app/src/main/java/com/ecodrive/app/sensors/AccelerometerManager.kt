package com.ecodrive.app.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.sqrt

data class AccelerometerData(
    val x: Float,
    val y: Float,
    val z: Float,
    val magnitude: Float,
    val timestamp: Long
)

class AccelerometerManager(context: Context) {
    
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    
    fun getAccelerometerUpdates(): Flow<AccelerometerData> = callbackFlow {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                
                // Calculate magnitude (removing gravity)
                val magnitude = sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH
                
                trySend(
                    AccelerometerData(
                        x = x,
                        y = y,
                        z = z,
                        magnitude = magnitude,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
            
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Not needed
            }
        }
        
        accelerometer?.let {
            sensorManager.registerListener(
                listener,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
        
        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
    
    fun isAvailable(): Boolean = accelerometer != null
}
