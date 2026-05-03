"""
models/recovery_model.py

Recovery Score ML model definition.


Architecture: Small 3-layer dense neural network (4 inputs → 32 → 16 → 1)
  - Trained to output a 0.0–1.0 recovery score
  - Inputs mirror RecoveryEngine.kt heuristic inputs so we can bootstrap from known-good labels

Input features (normalised 0.0–1.0):
    [hrv_norm, rhr_norm, sleep_score_norm, prior_strain_norm]

Output:
    [recovery_score]  — single float, 0.0–1.0  (multiply by 100 for UI)
"""

import numpy as np
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import MinMaxScaler
import tensorflow as tf
from tensorflow import keras

FEATURE_COLS = ["hrv_norm", "rhr_norm", "sleep_score_norm", "prior_strain_norm"]
LABEL_COL    = "label"
MODEL_NAME   = "recovery_model"


def build_model(input_dim: int = 4) -> keras.Model:
    model = keras.Sequential([
        keras.Input(shape=(input_dim,), name="features"),
        keras.layers.Dense(32, activation="relu", name="dense_1"),
        keras.layers.Dropout(0.1),
        keras.layers.Dense(16, activation="relu", name="dense_2"),
        keras.layers.Dense(1, activation="sigmoid", name="output"),  # output is 0–1
    ], name=MODEL_NAME)
    model.compile(optimizer="adam", loss="mse", metrics=["mae"])
    return model


def train(data_path: str, out_dir: str, epochs: int = 80, batch_size: int = 16):
    df = pd.read_csv(data_path)
    df = df[FEATURE_COLS + [LABEL_COL]].dropna()

    X = df[FEATURE_COLS].values.astype("float32")
    y = df[LABEL_COL].values.astype("float32").reshape(-1, 1)

    X_train, X_val, y_train, y_val = train_test_split(X, y, test_size=0.2, random_state=42)

    model = build_model(input_dim=X.shape[1])

    early_stop = keras.callbacks.EarlyStopping(monitor="val_loss", patience=10, restore_best_weights=True)

    history = model.fit(
        X_train, y_train,
        validation_data=(X_val, y_val),
        epochs=epochs,
        batch_size=batch_size,
        callbacks=[early_stop],
        verbose=1
    )

    save_path = f"{out_dir}/{MODEL_NAME}"
    model.save(save_path)
    print(f"[{MODEL_NAME}] Saved to {save_path}")
    print(f"[{MODEL_NAME}] val_mae = {min(history.history['val_mae']):.4f}")
    return model


if __name__ == "__main__":
    import argparse, pathlib
    p = argparse.ArgumentParser()
    p.add_argument("--data", required=True)
    p.add_argument("--out",  required=True)
    args = p.parse_args()
    pathlib.Path(args.out).mkdir(parents=True, exist_ok=True)
    train(args.data, args.out)
