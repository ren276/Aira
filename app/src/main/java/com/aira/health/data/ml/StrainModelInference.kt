package com.aira.health.data.ml

import android.content.Context
import com.aira.health.domain.ml.MetricInferenceResult
import com.aira.health.domain.ml.MetricInferenceResult.InferenceSource
import com.aira.health.domain.ml.PhysiologicalModelProxy
import org.tensorflow.lite.Interpreter
import javax.inject.Inject

/**
 * TFLite inference class for **Strain Score**.
 *
 * Input tensor  : [z1, z2, z3, z4, z5, active]  (6 floats, normalised 0-1)
 * Output tensor : [strain_score]  (1 float, 0.0–1.0 → scaled to 0–100)
 */
class StrainModelInference @Inject constructor(
    context: Context,
    loader: TFLiteModelLoader,
    private val weightsStore: PersonalizedWeightsStore
) : PhysiologicalModelProxy {

    private val interpreter: Interpreter? = loader.load("strain_model.tflite")
    override val isAvailable: Boolean get() = interpreter != null

    /** @param features [z1, z2, z3, z4, z5, active] — 6 normalised zone minute values */
    override fun infer(features: FloatArray): MetricInferenceResult {
        val interp = interpreter
            ?: return MetricInferenceResult(0, 0f, InferenceSource.HEURISTIC_FALLBACK)

        require(features.size == 6) { "StrainModel expects 6 features, got ${features.size}" }

        val inputTensor  = arrayOf(features)
        val outputTensor = Array(1) { FloatArray(1) }
        interp.run(inputTensor, outputTensor)

        val rawScore = outputTensor[0][0]
        val bias     = weightsStore.getBias("strain")
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
