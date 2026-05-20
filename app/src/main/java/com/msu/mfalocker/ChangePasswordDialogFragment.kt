package com.msu.mfalocker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.msu.mfalocker.databinding.DialogChangePasswordBinding

class ChangePasswordDialogFragment : DialogFragment() {

    var onSuccess: (() -> Unit)? = null

    private var _binding: DialogChangePasswordBinding? = null
    private val binding get() = _binding!!

    private lateinit var passwordFlow: PasswordFlowLogic

    // Tracks whether we are in step 2 (confirm field visible)
    private var inConfirmStep = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = true
        passwordFlow = PasswordFlowLogic { password ->
            CredentialStore(requireContext().filesDir).setPassword(password)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogChangePasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnPasswordNext.setOnClickListener { onButtonPressed() }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun onButtonPressed() {
        val newPassword = binding.passwordInput.text?.toString() ?: ""
        val confirmPassword = if (inConfirmStep) {
            binding.passwordConfirmInput.text?.toString() ?: ""
        } else {
            null
        }

        when (val result = passwordFlow.onNext(newPassword, confirmPassword)) {
            is PasswordFlowResult.ShortPassword -> {
                showFeedback(result.error)
            }
            is PasswordFlowResult.AdvanceToConfirm -> {
                inConfirmStep = true
                binding.passwordConfirmLayout.visibility = View.VISIBLE
                binding.btnPasswordNext.text = "Save"
                hideFeedback()
            }
            is PasswordFlowResult.Success -> {
                dismiss()
                onSuccess?.invoke()
            }
            is PasswordFlowResult.Mismatch -> {
                showFeedback(result.error)
                resetToStep1()
            }
        }
    }

    private fun resetToStep1() {
        inConfirmStep = false
        binding.passwordInput.text?.clear()
        binding.passwordConfirmInput.text?.clear()
        binding.passwordConfirmLayout.visibility = View.GONE
        binding.btnPasswordNext.text = "Next"
    }

    private fun showFeedback(message: String) {
        binding.passwordFeedback.text = message
        binding.passwordFeedback.visibility = View.VISIBLE
    }

    private fun hideFeedback() {
        binding.passwordFeedback.visibility = View.GONE
    }
}
