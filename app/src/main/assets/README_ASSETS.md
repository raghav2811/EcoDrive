# ASSETS SETUP GUIDE

## ⚠️ REQUIRED FILES

You MUST add these two files to this directory before the app will work:

### 1. eco_score_model.tflite
**Location**: `app/src/main/assets/eco_score_model.tflite`

This is your TensorFlow Lite model file.

**Requirements**:
- Input: 10 float values (see order below)
- Output: 1 float value (eco_score between 0-100)

**Input Feature Order** (CRITICAL - DO NOT CHANGE):
```
Index 0:  acceleration_variation
Index 1:  stop_events
Index 2:  acceleration
Index 3:  speed
Index 4:  trip_duration
Index 5:  trip_distance
Index 6:  road_type_Urban        (one-hot encoded: 1 or 0)
Index 7:  traffic_condition_Moderate (one-hot encoded: 1 or 0)
Index 8:  traffic_condition_Light    (one-hot encoded: 1 or 0)
Index 9:  road_type_Rural        (one-hot encoded: 1 or 0)
```

### 2. eco_feature_scaler.json
**Location**: `app/src/main/assets/eco_feature_scaler.json`

This file contains StandardScaler parameters from your training process.

**Format**:
```json
{
  "mean": [
    mean_value_for_feature_0,
    mean_value_for_feature_1,
    ...
    mean_value_for_feature_9
  ],
  "scale": [
    scale_value_for_feature_0,
    scale_value_for_feature_1,
    ...
    scale_value_for_feature_9
  ]
}
```

**Example** (you must replace with your actual values):
```json
{
  "mean": [
    0.1523,
    3.245,
    0.8234,
    45.3421,
    15.234,
    5.432,
    0.6234,
    0.3124,
    0.6876,
    0.3766
  ],
  "scale": [
    0.0543,
    2.123,
    0.312,
    15.234,
    8.543,
    3.234,
    0.4854,
    0.4632,
    0.4632,
    0.4854
  ]
}
```

## 📝 How to Get These Files

### From Python/Scikit-learn Training:

```python
import joblib
import json
import tensorflow as tf

# After training your model with scikit-learn
scaler = StandardScaler()
X_scaled = scaler.fit_transform(X)

# Save scaler parameters
scaler_params = {
    "mean": scaler.mean_.tolist(),
    "scale": scaler.scale_.tolist()
}

with open('eco_feature_scaler.json', 'w') as f:
    json.dump(scaler_params, f, indent=2)

# Convert your model to TensorFlow Lite
# (if using TensorFlow/Keras)
converter = tf.lite.TFLiteConverter.from_keras_model(model)
tflite_model = converter.convert()

with open('eco_score_model.tflite', 'wb') as f:
    f.write(tflite_model)
```

## ✅ Verification

After adding both files, your assets directory should look like:
```
app/src/main/assets/
├── eco_score_model.tflite
└── eco_feature_scaler.json
```

## 🧪 Testing Without Real Model

The provided `eco_feature_scaler.json` has default values (mean=0, scale=1) which means no scaling is applied.

If you want to test the app without a real model initially:
1. Create a dummy TensorFlow Lite model that returns a constant score
2. Use the default scaler values provided

**Note**: The app will crash if `eco_score_model.tflite` is missing!

## 🔍 Troubleshooting

**Error: "Model not found"**
- Ensure file is named exactly: `eco_score_model.tflite`
- Check it's in the correct directory
- Clean and rebuild project

**Error: "Scaler data parsing failed"**
- Validate JSON syntax
- Ensure arrays have exactly 10 elements
- Check no trailing commas

**Wrong predictions**
- Verify feature order matches training
- Confirm scaler mean/scale values are correct
- Check model input/output dimensions

---

**Once these files are added, rebuild the project in Android Studio!**
