package com.aira.health.data.ml

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject

/**
 * Loads a `.tflite` model file from `app/src/main/assets/models/`
 * and returns an [Interpreter] ready for inference.
 *
 * Call [close] to release native resources when done.
 *
 * Model files ship as assets to avoid bundling large files in source control.
 * Use `scripts/ml/convert_to_tflite.py` to generate them.
 */
class TFLiteModelLoader @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "TFLiteModelLoader"
        private const val ASSETS_MODEL_DIR = "models"

        /** Default inference options: 2 threads, no NNAPI (stable on all devices). */
        private fun defaultOptions() = Interpreter.Options().apply {
            setNumThreads(2)
            setUseNNAPI(false)
        }
    }

    /**
     * Load a TFLite model by [modelFileName] (e.g. `"recovery_model.tflite"`).
     *
     * @return A ready [Interpreter], or `null` if the file is not bundled in assets.
     */
    fun load(modelFileName: String): Interpreter? {
        return try {
            val buffer = loadModelBuffer("$ASSETS_MODEL_DIR/$modelFileName")
            Interpreter(buffer, defaultOptions())
        } catch (e: Exception) {
            Log.w(TAG, "TFLite model [$modelFileName] not found in assets — ML mode disabled.", e)
            null
        }
    }

    private fun loadModelBuffer(assetPath: String): MappedByteBuffer {
        val afd = context.assets.openFd(assetPath)
        val inputStream = FileInputStream(afd.fileDescriptor)
        val channel = inputStream.channel
        return channel.map(FileChannel.MapMode.READ_ONLY, afd.startOffset, afd.declaredLength)
    }
}
