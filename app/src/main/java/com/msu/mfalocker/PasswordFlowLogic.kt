package com.msu.mfalocker

sealed class PasswordFlowResult {
    data class ShortPassword(val error: String) : PasswordFlowResult()
    object AdvanceToConfirm : PasswordFlowResult()
    object Success : PasswordFlowResult()
    data class Mismatch(val error: String) : PasswordFlowResult()
}

class PasswordFlowLogic(private val setPassword: (String) -> Unit) {

    private var storedNewPassword: String = ""

    /**
     * Handles the Next/Save button press.
     *
     * @param newPassword  The value in the "New password" field.
     * @param confirmPassword  null when in step 1 (confirm field hidden),
     *                         non-null when in step 2 (confirm field visible).
     */
    fun onNext(newPassword: String, confirmPassword: String?): PasswordFlowResult {
        return if (confirmPassword == null) {
            // Step 1: validate length and store the entered password
            if (newPassword.length < 6) {
                PasswordFlowResult.ShortPassword("Password must be at least 6 characters.")
            } else {
                storedNewPassword = newPassword
                PasswordFlowResult.AdvanceToConfirm
            }
        } else {
            // Step 2: compare against the stored original, not the (possibly edited) field
            if (storedNewPassword == confirmPassword) {
                setPassword(storedNewPassword)
                PasswordFlowResult.Success
            } else {
                storedNewPassword = ""
                PasswordFlowResult.Mismatch("Passwords do not match. Please try again.")
            }
        }
    }
}
