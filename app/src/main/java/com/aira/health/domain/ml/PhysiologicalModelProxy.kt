package com.aira.health.domain.ml

/**
 * Abstract contract for any on-device physiological metric ML model.
 *
 * Each concrete implementation wraps a TFLite interpreter and handles:
 *  - Input tensor population
 *  - Inference execution
 *  - Output denormalization (0-1 → 0-100)
 *  - Personalisation bias application from [PersonalizedWeightsStore]
 */
interface PhysiologicalModelProxy {

    /**
     * Returns true when ≥14 days of history exist and the TFLite model file
     * is available in assets — i.e. when ML mode can be activated.
     */
    val isAvailable: Boolean

    /**
     * Run inference and return the scored + confidence result.
     * Implementations MUST call [close] when the proxy is no longer needed.
     *
     * All numeric inputs are expected in [0.0, 1.0] normalised form.
     */
    fun infer(features: FloatArray): MetricInferenceResult

    /** Release TFLite interpreter resources. */
    fun close()
}
