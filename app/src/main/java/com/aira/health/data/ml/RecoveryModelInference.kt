package com.aira.health.data.ml

import android.content.Context
import com.aira.health.domain.ml.MetricInferenceResult
import com.aira.health.domain.ml.MetricInferenceResult.InferenceSource
import com.aira.health.domain.ml.PhysiologicalModelProxy
import org.tensorflow.lite.Interpreter
import javax.inject.Inject

/**
 * TFLite inference class for **Recovery Score**.
 *
 * Input tensor  : [hrv_norm, rhr_norm, sleep_score_norm, prior_strain_norm]  (4 floats)
 * Output tensor : [recovery_score]  (1 float, 0.0–1.0 → scaled to 0–100)
 *
 * Falls back to [InferenceSource.HEURISTIC_FALLBACK] when the model isn't loaded.
 */
class RecoveryModelInference @Inject constructor(
    context: Context,
    loader: TFLiteModelLoader,
    private val weightsStore: PersonalizedWeightsStore
) : PhysiologicalModelProxy {

    private val interpreter: Interpreter? = loader.load("recovery_model.tflite")

    override val isAvailable: Boolean get() = interpreter != null

    /**
     * @param features FloatArray of size 4: [hrv_norm, rhr_norm, sleep_score_norm, prior_strain_norm]
     */
    override fun infer(features: FloatArray): MetricInferenceResult {
        val interp = interpreter
            ?: return MetricInferenceResult(score = 0, confidence = 0f, source = InferenceSource.HEURISTIC_FALLBACK)

        require(features.size == 4) { "RecoveryModel expects 4 features, got ${features.size}" }

        val inputTensor  = arrayOf(features)
        val outputTensor = Array(1) { FloatArray(1) }

        interp.run(inputTensor, outputTensor)

        val rawScore = outputTensor[0][0]          // 0.0–1.0
        val bias     = weightsStore.getBias("recovery")
        val adjusted = (rawScore + bias).coerceIn(0f, 1f)
        val confidence = features.count { it > 0f }.toFloat() / features.size.toFloat()

        return MetricInferenceResult(
            score      = (adjusted * 100).toInt(),
            confidence = confidence,
            source     = InferenceSource.ML_MODEL
        )
    }

    override fun close() { interpreter?.close() }
}
