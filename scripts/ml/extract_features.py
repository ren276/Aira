"""
extract_features.py

Reads the local Health Connect export SQLite DB and produces per-day
feature rows for each Aira metric model.

Usage:
    python extract_features.py --db ../../.planning/health_connect_export.db --out data/

Output CSVs:
    data/recovery_features.csv    — for RecoveryModel
    data/strain_features.csv      — for StrainModel
    data/stress_features.csv      — for StressModel
    data/sleep_features.csv       — for SleepModel

All values are normalized to [0.0, 1.0] before saving.
"""

import argparse
import sqlite3
import pathlib
import pandas as pd
import numpy as np


def load_db(path: str) -> sqlite3.Connection:
    conn = sqlite3.connect(path)
    conn.row_factory = sqlite3.Row
    return conn


def _safe_norm(series: pd.Series, lo: float = 0.0, hi: float = None) -> pd.Series:
    """Min-max normalise; if hi is None use series max."""
    hi = hi if hi is not None else series.max()
    lo = lo if lo is not None else series.min()
    denom = hi - lo
    if denom == 0:
        return pd.Series(np.zeros(len(series)), index=series.index)
    return ((series.clip(lo, hi) - lo) / denom).astype("float32")


def extract_recovery(conn: sqlite3.Connection, out_dir: pathlib.Path):
    """
    Features  : hrv_norm, rhr_norm, sleep_score_norm, prior_strain_norm
    Label     : recovery_score (heuristic ground-truth, 0-100 scaled to 0-1)
    """
    query = """
    SELECT
        date,
        hrv_rmssd_morning          AS hrv,
        rhr                        AS rhr,
        sleep_quality_score        AS sleep_score,
        LAG(strain_score, 1) OVER (ORDER BY date) AS prior_strain,
        recovery_score             AS label
    FROM daily_summary
    WHERE recovery_score IS NOT NULL
    ORDER BY date;
    """
    df = pd.read_sql_query(query, conn)
    df.dropna(inplace=True)

    # Normalise against population-level reference ranges
    df["hrv_norm"]          = _safe_norm(df["hrv"], 20, 120)
    df["rhr_norm"]          = _safe_norm(df["rhr"].apply(lambda r: 1 - (r - 40) / 60))  # lower RHR → better
    df["sleep_score_norm"]  = _safe_norm(df["sleep_score"], 0, 100)
    df["prior_strain_norm"] = _safe_norm(df["prior_strain"], 0, 100)
    df["label"]             = _safe_norm(df["label"], 0, 100)

    out = df[["date", "hrv_norm", "rhr_norm", "sleep_score_norm", "prior_strain_norm", "label"]]
    out.to_csv(out_dir / "recovery_features.csv", index=False)
    print(f"[recovery] {len(out)} rows → {out_dir / 'recovery_features.csv'}")


def extract_strain(conn: sqlite3.Connection, out_dir: pathlib.Path):
    """
    Features  : z1_min, z2_min, z3_min, z4_min, z5_min, active_min (all normed)
    Label     : strain_score (0-100 scaled to 0-1)
    """
    query = """
    SELECT
        date,
        zone1_minutes AS z1, zone2_minutes AS z2, zone3_minutes AS z3,
        zone4_minutes AS z4, zone5_minutes AS z5, total_active_minutes AS active,
        strain_score  AS label
    FROM daily_summary
    WHERE strain_score IS NOT NULL
    ORDER BY date;
    """
    df = pd.read_sql_query(query, conn)
    df.dropna(inplace=True)

    for col in ["z1", "z2", "z3", "z4", "z5", "active"]:
        df[col] = _safe_norm(df[col], 0, 120)

    df["label"] = _safe_norm(df["label"], 0, 100)
    out = df[["date", "z1", "z2", "z3", "z4", "z5", "active", "label"]]
    out.to_csv(out_dir / "strain_features.csv", index=False)
    print(f"[strain] {len(out)} rows → {out_dir / 'strain_features.csv'}")


def extract_stress(conn: sqlite3.Connection, out_dir: pathlib.Path):
    """
    Features  : hrv_norm, sleep_quality_norm, steps_norm, calorie_deficit_norm
    Label     : stress_score (0-100 scaled to 0-1)
    """
    query = """
    SELECT
        date,
        hrv_rmssd_morning   AS hrv,
        sleep_quality_score AS sleep_q,
        steps               AS steps,
        calorie_deficit     AS cal_def,
        stress_score        AS label
    FROM daily_summary
    WHERE stress_score IS NOT NULL
    ORDER BY date;
    """
    df = pd.read_sql_query(query, conn)
    df.dropna(inplace=True)

    df["hrv_norm"]      = _safe_norm(df["hrv"], 20, 120)
    df["sleep_q_norm"]  = _safe_norm(df["sleep_q"], 0, 100)
    df["steps_norm"]    = _safe_norm(df["steps"], 0, 25000)
    df["cal_def_norm"]  = _safe_norm(df["cal_def"], -1000, 1000)
    df["label"]         = _safe_norm(df["label"], 0, 100)

    out = df[["date", "hrv_norm", "sleep_q_norm", "steps_norm", "cal_def_norm", "label"]]
    out.to_csv(out_dir / "stress_features.csv", index=False)
    print(f"[stress] {len(out)} rows → {out_dir / 'stress_features.csv'}")


def extract_sleep(conn: sqlite3.Connection, out_dir: pathlib.Path):
    """
    Features  : duration_norm, rem_pct, deep_pct, interruptions_norm
    Label     : sleep_quality_score (0-100 scaled to 0-1)
    """
    query = """
    SELECT
        date,
        sleep_duration_minutes    AS duration,
        rem_sleep_minutes         AS rem_min,
        deep_sleep_minutes        AS deep_min,
        sleep_interruptions       AS interruptions,
        sleep_quality_score       AS label
    FROM daily_summary
    WHERE sleep_quality_score IS NOT NULL
    ORDER BY date;
    """
    df = pd.read_sql_query(query, conn)
    df.dropna(inplace=True)

    df["duration_norm"]      = _safe_norm(df["duration"], 0, 600)  # up to 10 h
    df["rem_pct"]            = _safe_norm(df["rem_min"] / df["duration"].clip(lower=1), 0, 0.35)
    df["deep_pct"]           = _safe_norm(df["deep_min"] / df["duration"].clip(lower=1), 0, 0.25)
    df["interruptions_norm"] = _safe_norm(df["interruptions"], 0, 10)
    df["label"]              = _safe_norm(df["label"], 0, 100)

    out = df[["date", "duration_norm", "rem_pct", "deep_pct", "interruptions_norm", "label"]]
    out.to_csv(out_dir / "sleep_features.csv", index=False)
    print(f"[sleep]  {len(out)} rows → {out_dir / 'sleep_features.csv'}")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--db",  required=True, help="Path to Health Connect export SQLite DB")
    parser.add_argument("--out", required=True, help="Output directory for CSV files")
    args = parser.parse_args()

    out_dir = pathlib.Path(args.out)
    out_dir.mkdir(parents=True, exist_ok=True)

    conn = load_db(args.db)

    extract_recovery(conn, out_dir)
    extract_strain(conn, out_dir)
    extract_stress(conn, out_dir)
    extract_sleep(conn, out_dir)

    conn.close()
    print("Feature extraction complete.")


if __name__ == "__main__":
    main()
