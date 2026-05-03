"""
convert_to_tflite.py

Converts trained TensorFlow SavedModels to TFLite flatbuffers and copies
them to app/src/main/assets/models/ for packaging into the APK.

Usage:
    python convert_to_tflite.py --in trained_models/ --out ../../app/src/main/assets/models/

Conversion options:
    --quantize     Apply dynamic-range quantization for smaller model size (recommended)
"""

import argparse
import pathlib
import tensorflow as tf


MODELS = ["recovery_model", "strain_model", "stress_model", "sleep_model"]


def convert(model_dir: pathlib.Path, out_dir: pathlib.Path, quantize: bool = True):
    out_dir.mkdir(parents=True, exist_ok=True)

    for model_name in MODELS:
        saved_model_path = model_dir / model_name
        if not saved_model_path.exists():
            print(f"[{model_name}] Not found at {saved_model_path} — skipping.")
            continue

        converter = tf.lite.TFLiteConverter.from_saved_model(str(saved_model_path))

        if quantize:
            converter.optimizations = [tf.lite.Optimize.DEFAULT]

        tflite_model = converter.convert()

        out_path = out_dir / f"{model_name}.tflite"
        out_path.write_bytes(tflite_model)
        size_kb = len(tflite_model) / 1024
        print(f"[{model_name}] → {out_path}  ({size_kb:.1f} KB)")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--in",      dest="model_dir", required=True)
    parser.add_argument("--out",     dest="out_dir",   required=True)
    parser.add_argument("--quantize", action="store_true", default=True,
                        help="Apply dynamic-range quantization (default: True)")
    args = parser.parse_args()

    convert(pathlib.Path(args.model_dir), pathlib.Path(args.out_dir), args.quantize)
    print("\nConversion complete. Run ./gradlew assembleDevDebug to bundle the models.")


if __name__ == "__main__":
    main()
