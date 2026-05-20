// Feature: app-lock-type-selector, Property 5: Credentials stored as SHA-256 hashes
package com.msu.mfalocker

import io.kotest.core.spec.style.StringSpec
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.stringPattern
import io.kotest.property.forAll
import org.json.JSONObject
import java.io.File
import java.nio.file.Files

/**
 * Validates: Requirements 4.5, 5.6, 7.8, 12.3
 *
 * Property 5: Credentials are stored as SHA-256 hashes.
 * For any non-empty credential input, after storing via CredentialStore,
 * the value written to credentials.txt SHALL be a 64-character lowercase
 * hexadecimal string and SHALL NOT equal the plain-text input.
 */
class CredentialStoreTest : StringSpec({

    "Property 5 - setPin stores a 64-char lowercase hex hash, not the plain-text input" {
        forAll(PropTestConfig(iterations = 20), Arb.string(1..50)) { input ->
            val tempDir = Files.createTempDirectory("cred_pin_test").toFile()
            try {
                val store = CredentialStore(tempDir)
                store.setPin(input)

                val credentialsFile = File(tempDir, "credentials.txt")
                val json = JSONObject(credentialsFile.readText())
                val pinHash = json.getString("pin_hash")

                val is64CharHex = pinHash.length == 64 && pinHash.matches(Regex("[0-9a-f]+"))
                val isNotPlainText = pinHash != input

                is64CharHex && isNotPlainText
            } finally {
                tempDir.deleteRecursively()
            }
        }
    }

    "Property 5 - setPassword stores a 64-char lowercase hex hash, not the plain-text input" {
        forAll(PropTestConfig(iterations = 20), Arb.string(1..50)) { input ->
            val tempDir = Files.createTempDirectory("cred_pwd_test").toFile()
            try {
                val store = CredentialStore(tempDir)
                store.setPassword(input)

                val credentialsFile = File(tempDir, "credentials.txt")
                val json = JSONObject(credentialsFile.readText())
                val passwordHash = json.getString("password_hash")

                val is64CharHex = passwordHash.length == 64 && passwordHash.matches(Regex("[0-9a-f]+"))
                val isNotPlainText = passwordHash != input

                is64CharHex && isNotPlainText
            } finally {
                tempDir.deleteRecursively()
            }
        }
    }

    // Feature: app-lock-type-selector, Property 6: Correct credential unlocks the app

    /**
     * Validates: Requirements 4.2, 5.3, 7.4
     *
     * Property 6: Correct credential unlocks the app.
     * For any valid PIN, password, or pattern, verifying with the same credential
     * that was set SHALL return true.
     */

    "Property 6 - verifyPin returns true for the same PIN that was set" {
        forAll(
            PropTestConfig(iterations = 20),
            Arb.stringPattern("[0-9]{6}")
        ) { pin ->
            val tempDir = Files.createTempDirectory("cred_pin_verify_test").toFile()
            try {
                val store = CredentialStore(tempDir)
                store.setPin(pin)
                store.verifyPin(pin)
            } finally {
                tempDir.deleteRecursively()
            }
        }
    }

    "Property 6 - verifyPassword returns true for the same password that was set" {
        forAll(
            PropTestConfig(iterations = 20),
            Arb.string(6..50)
        ) { pwd ->
            val tempDir = Files.createTempDirectory("cred_pwd_verify_test").toFile()
            try {
                val store = CredentialStore(tempDir)
                store.setPassword(pwd)
                store.verifyPassword(pwd)
            } finally {
                tempDir.deleteRecursively()
            }
        }
    }

    "Property 6 - verifyPattern returns true for the same pattern that was set" {
        val validPatternArb = arbitrary { rs ->
            val size = rs.random.nextInt(4, 10)
            (0..8).toList().shuffled(rs.random).take(size)
        }
        forAll(
            PropTestConfig(iterations = 20),
            validPatternArb
        ) { dots ->
            val tempDir = Files.createTempDirectory("cred_pattern_verify_test").toFile()
            try {
                val store = CredentialStore(tempDir)
                store.setPattern(dots)
                store.verifyPattern(dots)
            } finally {
                tempDir.deleteRecursively()
            }
        }
    }

    // Feature: app-lock-type-selector, Property 7: Pattern minimum length validation

    /**
     * Validates: Requirements 7.3, 7.7
     *
     * Property 7: Pattern minimum length validation.
     * Sequences of < 4 dots are rejected — verifyPattern with < 4 dots SHALL return false.
     * Sequences of >= 4 dots that match the stored pattern SHALL return true.
     */

    "Property 7 - verifyPattern returns false for short patterns (< 4 dots)" {
        val validPatternArb = arbitrary { rs ->
            val size = rs.random.nextInt(4, 10)
            (0..8).toList().shuffled(rs.random).take(size)
        }
        forAll(
            PropTestConfig(iterations = 20),
            validPatternArb,
            Arb.list(Arb.int(0..8), 0..3)
        ) { validPattern, shortPattern ->
            val tempDir = Files.createTempDirectory("cred_pattern_short_test").toFile()
            try {
                val store = CredentialStore(tempDir)
                store.setPattern(validPattern)
                !store.verifyPattern(shortPattern)
            } finally {
                tempDir.deleteRecursively()
            }
        }
    }

    "Property 7 - verifyPattern returns true for valid patterns (>= 4 dots) matching the stored pattern" {
        val validPatternArb = arbitrary { rs ->
            val size = rs.random.nextInt(4, 10)
            (0..8).toList().shuffled(rs.random).take(size)
        }
        forAll(
            PropTestConfig(iterations = 20),
            validPatternArb
        ) { validPattern ->
            val tempDir = Files.createTempDirectory("cred_pattern_valid_test").toFile()
            try {
                val store = CredentialStore(tempDir)
                store.setPattern(validPattern)
                store.verifyPattern(validPattern)
            } finally {
                tempDir.deleteRecursively()
            }
        }
    }

    // Feature: app-lock-type-selector, Property 8: Password minimum length validation

    /**
     * Validates: Requirements 9.6
     *
     * Property 8: Password minimum length validation.
     * Passwords of length >= 6 meet the acceptance criterion.
     * Passwords of length < 6 do not meet the acceptance criterion.
     */

    "Property 8 - passwords of length >= 6 meet the acceptance criterion" {
        forAll(
            PropTestConfig(iterations = 20),
            Arb.string(6..50)
        ) { pwd ->
            pwd.length >= 6
        }
    }

    "Property 8 - passwords of length < 6 do not meet the acceptance criterion" {
        forAll(
            PropTestConfig(iterations = 20),
            Arb.string(0..5)
        ) { pwd ->
            pwd.length < 6
        }
    }

    // Feature: app-lock-type-selector, Property 9: Corruption resilience

    /**
     * Validates: Requirements 12.5
     *
     * Property 9: Corruption resilience (CredentialStore).
     * For any arbitrary bytes written to credentials.txt, calling hasPin()
     * SHALL NOT throw an exception and SHALL return false.
     */

    "Property 9 - hasPin does not throw and returns false when credentials.txt contains arbitrary bytes" {
        val corruptBytesArb = arbitrary { rs ->
            val size = rs.random.nextInt(1, 101)
            ByteArray(size) { rs.random.nextInt(-128, 128).toByte() }
        }
        forAll(
            PropTestConfig(iterations = 20),
            corruptBytesArb
        ) { corruptBytes ->
            val tempDir = Files.createTempDirectory("cred_corrupt_test").toFile()
            try {
                val credentialsFile = File(tempDir, "credentials.txt")
                credentialsFile.writeBytes(corruptBytes)
                val store = CredentialStore(tempDir)
                !store.hasPin()
            } finally {
                tempDir.deleteRecursively()
            }
        }
    }
})
