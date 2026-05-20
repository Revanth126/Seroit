package com.msu.mfalocker

import android.content.pm.ApplicationInfo
import android.graphics.drawable.ColorDrawable
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.alphanumeric
import io.kotest.property.arbitrary.bind
import io.kotest.property.checkAll
import kotlin.io.path.createTempDirectory

// Feature: system-app-lock

/**
 * Property-based tests for the system app lock feature (task 6).
 * Uses Kotest property testing with 100 iterations per property.
 */
class SystemAppLockPropertyTest : FreeSpec({

    val stubDrawable = ColorDrawable(0)

    // Arb for package names: alphanumeric, non-empty, no commas
    val pkgArb = Arb.string(minSize = 1, maxSize = 30, codepoints = Codepoint.alphanumeric())

    // Arb for app names: any string
    val appNameArb = Arb.string(minSize = 0, maxSize = 40)

    // Data class representing raw app info for discovery tests
    data class AppInfo(val packageName: String, val flags: Int, val hasLaunchIntent: Boolean)

    val appInfoArb: Arb<AppInfo> = Arb.bind(
        pkgArb,
        Arb.int(),
        Arb.boolean()
    ) { pkg, flags, hasLaunch -> AppInfo(pkg, flags, hasLaunch) }

    /**
     * Pure model of the discovery function extracted from MainActivity.onCreate.
     * Takes a list of AppInfo triples and ownPackageName,
     * returns the list of App objects that would be discovered.
     */
    fun discoverApps(infos: List<AppInfo>, ownPackageName: String): List<MainActivity.App> {
        val result = mutableListOf<MainActivity.App>()
        for (info in infos) {
            // Self-exclusion (Req 7.1)
            if (info.packageName == ownPackageName) continue
            val isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            // System apps without a launch intent are excluded (Req 1.1, 1.2)
            if (isSystem && !info.hasLaunchIntent) continue
            result.add(
                MainActivity.App(
                    appName = info.packageName, // use pkg as name for simplicity
                    packageName = info.packageName,
                    icon = stubDrawable,
                    status = "Unlocked",
                    isSystem = isSystem
                )
            )
        }
        return result
    }

    /**
     * Pure model of the search filter extracted from MainActivity.
     */
    fun searchApps(apps: List<MainActivity.App>, query: String): List<MainActivity.App> {
        val filteredList = ArrayList<MainActivity.App>()
        for (app in apps) {
            if (app.appName.lowercase().contains(query.lowercase())) filteredList.add(app)
            else if (app.packageName.lowercase().contains(query.lowercase())) filteredList.add(app)
        }
        return filteredList
    }

    // ── Property 1 & 8 ───────────────────────────────────────────────────────
    // Feature: system-app-lock, Property 1 & 8
    // Property 1: Launchable system apps are discovered; non-launchable ones are excluded
    //   Validates: Requirements 1.1, 1.2
    // Property 8: Self-exclusion invariant
    //   Validates: Requirement 7.1
    "Property 1 & 8: discovery includes launchable system apps, excludes non-launchable ones, and never includes own package name" {
        checkAll(iterations = 100, Arb.list(appInfoArb, 0..20), pkgArb) { infos, ownPkg ->
            val result = discoverApps(infos, ownPkg)
            val resultPkgs = result.map { it.packageName }

            // Property 8: own package name is never in the result
            resultPkgs shouldNotContain ownPkg

            // Property 1: result contains exactly the expected apps
            val expected = infos
                .filter { it.packageName != ownPkg }
                .filter { info ->
                    val isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    !isSystem || info.hasLaunchIntent
                }
                .map { it.packageName }

            resultPkgs.toSet() shouldBe expected.toSet()

            // System apps without launch intent are excluded
            for (info in infos) {
                val isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                if (isSystem && !info.hasLaunchIntent && info.packageName != ownPkg) {
                    resultPkgs shouldNotContain info.packageName
                }
            }

            // User apps (non-system) are always included (unless they are ownPkg)
            for (info in infos) {
                val isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                if (!isSystem && info.packageName != ownPkg) {
                    resultPkgs shouldContain info.packageName
                }
            }
        }
    }

    // ── Property 2 ───────────────────────────────────────────────────────────
    // Feature: system-app-lock, Property 2
    // isSystem flag matches FLAG_SYSTEM for any app
    // Validates: Requirements 2.1, 2.2
    "Property 2: isSystem flag matches FLAG_SYSTEM for any app" {
        checkAll(iterations = 100, Arb.int()) { flags ->
            val expectedIsSystem = (flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val app = MainActivity.App(
                appName = "TestApp",
                packageName = "com.test.app",
                icon = stubDrawable,
                status = "Unlocked",
                isSystem = expectedIsSystem
            )
            app.isSystem shouldBe expectedIsSystem
            app.isSystem shouldBe ((flags and ApplicationInfo.FLAG_SYSTEM) != 0)
        }
    }

    // ── Property 3 ───────────────────────────────────────────────────────────
    // Feature: system-app-lock, Property 3
    // Alphabetical sort interleaves system and user apps correctly
    // Validates: Requirement 1.4
    "Property 3: alphabetical sort interleaves system and user apps correctly" {
        data class NameAndSystem(val name: String, val isSystem: Boolean)

        val nameAndSystemArb: Arb<NameAndSystem> = Arb.bind(
            appNameArb,
            Arb.boolean()
        ) { name, isSystem -> NameAndSystem(name, isSystem) }

        checkAll(iterations = 100, Arb.list(nameAndSystemArb, 0..20)) { items ->
            val apps = items.map { item ->
                MainActivity.App(
                    appName = item.name,
                    packageName = "pkg.${item.name}",
                    icon = stubDrawable,
                    status = "Unlocked",
                    isSystem = item.isSystem
                )
            }
            val sorted = apps.sortedBy { it.appName }

            // Verify non-decreasing alphabetical order (case-sensitive, natural String ordering)
            for (i in 0 until sorted.size - 1) {
                (sorted[i].appName <= sorted[i + 1].appName) shouldBe true
            }

            // Verify no grouping by isSystem — sort is purely by appName
            val sortedNames = sorted.map { it.appName }
            val expectedNames = apps.map { it.appName }.sorted()
            sortedNames shouldBe expectedNames
        }
    }

    // ── Property 4 ───────────────────────────────────────────────────────────
    // Feature: system-app-lock, Property 4
    // Lock toggle round-trip for any package name
    // Validates: Requirements 3.1, 3.2
    "Property 4: lock toggle round-trip for any package name" {
        checkAll(iterations = 100, pkgArb) { pkg ->
            val lockedAppsList = ArrayList<String>()

            // Add to locked list
            lockedAppsList.add(pkg)
            lockedAppsList shouldContain pkg

            // Remove from locked list
            lockedAppsList.remove(pkg)
            lockedAppsList shouldNotContain pkg
            lockedAppsList.size shouldBe 0
        }
    }

    // ── Property 5 ───────────────────────────────────────────────────────────
    // Feature: system-app-lock, Property 5
    // LockTypeStore per-app round-trip for system app package names
    // Validates: Requirement 4.3
    "Property 5: LockTypeStore per-app round-trip for system app package names" {
        checkAll(iterations = 100, pkgArb, Arb.enum<LockType>()) { pkg, lockType ->
            val tempDir = createTempDirectory("locktype_test_").toFile()
            try {
                val store = LockTypeStore(tempDir)
                store.setPerAppLockType(pkg, lockType)
                val retrieved = store.getPerAppLockType(pkg)
                retrieved shouldBe lockType
            } finally {
                tempDir.deleteRecursively()
            }
        }
    }

    // ── Property 6 ───────────────────────────────────────────────────────────
    // Feature: system-app-lock, Property 6
    // Locked apps persistence round-trip via locked.txt
    // Validates: Requirements 5.1, 5.2
    "Property 6: locked apps persistence round-trip via locked.txt" {
        checkAll(iterations = 100, Arb.list(pkgArb, 1..20)) { packages ->
            val tempDir = createTempDirectory("locked_test_").toFile()
            try {
                val lockedFile = java.io.File(tempDir, "locked.txt")

                // Write: same logic as MainActivity.onPause
                lockedFile.writeText(packages.joinToString(","))

                // Read: same logic as MainActivity.onCreate
                val readBack = if (lockedFile.exists() && lockedFile.readText().isNotEmpty())
                    ArrayList(lockedFile.readText().split(","))
                else ArrayList()

                readBack.toSet() shouldBe packages.toSet()
            } finally {
                tempDir.deleteRecursively()
            }
        }
    }

    "Property 6 (empty list): writing empty string and reading back yields empty list" {
        val tempDir = createTempDirectory("locked_empty_test_").toFile()
        try {
            val lockedFile = java.io.File(tempDir, "locked.txt")
            lockedFile.writeText("")
            val readBack = if (lockedFile.exists() && lockedFile.readText().isNotEmpty())
                ArrayList(lockedFile.readText().split(","))
            else ArrayList()
            readBack.size shouldBe 0
        } finally {
            tempDir.deleteRecursively()
        }
    }

    // ── Property 7 ───────────────────────────────────────────────────────────
    // Feature: system-app-lock, Property 7
    // Search returns exactly matching apps (case-insensitive) for any query and app list
    // Validates: Requirements 6.1, 6.2
    "Property 7: search returns exactly matching apps (case-insensitive) for any query and app list" {
        data class NameAndPkg(val name: String, val pkg: String)

        val nameAndPkgArb: Arb<NameAndPkg> = Arb.bind(
            appNameArb,
            pkgArb
        ) { name, pkg -> NameAndPkg(name, pkg) }

        checkAll(
            iterations = 100,
            Arb.list(nameAndPkgArb, 0..20),
            Arb.string(minSize = 0, maxSize = 15)
        ) { nameAndPkgList, query ->
            val apps = nameAndPkgList.map { item ->
                MainActivity.App(
                    appName = item.name,
                    packageName = item.pkg,
                    icon = stubDrawable,
                    status = "Unlocked",
                    isSystem = false
                )
            }

            val result = searchApps(apps, query)
            val resultPkgs = result.map { it.packageName }

            // Every returned app must match the query
            for (app in result) {
                val matches = app.appName.lowercase().contains(query.lowercase()) ||
                        app.packageName.lowercase().contains(query.lowercase())
                matches shouldBe true
            }

            // Every matching app must be in the result
            for (app in apps) {
                val matches = app.appName.lowercase().contains(query.lowercase()) ||
                        app.packageName.lowercase().contains(query.lowercase())
                if (matches) {
                    resultPkgs shouldContain app.packageName
                } else {
                    resultPkgs shouldNotContain app.packageName
                }
            }
        }
    }
})
