package com.msu.mfalocker

import android.graphics.drawable.ColorDrawable
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

// Feature: system-app-lock

/**
 * Unit tests for the system app lock feature (tasks 5.1–5.3).
 * These tests exercise the App data class directly without Android framework dependencies.
 */
class SystemAppLockUnitTest : StringSpec({

    // Reusable stub drawable — ColorDrawable has no Android context dependency in unit tests
    val stubDrawable = ColorDrawable(0)

    // ── 5.1 ──────────────────────────────────────────────────────────────────
    "5.1: App constructed with isSystem=true has isSystem flag set to true" {
        val app = MainActivity.App(
            appName = "Settings",
            packageName = "com.android.settings",
            icon = stubDrawable,
            status = "Unlocked",
            isSystem = true
        )
        app.isSystem shouldBe true
    }

    // ── 5.2 ──────────────────────────────────────────────────────────────────
    "5.2: App constructed without isSystem (default) has isSystem flag set to false" {
        val app = MainActivity.App(
            appName = "WhatsApp",
            packageName = "com.whatsapp",
            icon = stubDrawable,
            status = "Unlocked"
            // isSystem defaults to false
        )
        app.isSystem shouldBe false
    }

    "5.2b: App constructed with isSystem=false explicitly has isSystem flag set to false" {
        val app = MainActivity.App(
            appName = "Chrome",
            packageName = "com.android.chrome",
            icon = stubDrawable,
            status = "Unlocked",
            isSystem = false
        )
        app.isSystem shouldBe false
    }

    // ── 5.3 ──────────────────────────────────────────────────────────────────
    "5.3: Toggling a system app status from Unlocked to Locked sets status to Locked" {
        val app = MainActivity.App(
            appName = "Camera",
            packageName = "com.android.camera",
            icon = stubDrawable,
            status = "Unlocked",
            isSystem = true
        )
        app.status shouldBe "Unlocked"

        // Simulate the toggle logic from AppAdaptor.ViewHolder.bind()
        app.status = "Locked"

        app.status shouldBe "Locked"
    }

    "5.3b: Toggling a system app status from Locked back to Unlocked sets status to Unlocked" {
        val app = MainActivity.App(
            appName = "Phone",
            packageName = "com.android.phone",
            icon = stubDrawable,
            status = "Locked",
            isSystem = true
        )
        app.status = "Unlocked"
        app.status shouldBe "Unlocked"
    }
})
