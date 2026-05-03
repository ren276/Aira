package com.aira.health.domain.ml

/**
 * Unified result type returned by all ML metric inference classes.
 *
 * @property score           Computed metric score in range [0, 100].
 * @property confidence      Data confidence in [0.0, 1.0]. Reflects how fully
 *                           the model's feature inputs were available.
 * @property source          Whether this result came from the ML model or
 *                           the heuristic fallback engine.
 */
data class MetricInferenceResult(
    val score: Int,
    val confidence: Float,
    val source: InferenceSource
) {
    enum class InferenceSource {
        /** Full TFLite model prediction with personalisation applied. */
        ML_MODEL,
        /** Deterministic formula fallback (insufficient history or model unavailable). */
        HEURISTIC_FALLBACK
    }
}
