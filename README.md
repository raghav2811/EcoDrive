# 🌱🚗 EcoDrive – Eco-Driving Android Application

A complete Android application built with **Kotlin**, **Jetpack Compose**, and **TensorFlow Lite** that tracks driving behavior and provides real-time eco-score feedback using on-device machine learning.

---

## 🚗 Features

### 📡 Real-time Sensor Data Collection

* GPS tracking (location, speed, distance)
* Accelerometer monitoring (acceleration patterns, harsh braking)

### 🧠 On-Device ML Inference

* TensorFlow Lite model for eco-score prediction
* StandardScaler feature normalization
* Real-time score updates every 2 seconds

### 🗺️ Live Trip Tracking

* Google Maps integration with live route display
* Real-time eco-score gauge (0–100)
* Fuel consumption estimation
* CO₂ emissions calculation

### 💡 Eco-Driving Tips

* Context-aware driving suggestions
* Performance-based feedback

---

## 📋 Prerequisites

* Android Studio Flamingo or later
* Android SDK 26 (Android 8.0) or higher
* Google Maps API Key
* TensorFlow Lite model file

---

## 🔧 Setup Instructions

### 1️⃣ Clone the Project

```bash
git clone <your-repo-url>
```

Or open directly in Android Studio.

---

### 2️⃣ Configure Google Maps API Key

1. Go to [https://console.cloud.google.com/](https://console.cloud.google.com/)
2. Enable **Maps SDK for Android**
3. Create credentials and copy your API key

Edit `local.properties`:

```properties
MAPS_API_KEY=YOUR_ACTUAL_API_KEY_HERE
```

---

### 3️⃣ Add TensorFlow Lite Model Files

Place these files inside:

```
app/src/main/assets/
```

#### 📦 eco_score_model.tflite

Model requirements:

* **Input:** 10 float values
* **Output:** 1 float value (eco_score 0–100)

Feature order (must match exactly):

```
0. acceleration_variation
1. stop_events
2. acceleration
3. speed
4. trip_duration
5. trip_distance
6. road_type_Urban
7. traffic_condition_Moderate
8. traffic_condition_Light
9. road_type_Rural
```

---

#### 📦 eco_feature_scaler.json

```json
{
  "mean": [mean_0, mean_1, ..., mean_9],
  "scale": [scale_0, scale_1, ..., scale_9]
}
```

Example:

```json
{
  "mean": [0.15, 3.2, 0.8, 45.3, 15.2, 5.4, 0.6, 0.3, 0.7, 0.4],
  "scale": [0.05, 2.1, 0.3, 15.2, 8.5, 3.2, 0.49, 0.46, 0.46, 0.49]
}
```

---

### 4️⃣ Build the Project

```bash
./gradlew build
```

Or:

Build → Make Project (Android Studio)

---

### 5️⃣ Run on Device

⚠️ Use a real Android device for accurate sensor data.

1. Enable Developer Options
2. Enable USB Debugging
3. Connect device
4. Click ▶ Run

---

## 📱 Permissions

The app requires:

* `ACCESS_FINE_LOCATION`
* `ACCESS_COARSE_LOCATION`
* `INTERNET`
* `ACCESS_NETWORK_STATE`

Permissions are requested at runtime.

---

## 🏗️ Project Structure

```
app/src/main/
├── java/com/ecodrive/app/
│   ├── data/
│   │   ├── Models.kt
│   │   └── ScalerData.kt
│   ├── ml/
│   │   └── EcoScoreInference.kt
│   ├── sensors/
│   │   ├── LocationManager.kt
│   │   ├── AccelerometerManager.kt
│   │   └── TripDataProcessor.kt
│   ├── ui/
│   │   ├── components/
│   │   │   ├── EcoScoreGauge.kt
│   │   │   └── StatsComponents.kt
│   │   ├── screens/
│   │   │   ├── HomeScreen.kt
│   │   │   └── LiveDriveScreen.kt
│   │   ├── theme/
│   │   │   └── Theme.kt
│   │   └── TripViewModel.kt
│   ├── utils/
│   │   └── Constants.kt
│   └── MainActivity.kt
├── assets/
│   ├── eco_score_model.tflite
│   └── eco_feature_scaler.json
└── res/
```

---

## 🔬 How It Works

### 1️⃣ Sensor Data Collection

* GPS updates every 1 second
* Accelerometer tracks acceleration patterns

### 2️⃣ Feature Engineering

The app calculates:

* `acceleration_variation`
* `stop_events`
* `acceleration`
* `speed`
* `trip_duration`
* `trip_distance`
* `road_type`
* `traffic_condition`

### 3️⃣ ML Inference Pipeline

```
Features → StandardScaler → TensorFlow Lite → Eco Score
```

Normalization formula:

```kotlin
normalized_value = (value - mean) / scale
```

### 4️⃣ Eco Score → Fuel → CO₂

```kotlin
// Base fuel rate: 6.0 L/100km
fuel_multiplier = 1.0 + ((100 - eco_score) / 100)
fuel_used = (6.0 * distance / 100) * fuel_multiplier

// CO₂: 2.31 kg per liter
co2_emitted = fuel_used * 2.31
```

---

## 🎨 UI Components

### 🏠 Home Screen

* Welcome message
* Start Trip button
* App overview

### 🚘 Live Drive Screen

* Real-time route map
* Animated eco-score gauge
* Distance, duration, speed stats
* Fuel consumption tracking
* CO₂ emissions tracking
* Eco-driving tips
* Stop trip button

---

## 🔐 Security Notes

Add to `.gitignore`:

```
local.properties
*.keystore
```

Never commit API keys.

---

## 📄 License

Created for demonstration purposes.

---

## 🤝 Contributing

1. Modify feature calculations → `TripDataProcessor.kt`
2. Update UI colors → `Theme.kt`
3. Adjust eco tips logic → `Constants.kt`
4. Retrain and replace the ML model

---

# 🌍 Drive Smarter. Drive Greener.
