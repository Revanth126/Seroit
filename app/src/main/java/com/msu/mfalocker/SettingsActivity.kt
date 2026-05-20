@file:Suppress("DEPRECATION")

package com.msu.mfalocker

import android.app.KeyguardManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import com.msu.mfalocker.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var lockTypeStore: LockTypeStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lockTypeStore = LockTypeStore(filesDir)

        setupToolbar()
        setupLockTypeSelector()
        setupChangePinFlow()
        setupChangePasswordFlow()
        setupChangePatternFlow()
    }

    // ─── Toolbar ──────────────────────────────────────────────────────────────

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    // ─── Lock Type Selector ───────────────────────────────────────────────────

    private fun setupLockTypeSelector() {
        val current = lockTypeStore.getGlobalLockType()
        val radioId = when (current) {
            LockType.PIN               -> R.id.radioPin
            LockType.PASSWORD          -> R.id.radioPassword
            LockType.BIOMETRIC         -> R.id.radioBiometric
            LockType.PATTERN           -> R.id.radioPattern
            LockType.DEVICE_CREDENTIAL -> R.id.radioDeviceCredential
            LockType.QUIZ              -> R.id.radioQuiz
        }
        binding.lockTypeGroup.check(radioId)

        binding.lockTypeGroup.setOnCheckedChangeListener { _, checkedId ->
            val selected = when (checkedId) {
                R.id.radioPin               -> LockType.PIN
                R.id.radioPassword          -> LockType.PASSWORD
                R.id.radioBiometric         -> LockType.BIOMETRIC
                R.id.radioPattern           -> LockType.PATTERN
                R.id.radioDeviceCredential  -> LockType.DEVICE_CREDENTIAL
                R.id.radioQuiz              -> LockType.QUIZ
                else                        -> return@setOnCheckedChangeListener
            }
            saveLockType(selected)
        }
    }

    private fun saveLockType(type: LockType) {
        when (type) {
            LockType.BIOMETRIC -> {
                val bm = BiometricManager.from(this)
                if (bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                    != BiometricManager.BIOMETRIC_SUCCESS
                ) {
                    Toast.makeText(
                        this,
                        "No biometrics enrolled. Please enroll a fingerprint in device settings.",
                        Toast.LENGTH_LONG
                    ).show()
                    revertRadioToCurrent()
                    return
                }
            }
            LockType.DEVICE_CREDENTIAL -> {
                val km = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
                if (!km.isKeyguardSecure) {
                    Toast.makeText(
                        this,
                        "No screen lock configured. Please set up a screen lock in device settings.",
                        Toast.LENGTH_LONG
                    ).show()
                    revertRadioToCurrent()
                    return
                }
            }
            else -> { /* no extra validation needed */ }
        }
        lockTypeStore.setGlobalLockType(type)
    }

    private fun revertRadioToCurrent() {
        val current = lockTypeStore.getGlobalLockType()
        val radioId = when (current) {
            LockType.PIN               -> R.id.radioPin
            LockType.PASSWORD          -> R.id.radioPassword
            LockType.BIOMETRIC         -> R.id.radioBiometric
            LockType.PATTERN           -> R.id.radioPattern
            LockType.DEVICE_CREDENTIAL -> R.id.radioDeviceCredential
            LockType.QUIZ              -> R.id.radioQuiz
        }
        binding.lockTypeGroup.setOnCheckedChangeListener(null)
        binding.lockTypeGroup.check(radioId)
        binding.lockTypeGroup.setOnCheckedChangeListener { _, checkedId ->
            val selected = when (checkedId) {
                R.id.radioPin               -> LockType.PIN
                R.id.radioPassword          -> LockType.PASSWORD
                R.id.radioBiometric         -> LockType.BIOMETRIC
                R.id.radioPattern           -> LockType.PATTERN
                R.id.radioDeviceCredential  -> LockType.DEVICE_CREDENTIAL
                R.id.radioQuiz              -> LockType.QUIZ
                else                        -> return@setOnCheckedChangeListener
            }
            saveLockType(selected)
        }
    }

    // ─── Change PIN (Requirements 1.1, 2.1–2.5) ──────────────────────────────

    private fun setupChangePinFlow() {
        binding.btnChangePin.setOnClickListener {
            val dialog = ChangePinDialogFragment()
            dialog.onSuccess = {
                Toast.makeText(this, "PIN updated successfully.", Toast.LENGTH_SHORT).show()
            }
            dialog.show(supportFragmentManager, "ChangePinDialog")
        }
    }

    // ─── Change Password (Requirements 3.1, 4.1–4.5) ─────────────────────────

    private fun setupChangePasswordFlow() {
        binding.btnChangePassword.setOnClickListener {
            val dialog = ChangePasswordDialogFragment()
            dialog.onSuccess = {
                Toast.makeText(this, "Password updated successfully.", Toast.LENGTH_SHORT).show()
            }
            dialog.show(supportFragmentManager, "ChangePasswordDialog")
        }
    }

    // ─── Change Pattern (Requirements 5.1, 6.1–6.5) ──────────────────────────

    private fun setupChangePatternFlow() {
        binding.btnChangePattern.setOnClickListener {
            val dialog = ChangePatternDialogFragment()
            dialog.onSuccess = {
                Toast.makeText(this, "Pattern updated successfully.", Toast.LENGTH_SHORT).show()
            }
            dialog.show(supportFragmentManager, "ChangePatternDialog")
        }
    }
}
