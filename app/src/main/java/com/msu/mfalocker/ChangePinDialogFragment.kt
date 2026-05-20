package com.msu.mfalocker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.cardview.widget.CardView
import androidx.fragment.app.DialogFragment
import com.msu.mfalocker.databinding.DialogChangePinBinding

class ChangePinDialogFragment : DialogFragment() {

    var onSuccess: (() -> Unit)? = null

    private var _binding: DialogChangePinBinding? = null
    private val binding get() = _binding!!

    private val dots: List<CardView> by lazy {
        listOf(binding.pinDot1, binding.pinDot2, binding.pinDot3,
               binding.pinDot4, binding.pinDot5, binding.pinDot6)
    }

    private lateinit var pinFlow: PinFlowLogic

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = true
        pinFlow = PinFlowLogic { pin ->
            CredentialStore(requireContext().filesDir).setPin(pin)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogChangePinBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupKeypad()
        updateUi()
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

    private fun setupKeypad() {
        val keyMap = mapOf(
            binding.pinKey0 to "0", binding.pinKey1 to "1", binding.pinKey2 to "2",
            binding.pinKey3 to "3", binding.pinKey4 to "4", binding.pinKey5 to "5",
            binding.pinKey6 to "6", binding.pinKey7 to "7", binding.pinKey8 to "8",
            binding.pinKey9 to "9"
        )
        keyMap.forEach { (key, digit) ->
            key.setOnClickListener { onDigitPressed(digit) }
        }
        binding.pinKeyBackspace.setOnClickListener { onBackspacePressed() }
    }

    private fun onDigitPressed(digit: String) {
        if (pinFlow.currentPin.length >= 6) return
        when (val result = pinFlow.onDigit(digit)) {
            is PinFlowResult.Digit -> updateDots()
            is PinFlowResult.Advance -> {
                binding.pinEntryLabel.text = result.label
                binding.pinFeedback.visibility = View.GONE
                updateDots()
            }
            is PinFlowResult.Success -> {
                dismiss()
                onSuccess?.invoke()
            }
            is PinFlowResult.Mismatch -> {
                binding.pinFeedback.text = result.error
                binding.pinFeedback.visibility = View.VISIBLE
                binding.pinEntryLabel.text = "Enter new PIN"
                updateDots()
            }
        }
    }

    private fun onBackspacePressed() {
        pinFlow.onBackspace()
        updateDots()
    }

    private fun updateUi() {
        binding.pinEntryLabel.text = if (pinFlow.step == 2) "Confirm new PIN" else "Enter new PIN"
        if (pinFlow.step == 1) {
            binding.pinFeedback.visibility = View.GONE
        }
        updateDots()
    }

    private fun updateDots() {
        dots.forEachIndexed { index, dot ->
            dot.visibility = if (index < pinFlow.currentPin.length) View.VISIBLE else View.GONE
        }
    }
}
