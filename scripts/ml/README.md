# aira-ml — On-Device Physiological Metric Models

Python training + TFLite conversion pipeline for Aira's core health-scoring ML models.
All models are designed to run fully on-device via TFLite. **No user data is used outside the device.**

## Models

| Model | Input Features | Output | Fallback |
|-------|---------------|--------|----------|
| `recovery_model` | HRV norm, RHR norm, sleep score, prior strain | 0–100 score | `RecoveryEngine.kt` |
| `strain_model` | Zone 1-5 minutes, active minutes | 0–100 score | `StrainEngine.kt` |
| `stress_model` | HRV norm, sleep quality, steps, calorie deficit | 0–100 score | `StressEngine.kt` |
| `sleep_model` | Sleep duration, rem%, deep%, interruptions | 0–100 score | `SleepEngine.kt` |

## Quickstart

```bash
# 1. Set up environment
cd scripts/ml
python -m venv .venv
source .venv/bin/activate   # Windows: .venv\Scripts\activate
pip install -r requirements.txt

# 2. Simulate training data (uses Health Connect export DB from .planning/)
python extract_features.py --db ../../.planning/health_connect_export.db --out data/

# 3. Train all models
python train_all.py --data data/ --out trained_models/

# 4. Convert to TFLite
python convert_to_tflite.py --in trained_models/ --out ../../app/src/main/assets/models/

# 5. Copy assets and rebuild the app
./gradlew assembleDevDebug
```

## Directory Structure

```
scripts/ml/
  requirements.txt         — Python dependencies
  extract_features.py      — Room/SQLite feature extraction
  train_all.py             — Trains all four models
  convert_to_tflite.py     — Converts SavedModels → TFLite flatbuffers
  models/
    recovery_model.py      — Recovery score model definition
    strain_model.py        — Strain score model definition
    stress_model.py        — Stress score model definition
    sleep_model.py         — Sleep quality model definition
  data/                    — (git-ignored) Local extracted feature CSVs
  trained_models/          — (git-ignored) SavedModel checkpoints
```

## Privacy Contract

- Training data is extracted from the **local** Room/SQLCipher DB only.
- No data is uploaded to any cloud service during training or inference.
- Model weight files (`.tflite`) stored in `app/src/main/assets/models/` contain **no user data**, only learned weight matrices.

## On-Device Personalization

After deployment, the Android app's `PersonalizedWeightsStore` can accumulate bias corrections
per user, which are applied as post-processing adjustments on top of the global TFLite model predictions.
These corrections are AES-encrypted and stored in internal storage.
