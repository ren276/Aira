# Features Research

**Domain:** Android Health & Fitness App (On-device AI + Wearables)
**Researched:** 2026-04-15
**Confidence:** HIGH

## Feature Categories

### Table Stakes (Must-haves to compete)

- **Comprehensive View of Vitals**: RHR, HRV, SpO2, Respiratory Rate, Skin Temp.
- **Health Connect Integration**: Silent background syncing of data. Users won't use an app that requires manual data entry.
- **Sleep Architecture**: Breaking down REM vs Deep vs Core, along with efficiency and continuity scores.
- **Cardio Load / Strain Models**: Standard TRIMP or WHOOP-like models based on max HR zones.
- **Local Persistence & Charts**: Ability to view historic 7/14/30-day trends dynamically.

### Differentiators (Competitive advantages)

- **Predictive & Adaptive Baselines (EMA)**: Exponential moving average baselines that actually adjust to the user's specific context, rather than generic age-based comparisons.
- **Aira AI Coach (Gemma 4 2B)**: True conversational AI running locally on-device.
- **On-Device Confidence Engines**: Displaying exact confidence weights derived from the specific hardware source (e.g. Garmin over generic phone sensors).
- **Causal Anomaly Explanations**: "Why did my recovery drop?" mathematically correlated with habits, not just a generic guess.
- **Local Hardening & Privacy**: Pure on-device ML compute, SQLCipher database encryption, Biometric App-Lock, and strict FLAG_SECURE.

### Anti-features (What NOT to build)

- **Medical Diagnostics**: Must aggressively avoid claiming to diagnose, treat, or prevent medical conditions. It is a wellness coaching tool.
- **Mandatory Cloud Compute for Core Scores**: Computing recovery scores must not require internet access or remote API calls. All 8 scores (Recovery, Sleep, Strain, Stress, Energy Bank, Readiness-to-Learn, Nutrition, Burnout Risk) must be calculated on-device.

---
*Features research for: Android Health & Fitness App (On-device AI + Wearables)*
*Researched: 2026-04-15*
