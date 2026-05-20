package com.msu.mfalocker

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * Unit tests for LlmClient.parseResponse.
 *
 * Validates: Requirements 3.3, 3.5
 */
class LlmClientTest : StringSpec({

    fun makeContent(questions: List<Map<String, Any>>): String {
        val sb = StringBuilder("[")
        questions.forEachIndexed { i, q ->
            if (i > 0) sb.append(",")
            val opts = (q["options"] as List<*>).joinToString(",") { "\"$it\"" }
            sb.append("""{"question":"${q["question"]}","options":[$opts],"correctIndex":${q["correctIndex"]},"explanation":"${q["explanation"]}"}""")
        }
        sb.append("]")
        return sb.toString()
    }

    fun makeResponse(content: String): String =
        """{"choices":[{"message":{"content":"${content.replace("\"", "\\\"")}","role":"assistant"}}]}"""

    fun fiveQuestions(
        optionCount: Int = 4,
        correctIndex: Int = 0
    ): List<Map<String, Any>> = (1..5).map { n ->
        mapOf(
            "question" to "Question $n",
            "options" to (1..optionCount).map { "Option $it" },
            "correctIndex" to correctIndex,
            "explanation" to "Explanation $n"
        )
    }

    // Requirement 3.3 — valid response parses to 5 QuizQuestion objects
    "valid response: 5 questions parse correctly" {
        val content = makeContent(fiveQuestions())
        val responseText = makeResponse(content)

        val result = LlmClient.parseResponse(responseText)

        result.size shouldBe 5
        result[0].question shouldBe "Question 1"
        result[0].options shouldBe listOf("Option 1", "Option 2", "Option 3", "Option 4")
        result[0].correctIndex shouldBe 0
        result[0].explanation shouldBe "Explanation 1"
    }

    // Requirement 3.5 — malformed JSON throws IllegalStateException
    "malformed JSON: throws IllegalStateException" {
        shouldThrow<IllegalStateException> {
            LlmClient.parseResponse("this is not json at all")
        }
    }

    // Requirement 3.5 — wrong question count throws IllegalStateException
    "wrong question count (3 questions): throws IllegalStateException" {
        val content = makeContent((1..3).map { n ->
            mapOf(
                "question" to "Q$n",
                "options" to listOf("A", "B", "C", "D"),
                "correctIndex" to 0,
                "explanation" to "E$n"
            )
        })
        val responseText = makeResponse(content)

        shouldThrow<IllegalStateException> {
            LlmClient.parseResponse(responseText)
        }
    }

    // Requirement 3.5 — missing choices field throws IllegalStateException
    "missing choices field: throws IllegalStateException" {
        val responseText = """{"model":"gpt-4o-mini","usage":{}}"""

        shouldThrow<IllegalStateException> {
            LlmClient.parseResponse(responseText)
        }
    }

    // Requirement 3.5 — wrong options count (3 options) throws IllegalStateException
    "wrong options count (3 options): throws IllegalStateException" {
        val content = makeContent(fiveQuestions(optionCount = 3))
        val responseText = makeResponse(content)

        shouldThrow<IllegalStateException> {
            LlmClient.parseResponse(responseText)
        }
    }

    // Requirement 3.5 — correctIndex out of range throws IllegalStateException
    "correctIndex out of range (5): throws IllegalStateException" {
        val content = makeContent(fiveQuestions(correctIndex = 5))
        val responseText = makeResponse(content)

        shouldThrow<IllegalStateException> {
            LlmClient.parseResponse(responseText)
        }
    }
})
