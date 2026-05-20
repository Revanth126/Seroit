@file:Suppress("DEPRECATION")

package com.msu.mfalocker

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.AppOpsManager
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.msu.mfalocker.databinding.ActivityMainBinding
import com.msu.mfalocker.databinding.BottomSheetLockTypeBinding
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var id: ActivityMainBinding
    private lateinit var vibrator: Vibrator
    private lateinit var lockTypeStore: LockTypeStore
    private var appsList = ArrayList<App>()
    private lateinit var lockedAppsList: ArrayList<String>
    private lateinit var lockedFile: File
    private lateinit var ifLockedFile: File
    private var pausedFor = 0
    private var type = ""

    @SuppressLint("HardwareIds")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        id = ActivityMainBinding.inflate(layoutInflater)
        setContentView(id.root)

        lockTypeStore = LockTypeStore(filesDir)

        lockedFile = File(filesDir, "locked.txt")
        ifLockedFile = File(filesDir, "ifLocked.txt")
        lockedAppsList = if (lockedFile.exists() && lockedFile.readText().isNotEmpty())
            ArrayList(lockedFile.readText().split(","))
        else ArrayList()

        // Requirement 10.1: toolbar with settings icon
        id.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_settings) {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            } else false
        }

        if (ifLockedFile.exists() && ifLockedFile.readText() == "true") {
            if (!Settings.canDrawOverlays(this) || !isUsagePermitted()) {
                type = "none"
                id.materialSwitch.isChecked = false
                id.appsList.alpha = 0.5f
            } else {
                type = ""
                id.materialSwitch.isChecked = true
                id.appsList.alpha = 1f
            }
        } else {
            type = "none"
            id.materialSwitch.isChecked = false
            id.appsList.alpha = 0.5f
        }

        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        for (packageInfo in packages) {
            val pkgName = packageInfo.packageName
            if (packageName == pkgName) continue  // self-exclusion (Req 7.1)
            val isSystem = (packageInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            // For system apps, only include those with a launcher activity (Req 1.1, 1.2)
            if (isSystem && packageManager.getLaunchIntentForPackage(pkgName) == null) continue
            val appName = packageManager.getApplicationLabel(packageInfo).toString()
            val appIcon = packageManager.getApplicationIcon(packageInfo)
            appsList.add(App(appName, pkgName, appIcon, if (lockedAppsList.contains(pkgName)) "Locked" else "Unlocked", isSystem))
        }

        appsList.sortBy { it.appName }
        setupAdaptor()
        updateEmptyState()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(VibratorManager::class.java)
            vibrator = vibratorManager.defaultVibrator
        } else {
            vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        id.filterCard.setOnClickListener {
            if (type == "") {
                type = "filter"
                appsList.sortBy { it.status }
                id.filterCard.setCardBackgroundColor(getColor(R.color.grey_light))
            } else {
                type = ""
                appsList.sortBy { it.appName }
                id.filterCard.setCardBackgroundColor(getColor(R.color.transparent))
            }
            setupAdaptor()
        }

        id.materialSwitch.setOnCheckedChangeListener { _, b ->
            id.filterCard.setCardBackgroundColor(getColor(R.color.transparent))
            if (b) {
                if (!isUsagePermitted()) {
                    pausedFor = 1
                    type = "none"
                    ifLockedFile.writeText("false")
                    id.appsList.alpha = 0.5f
                    id.materialSwitch.isChecked = false
                    // Requirement 10.3: rationale dialog before redirecting
                    MaterialAlertDialogBuilder(this)
                        .setTitle("Usage Stats Permission Required")
                        .setMessage("SEROIT needs access to usage stats to detect when a locked app is opened. Please grant this permission to enable the locker service.")
                        .setPositiveButton("Open Settings") { _, _ ->
                            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:$packageName")))
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                } else if (!Settings.canDrawOverlays(this)) {
                    pausedFor = 2
                    type = "none"
                    ifLockedFile.writeText("false")
                    id.appsList.alpha = 0.5f
                    id.materialSwitch.isChecked = false
                    // Requirement 10.4: rationale dialog before redirecting
                    MaterialAlertDialogBuilder(this)
                        .setTitle("Overlay Permission Required")
                        .setMessage("SEROIT needs permission to display over other apps so it can show the lock screen when a locked app is opened. Please grant this permission to enable the locker service.")
                        .setPositiveButton("Open Settings") { _, _ ->
                            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                } else {
                    type = ""
                    ifLockedFile.writeText("true")
                    id.appsList.alpha = 1f
                }
            } else {
                type = "none"
                ifLockedFile.writeText("false")
                id.appsList.alpha = 0.5f
            }
            setupAdaptor()
        }

        id.appsList.setOnScrollChangeListener { _, _, _, _, _ ->
            if (!id.appsList.canScrollVertically(1)) {
                id.viewBottom.visibility = View.GONE
                vibrate()
            } else if (!id.appsList.canScrollVertically(-1)) {
                id.viewTop.visibility = View.GONE
                vibrate()
            } else {
                id.viewTop.visibility = View.VISIBLE
                id.viewBottom.visibility = View.VISIBLE
            }
        }

        id.search.addTextChangedListener { query ->
            if (query.toString().trim().isEmpty()) {
                type = ""
                id.filterCard.setCardBackgroundColor(getColor(R.color.transparent))
                setupAdaptor()
            } else {
                type = "filter"
                id.filterCard.setCardBackgroundColor(getColor(R.color.grey_light))
                val filteredList = ArrayList<App>()
                for (app in appsList) {
                    if (app.appName.lowercase().contains(query.toString().lowercase())) filteredList.add(app)
                    else if (app.packageName.lowercase().contains(query.toString().lowercase())) filteredList.add(app)
                }
                val adaptor = AppAdaptor(this, filteredList, type, lockedAppsList, lockTypeStore)
                adaptor.onSettingsPress = { pkg -> showLockTypeSheet(pkg) }
                id.appsList.adapter = adaptor
            }
        }
    }

    private fun setupAdaptor() {
        val adaptor = AppAdaptor(this, appsList, type, lockedAppsList, lockTypeStore)
        adaptor.onSettingsPress = { pkg -> showLockTypeSheet(pkg) }
        id.appsList.adapter = adaptor
        if (id.appsList.layoutManager == null)
            id.appsList.layoutManager = LinearLayoutManager(this)
        updateEmptyState()
    }

    // Requirement 10.2: show/hide empty state
    private fun updateEmptyState() {
        id.emptyState.visibility = if (lockedAppsList.isEmpty()) View.VISIBLE else View.GONE
    }

    // Requirements 2.1–2.6: bottom sheet for per-app lock type
    private fun showLockTypeSheet(packageName: String) {
        LockTypeBottomSheet.newInstance(packageName, lockTypeStore).show(supportFragmentManager, "lock_type_sheet")
    }

    private fun vibrate() {
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    data class App(
        val appName: String,
        val packageName: String,
        val icon: Drawable,
        var status: String,
        val isSystem: Boolean = false
    )

    override fun onPause() {
        super.onPause()
        if (isServiceRunning()) stopService(Intent(this, ListenerService::class.java))
        if (lockedAppsList.isEmpty()) lockedFile.writeText("")
        else lockedFile.writeText(lockedAppsList.joinToString(","))
        startService(Intent(this, ListenerService::class.java))
    }

    private fun isServiceRunning(): Boolean {
        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val runningServices = activityManager.getRunningServices(Integer.MAX_VALUE)
        val serviceName = ListenerService::class.java.name
        for (serviceInfo in runningServices) {
            if (serviceInfo.service.className == serviceName) return true
        }
        return false
    }

    private fun isUsagePermitted(): Boolean {
        return try {
            val appOps = getSystemService(APP_OPS_SERVICE) as AppOpsManager
            val mode = appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun onResume() {
        super.onResume()
        if (pausedFor == 1) {
            pausedFor = 0
            if (isUsagePermitted()) {
                if (!Settings.canDrawOverlays(this)) {
                    pausedFor = 2
                    MaterialAlertDialogBuilder(this)
                        .setTitle("Overlay Permission Required")
                        .setMessage("SEROIT needs permission to display over other apps so it can show the lock screen when a locked app is opened. Please grant this permission to enable the locker service.")
                        .setPositiveButton("Open Settings") { _, _ ->
                            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                } else {
                    type = ""
                    ifLockedFile.writeText("true")
                    id.materialSwitch.isChecked = true
                    id.appsList.alpha = 1f
                    setupAdaptor()
                }
            }
        } else if (pausedFor == 2) {
            pausedFor = 0
            if (Settings.canDrawOverlays(this)) {
                type = ""
                ifLockedFile.writeText("true")
                id.materialSwitch.isChecked = true
                id.appsList.alpha = 1f
                setupAdaptor()
            }
        }
    }

    // ─── Lock Type Bottom Sheet ───────────────────────────────────────────────

    class LockTypeBottomSheet : BottomSheetDialogFragment() {

        companion object {
            private const val ARG_PKG = "packageName"
            private const val REQUEST_QUIZ_CONFIG = 1001

            fun newInstance(packageName: String, lockTypeStore: LockTypeStore): LockTypeBottomSheet {
                val sheet = LockTypeBottomSheet()
                sheet.arguments = Bundle().apply { putString(ARG_PKG, packageName) }
                sheet.lockTypeStore = lockTypeStore
                return sheet
            }
        }

        var lockTypeStore: LockTypeStore? = null

        override fun onCreateView(
            inflater: android.view.LayoutInflater,
            container: android.view.ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            val binding = BottomSheetLockTypeBinding.inflate(inflater, container, false)
            val packageName = arguments?.getString(ARG_PKG) ?: return binding.root
            val store = lockTypeStore ?: return binding.root

            // Show app name
            val appName = try {
                requireContext().packageManager.getApplicationLabel(
                    requireContext().packageManager.getApplicationInfo(packageName, 0)
                ).toString()
            } catch (e: Exception) { packageName }
            binding.sheetAppName.text = appName

            // Pre-select current effective type (Req 2.4, 2.5)
            val perApp = store.getPerAppLockType(packageName)
            val radioId = if (perApp == null) {
                R.id.sheetRadioDefault
            } else {
                when (perApp) {
                    LockType.PIN               -> R.id.sheetRadioPin
                    LockType.PASSWORD          -> R.id.sheetRadioPassword
                    LockType.BIOMETRIC         -> R.id.sheetRadioBiometric
                    LockType.PATTERN           -> R.id.sheetRadioPattern
                    LockType.DEVICE_CREDENTIAL -> R.id.sheetRadioDeviceCredential
                    LockType.QUIZ              -> R.id.sheetRadioQuiz
                }
            }
            binding.sheetLockTypeGroup.check(radioId)

            binding.sheetBtnApply.setOnClickListener {
                when (val checkedId = binding.sheetLockTypeGroup.checkedRadioButtonId) {
                    R.id.sheetRadioDefault -> {
                        // Requirement 2.3: remove per-app override
                        // Requirement 8.2: clean up QuizConfig if previous type was QUIZ
                        if (store.getPerAppLockType(packageName) == LockType.QUIZ) {
                            QuizConfigStore(requireContext().filesDir).removeQuizConfig(packageName)
                        }
                        store.removePerAppLockType(packageName)
                        dismiss()
                    }
                    R.id.sheetRadioBiometric -> {
                        // Requirement 2.6: validate biometric availability
                        val bm = BiometricManager.from(requireContext())
                        if (bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                            != BiometricManager.BIOMETRIC_SUCCESS
                        ) {
                            MaterialAlertDialogBuilder(requireContext())
                                .setTitle("Biometrics Unavailable")
                                .setMessage("No biometrics enrolled. Please enroll a fingerprint in device settings.")
                                .setPositiveButton("OK", null)
                                .show()
                        } else {
                            store.setPerAppLockType(packageName, LockType.BIOMETRIC)
                            dismiss()
                        }
                    }
                    R.id.sheetRadioQuiz -> {
                        // Requirements 2.1, 2.5, 1.3: launch QuizConfigActivity for result
                        val intent = Intent(requireContext(), QuizConfigActivity::class.java)
                            .putExtra("packageName", packageName)
                        startActivityForResult(intent, REQUEST_QUIZ_CONFIG)
                        // Do NOT dismiss here — wait for onActivityResult
                    }
                    else -> {
                        val lockType = when (checkedId) {
                            R.id.sheetRadioPin               -> LockType.PIN
                            R.id.sheetRadioPassword          -> LockType.PASSWORD
                            R.id.sheetRadioPattern           -> LockType.PATTERN
                            R.id.sheetRadioDeviceCredential  -> LockType.DEVICE_CREDENTIAL
                            else                             -> return@setOnClickListener
                        }
                        // Requirement 2.2: persist per-app lock type
                        store.setPerAppLockType(packageName, lockType)
                        dismiss()
                    }
                }
            }

            return binding.root
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)
            if (requestCode == REQUEST_QUIZ_CONFIG && resultCode == android.app.Activity.RESULT_OK) {
                dismiss()
            }
            // RESULT_CANCELED: do nothing, keep sheet open
        }
    }
}
