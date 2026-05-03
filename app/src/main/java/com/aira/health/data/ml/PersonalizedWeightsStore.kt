package com.aira.health.data.ml

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists per-metric bias corrections that represent on-device personalisation.
 *
 * ### What it stores
 * For each metric (recovery, strain, stress, sleep) a single `Float` bias in [-0.1, 0.1]
 * is maintained. This bias is added to the TFLite model's raw [0,1] output BEFORE scaling
 * to 0-100, allowing the global model to shift up or down for this specific user without
 * retraining.
 *
 * ### Privacy
 * The bias file is stored in the app's **internal private storage** and is encrypted using
 * AES-256 via [EncryptedFile] + Android Keystore — raw biometrics never leave the device.
 *
 * ### Learning trigger
 * Callers accumulate observed score deltas (e.g. user corrected a score via feedback)
 * and call [applyCorrection] to nudge the stored bias.
 */
@Singleton
class PersonalizedWeightsStore @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG         = "PersonalizedWeightsStore"
        private const val FILE_NAME   = "ml_bias.enc"
        private const val LEARN_RATE  = 0.02f   // per correction
        private const val MAX_BIAS    = 0.10f   // ±10% of [0-1] range
    }

    @Serializable
    private data class BiasMap(
        val recovery: Float = 0f,
        val strain:   Float = 0f,
        val stress:   Float = 0f,
        val sleep:    Float = 0f
    )

    private var biasMap: BiasMap = load()

    /** Return the stored bias for a given metric key (e.g. "recovery"). */
    fun getBias(metric: String): Float = when (metric) {
        "recovery" -> biasMap.recovery
        "strain"   -> biasMap.strain
        "stress"   -> biasMap.stress
        "sleep"    -> biasMap.sleep
        else       -> 0f
    }

    /**
     * Nudge the bias for [metric] by [observedDelta] × LEARN_RATE.
     *
     * @param observedDelta Positive means model underestimated, negative means overestimated.
     *                      Should be in normalised [0,1] range (i.e. delta/100).
     */
    fun applyCorrection(metric: String, observedDelta: Float) {
        val nudge = (observedDelta * LEARN_RATE).coerceIn(-MAX_BIAS, MAX_BIAS)
        biasMap = when (metric) {
            "recovery" -> biasMap.copy(recovery = (biasMap.recovery + nudge).coerceIn(-MAX_BIAS, MAX_BIAS))
            "strain"   -> biasMap.copy(strain   = (biasMap.strain   + nudge).coerceIn(-MAX_BIAS, MAX_BIAS))
            "stress"   -> biasMap.copy(stress   = (biasMap.stress   + nudge).coerceIn(-MAX_BIAS, MAX_BIAS))
            "sleep"    -> biasMap.copy(sleep    = (biasMap.sleep    + nudge).coerceIn(-MAX_BIAS, MAX_BIAS))
            else       -> biasMap
        }
        persist()
    }

    private fun load(): BiasMap {
        return try {
            val file = encryptedFile() ?: return BiasMap()
            val content = file.openFileInput().bufferedReader().readText()
            Json.decodeFromString<BiasMap>(content)
        } catch (e: Exception) {
            Log.d(TAG, "No existing bias file, starting fresh.")
            BiasMap()
        }
    }

    private fun persist() {
        try {
            val plainFile = File(context.filesDir, FILE_NAME)
            if (plainFile.exists()) plainFile.delete()         // EncryptedFile can't overwrite
            val file = encryptedFile() ?: return
            file.openFileOutput().use { out ->
                out.write(Json.encodeToString(biasMap).toByteArray())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist bias corrections", e)
        }
    }

    private fun encryptedFile(): EncryptedFile? {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedFile.Builder(
                context,
                File(context.filesDir, FILE_NAME),
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()
        } catch (e: Exception) {
            Log.e(TAG, "EncryptedFile unavailable", e)
            null
        }
    }
}
