package com.msu.mfalocker

import io.kotest.core.spec.style.StringSpec
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.orNull
import io.kotest.property.arbitrary.string
import io.kotest.property.forAll
import java.io.File
import java.nio.file.Files

// Feature: app-lock-type-selector, Property 1: GlobalLockType round-trip
// Validates: Requirements 1.2, 12.1
class LockTypeStoreTest : StringSpec({

    "Property 1: GlobalLockType round-trip - setGlobalLockType then getGlobalLockType returns same type" {
        forAll(
            PropTestConfig(iterations = 20),
            Arb.of(*LockType.values())
        ) { type ->
            val tempDir = Files.createTempDirectory("lock_type_store_test").toFile()
            try {
                val store = LockTypeStore(tempDir)
                store.setGlobalLockType(type)
                store.getGlobalLockType() == type
            } finally {
                tempDir.deleteRecursively()
            }
        }
    }

    // Feature: app-lock-type-selector, Property 2: PerAppLockType round-trip
    // Validates: Requirements 2.2, 12.2
    "Property 2: PerAppLockType round-trip - setPerAppLockType then getPerAppLockType returns same type" {
        forAll(
            PropTestConfig(iterations = 20),
            Arb.string(),
            Arb.of(*LockType.values())
        ) { pkg, type ->
            val tempDir = Files.createTempDirectory("lock_type_store_per_app_test").toFile()
            try {
                val store = LockTypeStore(tempDir)
                store.setPerAppLockType(pkg, type)
                store.getPerAppLockType(pkg) == type
            } finally {
                tempDir.deleteRecursively()
            }
        }
    }

    // Feature: app-lock-type-selector, Property 3: Remove per-app override reverts to null
    // Validates: Requirements 2.3
    "Property 3: Remove per-app override reverts to null - setPerAppLockType then removePerAppLockType returns null" {
        forAll(
            PropTestConfig(iterations = 20),
            Arb.string(),
            Arb.of(*LockType.values())
        ) { pkg, type ->
            val tempDir = Files.createTempDirectory("lock_type_store_remove_test").toFile()
            try {
                val store = LockTypeStore(tempDir)
                store.setPerAppLockType(pkg, type)
                store.removePerAppLockType(pkg)
                store.getPerAppLockType(pkg) == null
            } finally {
                tempDir.deleteRecursively()
            }
        }
    }

    // Feature: app-lock-type-selector, Property 4: Lock type resolution priority
    // Validates: Requirements 3.1
    "Property 4: Lock type resolution priority - perAppType takes precedence over globalType when set" {
        forAll(
            PropTestConfig(iterations = 20),
            Arb.string(),
            Arb.of(*LockType.values()),
            Arb.of(*LockType.values()).orNull()
        ) { pkg, globalType, perAppType ->
            val tempDir = Files.createTempDirectory("lock_type_store_priority_test").toFile()
            try {
                val store = LockTypeStore(tempDir)
                store.setGlobalLockType(globalType)
                if (perAppType != null) {
                    store.setPerAppLockType(pkg, perAppType)
                    store.resolveEffectiveLockType(pkg) == perAppType
                } else {
                    store.resolveEffectiveLockType(pkg) == globalType
                }
            } finally {
                tempDir.deleteRecursively()
            }
        }
    }

    // Feature: app-lock-type-selector, Property 9: Corruption resilience
    // Validates: Requirements 12.5
    "Property 9: Corruption resilience - arbitrary bytes in store files do not throw and return safe defaults" {
        // **Validates: Requirements 12.5**
        val corruptBytesArb = arbitrary { rs ->
            val size = rs.random.nextInt(1, 101)
            ByteArray(size) { rs.random.nextInt(-128, 128).toByte() }
        }
        forAll(
            PropTestConfig(iterations = 20),
            corruptBytesArb
        ) { corruptBytes ->
            val tempDir = Files.createTempDirectory("lock_type_store_corruption_test").toFile()
            try {
                File(tempDir, "lock_type.txt").writeBytes(corruptBytes)
                File(tempDir, "lock_type_per_app.txt").writeBytes(corruptBytes)
                val store = LockTypeStore(tempDir)
                val globalResult = store.getGlobalLockType()
                val perAppResult = store.getPerAppLockType("any.package")
                globalResult == LockType.PIN && perAppResult == null
            } finally {
                tempDir.deleteRecursively()
            }
        }
    }
})

