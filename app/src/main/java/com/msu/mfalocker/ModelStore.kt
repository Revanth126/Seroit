package com.msu.mfalocker

import android.util.Log
import java.io.File

class ModelStore(private val filesDir: File) {

    companion object {
        private const val TAG = "ModelStore"
        private const val MODEL_FILE = "llm_model.txt"
    }

    /** Returns the persisted model. Falls back to DEFAULT if file absent or unrecognised. */
    fun getModel(): GeminiModel {
        val file = File(filesDir, MODEL_FILE)
        if (!file.exists()) return GeminiModel.DEFAULT
        return try {
            val raw = file.readText().trim()
            GeminiModel.valueOf(raw)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read or parse $MODEL_FILE, deleting and returning DEFAULT", e)
            file.delete()
            GeminiModel.DEFAULT
        }
    }

    /** Persists the selected model to llm_model.txt. */
    fun setModel(model: GeminiModel) {
        try {
            File(filesDir, MODEL_FILE).writeText(model.name)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write $MODEL_FILE", e)
        }
    }
}
