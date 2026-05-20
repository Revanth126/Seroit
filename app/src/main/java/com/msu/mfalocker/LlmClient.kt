package com.msu.mfalocker

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object LlmClient {

    suspend fun generateQuiz(topic: String, difficulty: String): List<QuizQuestion> =
        withContext(Dispatchers.IO) {
            val url = URL("${BuildConfig.LLM_BASE_URL}/chat/completions")
            val connection = url.openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "Bearer ${BuildConfig.LLM_API_KEY}")
                connection.doOutput = true
                connection.connectTimeout = 30_000
                connection.readTimeout = 60_000

                val requestBody = buildRequestBody(topic, difficulty)
                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(requestBody)
                }

                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw IllegalStateException("LLM API returned HTTP $responseCode")
                }

                val responseText = connection.inputStream.bufferedReader(Charsets.UTF_8).readText()
                parseResponse(responseText)
            } finally {
                connection.disconnect()
            }
        }

    private fun buildRequestBody(topic: String, difficulty: String): String {
        val systemMessage = JSONObject().apply {
            put("role", "system")
            put(
                "content",
                "You are a quiz generator. Always respond with a valid JSON array and nothing else."
            )
        }

        val userPrompt = """
            Generate exactly 5 multiple-choice questions about "$topic" at "$difficulty" difficulty.
            Respond ONLY with a JSON array of exactly 5 objects. Each object must have:
            - "question": the question text (string)
            - "options": an array of exactly 4 answer strings (A, B, C, D content without letter prefix)
            - "correctIndex": integer 0-3 indicating the correct option
            - "explanation": a brief explanation of the correct answer (string)
            Do not include any text outside the JSON array.
        """.trimIndent()

        val userMessage = JSONObject().apply {
            put("role", "user")
            put("content", userPrompt)
        }

        return JSONObject().apply {
            put("model", BuildConfig.LLM_MODEL)
            put("messages", JSONArray().apply {
                put(systemMessage)
                put(userMessage)
            })
        }.toString()
    }

    internal fun parseResponse(responseText: String): List<QuizQuestion> {
        val root = try {
            JSONObject(responseText)
        } catch (e: Exception) {
            throw IllegalStateException("Malformed LLM response: not a JSON object", e)
        }

        val content = try {
            root.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        } catch (e: Exception) {
            throw IllegalStateException("Malformed LLM response: missing choices[0].message.content", e)
        }

        // Strip markdown code fences if present (e.g. Gemini wraps in ```json ... ```)
        val cleanContent = content.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```")
            .trim()

        val questionsArray = try {
            JSONArray(cleanContent)
        } catch (e: Exception) {
            throw IllegalStateException("LLM response content is not a valid JSON array", e)
        }

        if (questionsArray.length() != 5) {
            throw IllegalStateException(
                "Expected exactly 5 questions but got ${questionsArray.length()}"
            )
        }

        return (0 until questionsArray.length()).map { i ->
            val obj = questionsArray.getJSONObject(i)
            val optionsArray = obj.getJSONArray("options")
            if (optionsArray.length() != 4) {
                throw IllegalStateException(
                    "Question $i must have exactly 4 options but has ${optionsArray.length()}"
                )
            }
            val options = (0 until optionsArray.length()).map { j -> optionsArray.getString(j) }
            val correctIndex = obj.getInt("correctIndex")
            if (correctIndex !in 0..3) {
                throw IllegalStateException(
                    "Question $i correctIndex must be 0-3 but is $correctIndex"
                )
            }
            QuizQuestion(
                question = obj.getString("question"),
                options = options,
                correctIndex = correctIndex,
                explanation = obj.getString("explanation")
            )
        }
    }
}
