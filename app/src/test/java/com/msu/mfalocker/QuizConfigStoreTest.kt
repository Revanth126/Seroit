package com.msu.mfalocker

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.io.File
import java.nio.file.Files

/**
 * Unit tests for QuizConfigStore.
 *
 * Validates: Requirements 2.4, 8.1, 8.3
 */
class QuizConfigStoreTest : StringSpec({

    // Requirement 8.1 — round-trip persist/read
    "round-trip: setQuizConfig then getQuizConfig returns the same config" {
        val tempDir = Files.createTempDirectory("quiz_config_test").toFile()
        try {
            val store = QuizConfigStore(tempDir)
            val config = QuizConfig(topic = "Science", difficulty = "Medium")

            store.setQuizConfig("com.example.app", config)
            val result = store.getQuizConfig("com.example.app")

            result shouldNotBe null
            result!!.topic shouldBe "Science"
            result.difficulty shouldBe "Medium"
        } finally {
            tempDir.deleteRecursively()
        }
    }

    // Requirement 8.1 — multiple packages stored independently
    "round-trip: multiple packages are stored and retrieved independently" {
        val tempDir = Files.createTempDirectory("quiz_config_multi_test").toFile()
        try {
            val store = QuizConfigStore(tempDir)
            val config1 = QuizConfig(topic = "History", difficulty = "Easy")
            val config2 = QuizConfig(topic = "Mathematics", difficulty = "Hard")

            store.setQuizConfig("com.example.app1", config1)
            store.setQuizConfig("com.example.app2", config2)

            val result1 = store.getQuizConfig("com.example.app1")
            val result2 = store.getQuizConfig("com.example.app2")

            result1!!.topic shouldBe "History"
            result1.difficulty shouldBe "Easy"
            result2!!.topic shouldBe "Mathematics"
            result2.difficulty shouldBe "Hard"
        } finally {
            tempDir.deleteRecursively()
        }
    }

    // Requirement 8.2 — removal
    "removal: after removeQuizConfig, getQuizConfig returns null for that package" {
        val tempDir = Files.createTempDirectory("quiz_config_remove_test").toFile()
        try {
            val store = QuizConfigStore(tempDir)
            val config = QuizConfig(topic = "Geography", difficulty = "Easy")

            store.setQuizConfig("com.example.app", config)
            store.removeQuizConfig("com.example.app")

            store.getQuizConfig("com.example.app") shouldBe null
        } finally {
            tempDir.deleteRecursively()
        }
    }

    // Requirement 8.2 — removal only affects the target package
    "removal: removing one package does not affect other packages" {
        val tempDir = Files.createTempDirectory("quiz_config_remove_other_test").toFile()
        try {
            val store = QuizConfigStore(tempDir)
            store.setQuizConfig("com.example.app1", QuizConfig("Science", "Hard"))
            store.setQuizConfig("com.example.app2", QuizConfig("Aptitude", "Medium"))

            store.removeQuizConfig("com.example.app1")

            store.getQuizConfig("com.example.app1") shouldBe null
            store.getQuizConfig("com.example.app2") shouldNotBe null
        } finally {
            tempDir.deleteRecursively()
        }
    }

    // Requirement 8.3 — corrupted file recovery
    "corrupted file: getQuizConfig returns null and deletes the corrupted file" {
        val tempDir = Files.createTempDirectory("quiz_config_corrupt_test").toFile()
        try {
            val store = QuizConfigStore(tempDir)
            val configFile = File(tempDir, "quiz_config.json")
            configFile.writeText("{ this is not valid json !!!")

            val result = store.getQuizConfig("com.example.app")

            result shouldBe null
            configFile.exists() shouldBe false
        } finally {
            tempDir.deleteRecursively()
        }
    }

    // Missing file — fresh store returns null
    "missing file: getQuizConfig on a fresh store returns null for any package" {
        val tempDir = Files.createTempDirectory("quiz_config_missing_test").toFile()
        try {
            val store = QuizConfigStore(tempDir)

            store.getQuizConfig("com.example.app") shouldBe null
        } finally {
            tempDir.deleteRecursively()
        }
    }
})
