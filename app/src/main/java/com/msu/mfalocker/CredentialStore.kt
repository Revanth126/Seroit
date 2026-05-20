package com.msu.mfalocker

import android.util.Log
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

class CredentialStore(private val filesDir: File) {

    companion object {
        private const val TAG = "CredentialStore"
        private const val CREDENTIALS_FILE = "credentials.txt"
        private const val PASSCODE_FILE = "passcode.txt"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PASSWORD_HASH = "password_hash"
        private const val KEY_PATTERN_HASH = "pattern_hash"
    }

    init {
        migratePasscode()
    }

    private fun migratePasscode() {
        val passcodeFile = File(filesDir, PASSCODE_FILE)
        if (!passcodeFile.exists()) return
        try {
            val plainText = passcodeFile.readText().trim()
            if (plainText.isNotEmpty()) {
                val json = readJson() ?: JSONObject()
                json.put(KEY_PIN_HASH, sha256(plainText))
                writeJson(json)
            }
            passcodeFile.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to migrate passcode.txt", e)
        }
    }

    private fun credentialsFile(): File = File(filesDir, CREDENTIALS_FILE)

    private fun readJson(): JSONObject? {
        val file = credentialsFile()
        if (!file.exists()) return null
        return try {
            JSONObject(file.readText())
        } catch (e: Exception) {
            Log.e(TAG, "Invalid JSON in $CREDENTIALS_FILE, deleting", e)
            try { file.delete() } catch (ex: Exception) { Log.e(TAG, "Failed to delete $CREDENTIALS_FILE", ex) }
            null
        }
    }

    private fun writeJson(json: JSONObject) {
        try {
            credentialsFile().writeText(json.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write $CREDENTIALS_FILE", e)
        }
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // --- PIN ---

    fun hasPin(): Boolean = readJson()?.optString(KEY_PIN_HASH, "")?.isNotEmpty() == true

    fun setPin(pin: String) {
        val json = readJson() ?: JSONObject()
        json.put(KEY_PIN_HASH, sha256(pin))
        writeJson(json)
    }

    fun verifyPin(pin: String): Boolean {
        val stored = readJson()?.optString(KEY_PIN_HASH, "") ?: return false
        return stored.isNotEmpty() && stored == sha256(pin)
    }

    // --- Password ---

    fun hasPassword(): Boolean = readJson()?.optString(KEY_PASSWORD_HASH, "")?.isNotEmpty() == true

    fun setPassword(password: String) {
        val json = readJson() ?: JSONObject()
        json.put(KEY_PASSWORD_HASH, sha256(password))
        writeJson(json)
    }

    fun verifyPassword(password: String): Boolean {
        val stored = readJson()?.optString(KEY_PASSWORD_HASH, "") ?: return false
        return stored.isNotEmpty() && stored == sha256(password)
    }

    // --- Pattern ---

    fun hasPattern(): Boolean = readJson()?.optString(KEY_PATTERN_HASH, "")?.isNotEmpty() == true

    fun setPattern(dotSequence: List<Int>) {
        val json = readJson() ?: JSONObject()
        json.put(KEY_PATTERN_HASH, sha256(dotSequence.joinToString(",")))
        writeJson(json)
    }

    fun verifyPattern(dotSequence: List<Int>): Boolean {
        if (dotSequence.size < 4) return false
        val stored = readJson()?.optString(KEY_PATTERN_HASH, "") ?: return false
        return stored.isNotEmpty() && stored == sha256(dotSequence.joinToString(","))
    }
}
