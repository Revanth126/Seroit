@file:Suppress("DEPRECATION")

package com.msu.mfalocker

import android.app.KeyguardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.msu.mfalocker.databinding.ActivityLockBinding
import java.io.File

class LockActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PACKAGE_NAME = "packageName"
        const val EXTRA_LOCK_TYPE = "lockType"
        private const val REQUEST_DEVICE_CREDENTIAL = 100
    }

    private lateinit var binding: ActivityLockBinding
    private lateinit var vibrator: Vibrator
    private lateinit var credentialStore: CredentialStore

    private var packageNameExtra: String = ""
    private var appName: String = ""
    private var lockType: LockType = LockType.PIN

    // PIN state
    private var pinDigits = ""
    private var passwordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Requirement 11.5: prevent screenshots/screen recording
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        binding = ActivityLockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        credentialStore = CredentialStore(filesDir)

        // Override back press to go home (Requirement 11.3)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                goHome()
            }
        })

        // Read extras
        packageNameExtra = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
        val lockTypeRaw = intent.getStringExtra(EXTRA_LOCK_TYPE)
        lockType = try {
            if (lockTypeRaw != null) LockType.valueOf(lockTypeRaw) else LockType.PIN
        } catch (e: IllegalArgumentException) {
            // Requirement 3.4: fall back to PIN on unrecognised LockType
            LockType.PIN
        }

        // Resolve app info (Requirement 11.1)
        val appInfo = resolveAppInfo(packageNameExtra)
        if (appInfo == null) {
            Toast.makeText(this, "App not found.", Toast.LENGTH_SHORT).show()
            goHome()
            return
        }

        binding.appIcon.setImageDrawable(packageManager.getApplicationIcon(appInfo))
        appName = packageManager.getApplicationLabel(appInfo).toString()
        binding.appName.text = appName

        // Show the correct UI section (Requirement 3.3)
        showLockTypeUI(lockType)
    }

    private fun resolveAppInfo(pkg: String): android.content.pm.ApplicationInfo? {
        if (pkg.isEmpty()) return null
        return try {
            val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            packages.firstOrNull { it.packageName == pkg }
        } catch (e: Exception) {
            null
        }
    }

    private fun showLockTypeUI(type: LockType) {
        // Hide all sections first
        binding.pinSection.visibility = View.GONE
        binding.passwordSection.visibility = View.GONE
        binding.patternSection.visibility = View.GONE

        when (type) {
            LockType.PIN -> setupPinUI()
            LockType.PASSWORD -> setupPasswordUI()
            LockType.BIOMETRIC -> setupBiometricUI()
            LockType.PATTERN -> setupPatternUI()
            LockType.DEVICE_CREDENTIAL -> setupDeviceCredentialUI()
            LockType.QUIZ -> {
                val intent = Intent(this, QuizActivity::class.java).apply {
                    putExtra(QuizActivity.EXTRA_PACKAGE_NAME, packageNameExtra)
                }
                startActivity(intent)
                finish()
            }
        }
    }

    // ─── PIN (Requirement 4.x) ────────────────────────────────────────────────

    private fun setupPinUI() {
        // Requirement 4.4: redirect to SettingsActivity if no PIN stored
        if (!credentialStore.hasPin()) {
            redirectToSettings()
            return
        }

        binding.lockTypeSubtitle.text = "Enter PIN"
        binding.pinSection.visibility = View.VISIBLE
        pinDigits = ""

        val digitButtons = listOf(
            binding.one to "1", binding.two to "2", binding.three to "3",
            binding.four to "4", binding.five to "5", binding.six to "6",
            binding.seven to "7", binding.eight to "8", binding.nine to "9",
            binding.zero to "0"
        )

        for ((btn, digit) in digitButtons) {
            btn.setOnClickListener {
                if (pinDigits.length < 6) {
                    pinDigits += digit
                    updatePinDots(pinDigits.length)
                    if (pinDigits.length == 6) verifyPin()
                }
            }
        }

        binding.backspace.setOnClickListener {
            if (pinDigits.isNotEmpty()) {
                pinDigits = pinDigits.dropLast(1)
                updatePinDots(pinDigits.length)
            }
        }

        binding.done.setOnClickListener {
            if (pinDigits.length == 6) verifyPin()
        }
    }

    private fun updatePinDots(length: Int) {
        val dots = listOf(binding.pass1, binding.pass2, binding.pass3,
            binding.pass4, binding.pass5, binding.pass6)
        dots.forEachIndexed { index, dot ->
            dot.visibility = if (index < length) View.VISIBLE else View.GONE
        }
    }

    private fun clearPinDots() {
        pinDigits = ""
        updatePinDots(0)
    }

    private fun verifyPin() {
        if (credentialStore.verifyPin(pinDigits)) {
            onAuthSuccess()
        } else {
            clearPinDots()
            vibrate50ms()
            showError("Wrong PIN.")
        }
    }

    // ─── Password (Requirement 5.x) ───────────────────────────────────────────

    private fun setupPasswordUI() {
        // Requirement 5.5: redirect to SettingsActivity if no password stored
        if (!credentialStore.hasPassword()) {
            redirectToSettings()
            return
        }

        binding.lockTypeSubtitle.text = "Enter Password"
        binding.passwordSection.visibility = View.VISIBLE

        // Requirement 5.2: show/hide toggle
        binding.passwordToggle.setOnClickListener {
            passwordVisible = !passwordVisible
            if (passwordVisible) {
                binding.passwordInput.transformationMethod = HideReturnsTransformationMethod.getInstance()
                binding.passwordToggle.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            } else {
                binding.passwordInput.transformationMethod = PasswordTransformationMethod.getInstance()
                binding.passwordToggle.setImageResource(android.R.drawable.ic_menu_view)
            }
            binding.passwordInput.setSelection(binding.passwordInput.text?.length ?: 0)
        }

        binding.passwordConfirm.setOnClickListener {
            val entered = binding.passwordInput.text?.toString() ?: ""
            if (credentialStore.verifyPassword(entered)) {
                onAuthSuccess()
            } else {
                binding.passwordInput.setText("")
                vibrate50ms()
                showError("Wrong password.")
            }
        }
    }

    // ─── Biometric (Requirement 6.x) ──────────────────────────────────────────

    private fun setupBiometricUI() {
        val biometricManager = BiometricManager.from(this)
        val canAuth = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        )

        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            // Requirement 6.5: fall back to PIN UI
            showError("Biometrics unavailable. Using PIN instead.")
            lockType = LockType.PIN
            setupPinUI()
            return
        }

        binding.lockTypeSubtitle.text = "Use Biometrics"

        val executor = ContextCompat.getMainExecutor(this)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onAuthSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                when (errorCode) {
                    BiometricPrompt.ERROR_LOCKOUT,
                    BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                        // Requirement 6.4
                        showError("Too many failed attempts. Try again later.")
                        goHome()
                    }
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                    BiometricPrompt.ERROR_CANCELED -> {
                        // Requirement 6.3
                        goHome()
                    }
                    else -> goHome()
                }
            }

            override fun onAuthenticationFailed() {
                // Individual failure — BiometricPrompt handles UI feedback
            }
        }

        val prompt = BiometricPrompt(this, executor, callback)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock $appName")
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()

        prompt.authenticate(promptInfo)
    }

    // ─── Pattern (Requirement 7.x) ────────────────────────────────────────────

    private fun setupPatternUI() {
        // Requirement 7.6: redirect to SettingsActivity if no pattern stored
        if (!credentialStore.hasPattern()) {
            redirectToSettings()
            return
        }

        binding.lockTypeSubtitle.text = "Draw Pattern"
        binding.patternSection.visibility = View.VISIBLE

        val patternView = PatternView(this)
        binding.patternViewContainer.removeAllViews()
        binding.patternViewContainer.addView(patternView)

        patternView.listener = object : PatternView.PatternListener {
            override fun onPatternComplete(dotSequence: List<Int>) {
                // Requirement 7.3: >= 4 dots verified by PatternView itself
                if (credentialStore.verifyPattern(dotSequence)) {
                    onAuthSuccess()
                } else {
                    patternView.reset()
                    vibrate50ms()
                    showError("Wrong pattern.")
                }
            }

            override fun onPatternProgress(dotSequence: List<Int>) {
                // no-op during unlock
            }

            override fun onPatternTooShort() {
                // Requirement 7.7
                showError("Pattern must connect at least 4 dots.")
                patternView.reset()
            }
        }
    }

    // ─── Device Credential (Requirement 8.x) ─────────────────────────────────

    private fun setupDeviceCredentialUI() {
        val keyguard = getSystemService(KEYGUARD_SERVICE) as KeyguardManager

        if (!keyguard.isKeyguardSecure) {
            // Requirement 8.4: no screen lock configured
            Toast.makeText(
                this,
                "No screen lock configured. Please set up a screen lock in device settings.",
                Toast.LENGTH_LONG
            ).show()
            goHome()
            return
        }

        binding.lockTypeSubtitle.text = "Use Device Credential"

        @Suppress("DEPRECATION")
        val credentialIntent = keyguard.createConfirmDeviceCredentialIntent(
            "Unlock $appName",
            "Authenticate to unlock $appName"
        )

        if (credentialIntent == null) {
            Toast.makeText(
                this,
                "No screen lock configured. Please set up a screen lock in device settings.",
                Toast.LENGTH_LONG
            ).show()
            goHome()
            return
        }

        startActivityForResult(credentialIntent, REQUEST_DEVICE_CREDENTIAL)
    }

    @Deprecated("Using deprecated API for device credential result")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_DEVICE_CREDENTIAL) {
            if (resultCode == RESULT_OK) {
                // Requirement 8.2
                onAuthSuccess()
            } else {
                // Requirement 8.3: cancelled or failed
                goHome()
            }
        }
    }

    // ─── Shared helpers ───────────────────────────────────────────────────────

    private fun onAuthSuccess() {
        try {
            File(filesDir, "lastApp.txt").writeText(packageNameExtra)
        } catch (e: Exception) {
            // best-effort
        }
        // Launch the target app so it comes to foreground
        val launchIntent = packageManager.getLaunchIntentForPackage(packageNameExtra)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launchIntent)
        }
        finish()
    }

    private fun showError(message: String) {
        binding.errorMessage.text = message
        binding.errorMessage.visibility = View.VISIBLE
    }

    private fun vibrate50ms() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(50)
        }
    }

    private fun goHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }

    private fun redirectToSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
        finish()
    }
}
