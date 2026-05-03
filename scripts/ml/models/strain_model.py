"""
models/strain_model.py

Strain Score ML model definition.

Architecture: 6-input dense network (zone 1-5 minutes + active minutes → 32 → 16 → 1)
  - Uses logarithmic zone weighting idea from StrainEngine.kt as built-in prior knowledge

Input features (normalised 0.0–1.0):
    [z1, z2, z3, z4, z5, active]   — heart rate zone minutes and total active

Output:
    [strain_score]  — single float, 0.0–1.0  (multiply by 100 for UI)
"""

import numpy as np
import pandas as pd
from sklearn.model_selection import train_test_split
import tensorflow as tf
from tensorflow import keras

FEATURE_COLS = ["z1", "z2", "z3", "z4", "z5", "active"]
LABEL_COL    = "label"
MODEL_NAME   = "strain_model"


def build_model(input_dim: int = 6) -> keras.Model:
    model = keras.Sequential([
        keras.Input(shape=(input_dim,), name="features"),
        keras.layers.Dense(32, activation="relu", name="dense_1"),
        keras.layers.Dropout(0.1),
        keras.layers.Dense(16, activation="relu", name="dense_2"),
        keras.layers.Dense(1, activation="sigmoid", name="output"),
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
    print(f"[{MODEL_NAME}] Saved to {save_path} | val_mae={min(history.history['val_mae']):.4f}")
    return model


if __name__ == "__main__":
    import argparse, pathlib
    p = argparse.ArgumentParser()
    p.add_argument("--data", required=True)
    p.add_argument("--out",  required=True)
    args = p.parse_args()
    pathlib.Path(args.out).mkdir(parents=True, exist_ok=True)
    train(args.data, args.out)
