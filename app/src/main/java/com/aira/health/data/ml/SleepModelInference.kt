package com.aira.health.data.ml

import android.content.Context
import com.aira.health.domain.ml.MetricInferenceResult
import com.aira.health.domain.ml.MetricInferenceResult.InferenceSource
import com.aira.health.domain.ml.PhysiologicalModelProxy
import org.tensorflow.lite.Interpreter
import javax.inject.Inject

/**
 * TFLite inference class for **Sleep Quality Score**.
 *
 * Input tensor  : [duration_norm, rem_pct, deep_pct, interruptions_norm]  (4 floats)
 * Output tensor : [sleep_score]  (1 float, 0.0–1.0 → scaled to 0–100)
 */
class SleepModelInference @Inject constructor(
    context: Context,
    loader: TFLiteModelLoader,
    private val weightsStore: PersonalizedWeightsStore
) : PhysiologicalModelProxy {

    private val interpreter: Interpreter? = loader.load("sleep_model.tflite")
    override val isAvailable: Boolean get() = interpreter != null

    /** @param features [duration_norm, rem_pct, deep_pct, interruptions_norm] */
    override fun infer(features: FloatArray): MetricInferenceResult {
        val interp = interpreter
            ?: return MetricInferenceResult(0, 0f, InferenceSource.HEURISTIC_FALLBACK)

        require(features.size == 4) { "SleepModel expects 4 features, got ${features.size}" }

        val inputTensor  = arrayOf(features)
        val outputTensor = Array(1) { FloatArray(1) }
        interp.run(inputTensor, outputTensor)

        val rawScore = outputTensor[0][0]
        val bias     = weightsStore.getBias("sleep")
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
