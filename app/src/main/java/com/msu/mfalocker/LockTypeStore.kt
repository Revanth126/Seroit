package com.msu.mfalocker

import android.util.Log
import org.json.JSONObject
import java.io.File

class LockTypeStore(private val filesDir: File) {

    companion object {
        private const val TAG = "LockTypeStore"
        private const val GLOBAL_FILE = "lock_type.txt"
        private const val PER_APP_FILE = "lock_type_per_app.txt"
    }

    /** Returns the global default. Falls back to PIN if file absent or unreadable. */
    fun getGlobalLockType(): LockType {
        val file = File(filesDir, GLOBAL_FILE)
        if (!file.exists()) return LockType.PIN
        return try {
            val raw = file.readText().trim()
            LockType.valueOf(raw)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read or parse $GLOBAL_FILE, deleting and returning PIN", e)
            file.delete()
            LockType.PIN
        }
    }

    /** Persists the global lock type to lock_type.txt. */
    fun setGlobalLockType(type: LockType) {
        try {
            File(filesDir, GLOBAL_FILE).writeText(type.name)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write $GLOBAL_FILE", e)
        }
    }

    /** Returns the per-app override, or null if none is set. */
    fun getPerAppLockType(packageName: String): LockType? {
        val map = readPerAppMap() ?: return null
        val value = if (map.has(packageName)) map.getString(packageName) else return null
        return try {
            LockType.valueOf(value)
        } catch (e: Exception) {
            Log.e(TAG, "Unrecognised LockType '$value' for $packageName", e)
            null
        }
    }

    /** Persists a per-app override. */
    fun setPerAppLockType(packageName: String, type: LockType) {
        val map = readPerAppMap() ?: JSONObject()
        try {
            map.put(packageName, type.name)
            File(filesDir, PER_APP_FILE).writeText(map.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write $PER_APP_FILE", e)
        }
    }

    /** Removes the per-app override (reverts to global). */
    fun removePerAppLockType(packageName: String) {
        val map = readPerAppMap() ?: return
        try {
            map.remove(packageName)
            File(filesDir, PER_APP_FILE).writeText(map.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write $PER_APP_FILE after removal", e)
        }
    }

    /**
     * Resolves the effective lock type for a package:
     * PerAppLockType if present, else GlobalLockType.
     */
    fun resolveEffectiveLockType(packageName: String): LockType {
        return getPerAppLockType(packageName) ?: getGlobalLockType()
    }

    // --- private helpers ---

    private fun readPerAppMap(): JSONObject? {
        val file = File(filesDir, PER_APP_FILE)
        if (!file.exists()) return JSONObject()
        return try {
            JSONObject(file.readText())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse $PER_APP_FILE, deleting and returning empty map", e)
            file.delete()
            null
        }
    }
}
