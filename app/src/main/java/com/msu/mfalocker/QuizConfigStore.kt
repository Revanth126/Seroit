package com.msu.mfalocker

import android.util.Log
import org.json.JSONObject
import java.io.File

class QuizConfigStore(private val filesDir: File) {

    companion object {
        private const val TAG = "QuizConfigStore"
        private const val FILE_NAME = "quiz_config.json"
    }

    /** Returns the QuizConfig for the given package, or null if not set. */
    fun getQuizConfig(packageName: String): QuizConfig? {
        val map = readMap() ?: return null
        if (!map.has(packageName)) return null
        return try {
            val obj = map.getJSONObject(packageName)
            QuizConfig(
                topic = obj.getString("topic"),
                difficulty = obj.getString("difficulty")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse QuizConfig for $packageName", e)
            null
        }
    }

    /** Persists the QuizConfig for the given package. */
    fun setQuizConfig(packageName: String, config: QuizConfig) {
        val map = readMap() ?: JSONObject()
        try {
            val obj = JSONObject()
            obj.put("topic", config.topic)
            obj.put("difficulty", config.difficulty)
            map.put(packageName, obj)
            File(filesDir, FILE_NAME).writeText(map.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write $FILE_NAME", e)
        }
    }

    /** Removes the QuizConfig for the given package. */
    fun removeQuizConfig(packageName: String) {
        val map = readMap() ?: return
        try {
            map.remove(packageName)
            File(filesDir, FILE_NAME).writeText(map.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write $FILE_NAME after removal", e)
        }
    }

    // --- private helpers ---

    private fun readMap(): JSONObject? {
        val file = File(filesDir, FILE_NAME)
        if (!file.exists()) return JSONObject()
        return try {
            JSONObject(file.readText())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse $FILE_NAME, deleting corrupted file", e)
            file.delete()
            null
        }
    }
}
