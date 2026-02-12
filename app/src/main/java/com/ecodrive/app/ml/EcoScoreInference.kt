package com.ecodrive.app.ml

import android.content.Context
import com.ecodrive.app.data.ScalerData
import com.ecodrive.app.data.TripData
import com.ecodrive.app.data.RoadType
import com.ecodrive.app.data.TrafficCondition
import com.ecodrive.app.utils.Constants
import com.google.gson.Gson
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class EcoScoreInference(private val context: Context) {
    
    private var interpreter: Interpreter? = null
    private var scaler: ScalerData? = null
    
    init {
        loadModel()
        loadScaler()
    }
    
    private fun loadModel() {
        try {
            val assetFileDescriptor = context.assets.openFd(Constants.MODEL_PATH)
            val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = assetFileDescriptor.startOffset
            val declaredLength = assetFileDescriptor.declaredLength
            val modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
            
            interpreter = Interpreter(modelBuffer)
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException("Error loading TensorFlow Lite model: ${e.message}")
        }
    }
    
    private fun loadScaler() {
        try {
            val json = context.assets.open(Constants.SCALER_PATH).bufferedReader().use { it.readText() }
            scaler = Gson().fromJson(json, ScalerData::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException("Error loading scaler data: ${e.message}")
        }
    }
    
    /**
     * Prepare features in the exact order required by the model:
     * 0. acceleration_variation
     * 1. stop_events
     * 2. acceleration
     * 3. speed
     * 4. trip_duration
     * 5. trip_distance
     * 6. road_type_Urban
     * 7. traffic_condition_Moderate
     * 8. traffic_condition_Light
     * 9. road_type_Rural
     */
    private fun prepareFeaturesArray(tripData: TripData): FloatArray {
        val features = FloatArray(Constants.FEATURE_COUNT)
        
        // Original features
        features[0] = tripData.accelerationVariation
        features[1] = tripData.stopEvents.toFloat()
        features[2] = tripData.acceleration
        features[3] = tripData.averageSpeed
        features[4] = tripData.tripDuration
        features[5] = tripData.tripDistance
        
        // One-hot encoded: road_type_Urban
        features[6] = if (tripData.roadType == RoadType.URBAN) 1.0f else 0.0f
        
        // One-hot encoded: traffic_condition_Moderate
        features[7] = if (tripData.trafficCondition == TrafficCondition.MODERATE) 1.0f else 0.0f
        
        // One-hot encoded: traffic_condition_Light
        features[8] = if (tripData.trafficCondition == TrafficCondition.LIGHT) 1.0f else 0.0f
        
        // One-hot encoded: road_type_Rural
        features[9] = if (tripData.roadType == RoadType.RURAL) 1.0f else 0.0f
        
        return features
    }
    
    /**
     * Apply StandardScaler normalization: (x - mean) / scale
     */
    private fun normalizeFeatures(features: FloatArray): FloatArray {
        val scalerData = scaler ?: throw IllegalStateException("Scaler not loaded")
        
        if (features.size != scalerData.mean.size || features.size != scalerData.scale.size) {
            throw IllegalArgumentException("Feature size mismatch")
        }
        
        val normalized = FloatArray(features.size)
        for (i in features.indices) {
            normalized[i] = ((features[i] - scalerData.mean[i]) / scalerData.scale[i]).toFloat()
        }
        
        return normalized
    }
    
    /**
     * Run inference and return eco score (0-100)
     */
    fun predictEcoScore(tripData: TripData): Float {
        val interpreterInstance = interpreter ?: throw IllegalStateException("Model not loaded")
        
        try {
            // Step 1: Prepare features in correct order
            val rawFeatures = prepareFeaturesArray(tripData)
            
            // Step 2: Normalize using StandardScaler
            val normalizedFeatures = normalizeFeatures(rawFeatures)
            
            // Step 3: Prepare input buffer
            val inputBuffer = ByteBuffer.allocateDirect(Constants.FEATURE_COUNT * 4)
            inputBuffer.order(ByteOrder.nativeOrder())
            normalizedFeatures.forEach { inputBuffer.putFloat(it) }
            
            // Step 4: Prepare output buffer
            val outputBuffer = ByteBuffer.allocateDirect(4)  // Single float output
            outputBuffer.order(ByteOrder.nativeOrder())
            
            // Step 5: Run inference
            interpreterInstance.run(inputBuffer, outputBuffer)
            
            // Step 6: Get result
            outputBuffer.rewind()
            var ecoScore = outputBuffer.float
            
            // Step 7: Clamp to 0-100 range
            ecoScore = ecoScore.coerceIn(0f, 100f)
            
            return ecoScore
            
        } catch (e: Exception) {
            e.printStackTrace()
            // Return a default middle score on error
            return 50f
        }
    }
    
    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
