package com.aira.health.data.model

/**
 * Maps Android package names to confidence weight integers (0–100).
 *
 * Strategy: "Highest Confidence Source Wins" — when two data points overlap for the
 * same timestamp, the one associated with the higher-tier package is preferred.
 *
 * Tiers:
 *   Tier 1 (100) — Dedicated, high-precision medical/sports wearables (Oura, Whoop)
 *   Tier 2  (85) — Dedicated fitness GPS watches (Garmin, Fitbit, Coros, Polar, Suunto)
 *   Tier 3  (65) — Consumer smartwatches with health features (Samsung, Google Pixel Watch)
 *   Tier 4  (40) — Phone-only or unrecognised source
 */
object ConfidenceRouter {

    // Tier 1 — High-precision dedicated wearables
    private val tier1 = setOf(
        "com.ouraring.oura",            // Oura Ring
        "com.whoop.android",            // Whoop Strap
        "com.withings.wiscale2",        // Withings
        "com.fitbit.FitbitMobile"       // Fitbit (premium HR accuracy tier)
    )

    // Tier 2 — Dedicated fitness watches
    private val tier2 = setOf(
        "com.garmin.android.apps.connectmobile", // Garmin Connect
        "com.coros.wearable",                    // Coros
        "com.polar.polarbeat",                   // Polar
        "com.suunto.app",                        // Suunto
        "com.wahoo.app",                         // Wahoo
        "com.strava"                             // Strava
    )

    // Tier 3 — Consumer smartwatch platforms
    private val tier3 = setOf(
        "com.samsung.android.app.shealth",       // Samsung Health
        "com.google.android.apps.fitness",       // Google Fit / Pixel Watch
        "com.apple.health"                       // Apple Health bridge apps (rare)
    )

    /**
     * Returns the confidence weight (0–100) for the given source package name.
     *
     * @param packageName The [androidx.health.connect.client.records.metadata.DataOrigin.packageName]
     *   from a Health Connect record's metadata.
     * @return Integer tier weight: 100, 85, 65, or 40.
     */
    fun getConfidenceWeight(packageName: String): Int = when (packageName) {
        in tier1 -> 100
        in tier2 -> 85
        in tier3 -> 65
        else -> 40
    }

    /**
     * Returns a float confidence (0.0–1.0) normalised from the integer weight.
     * Used for Room entity fields typed as Float.
     */
    fun getConfidenceFloat(packageName: String): Float = getConfidenceWeight(packageName) / 100f

    /**
     * Given two package names, returns the one with the higher confidence.
     * Used during conflict resolution when two sources overlap the same time window.
     */
    fun preferredSource(a: String, b: String): String =
        if (getConfidenceWeight(a) >= getConfidenceWeight(b)) a else b
}
