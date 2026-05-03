"""
train_all.py

Trains all four physiological metric models and saves them as SavedModels.

Usage:
    python train_all.py --data data/ --out trained_models/
"""

import argparse
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).parent))

from models.recovery_model import train as train_recovery
from models.strain_model   import train as train_strain
from models.stress_model   import train as train_stress
from models.sleep_model    import train as train_sleep


MODELS = [
    ("recovery", "data/recovery_features.csv", train_recovery),
    ("strain",   "data/strain_features.csv",   train_strain),
    ("stress",   "data/stress_features.csv",   train_stress),
    ("sleep",    "data/sleep_features.csv",     train_sleep),
]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", required=True, help="Directory containing *_features.csv files")
    parser.add_argument("--out",  required=True, help="Directory for SavedModel outputs")
    args = parser.parse_args()

    data_dir = pathlib.Path(args.data)
    out_dir  = pathlib.Path(args.out)
    out_dir.mkdir(parents=True, exist_ok=True)

    for name, csv_rel, train_fn in MODELS:
        csv_path = data_dir / pathlib.Path(csv_rel).name
        if not csv_path.exists():
            print(f"[{name}] WARNING: {csv_path} not found — skipping.")
            continue
        print(f"\n{'='*50}")
        print(f"Training [{name}]...")
        print(f"{'='*50}")
        train_fn(str(csv_path), str(out_dir))

    print("\nAll models trained successfully.")


if __name__ == "__main__":
    main()
