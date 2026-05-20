package com.msu.mfalocker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.msu.mfalocker.databinding.DialogChangePatternBinding

class ChangePatternDialogFragment : DialogFragment() {

    var onSuccess: (() -> Unit)? = null

    private var _binding: DialogChangePatternBinding? = null
    private val binding get() = _binding!!

    private lateinit var patternView: PatternView
    private lateinit var patternFlow: PatternFlowLogic

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = true
        patternFlow = PatternFlowLogic { dots ->
            CredentialStore(requireContext().filesDir).setPattern(dots)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogChangePatternBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        patternView = PatternView(requireContext())
        binding.patternContainer.addView(
            patternView,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        patternView.listener = object : PatternView.PatternListener {
            override fun onPatternComplete(dotSequence: List<Int>) {
                when (val result = patternFlow.onPatternComplete(dotSequence)) {
                    is PatternFlowResult.TooShort -> {
                        showFeedback(result.error)
                        patternView.reset()
                    }
                    is PatternFlowResult.Advance -> {
                        binding.patternEntryLabel.text = result.label
                        hideFeedback()
                        patternView.reset()
                    }
                    is PatternFlowResult.Success -> {
                        dismiss()
                        onSuccess?.invoke()
                    }
                    is PatternFlowResult.Mismatch -> {
                        showFeedback(result.error)
                        binding.patternEntryLabel.text = "Draw new pattern"
                        patternView.reset()
                    }
                }
            }

            override fun onPatternProgress(dotSequence: List<Int>) {
                // no-op
            }

            override fun onPatternTooShort() {
                showFeedback("Pattern must connect at least 4 dots.")
                patternView.reset()
            }
        }

        binding.patternEntryLabel.text = "Draw new pattern"
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

    private fun showFeedback(message: String) {
        binding.patternFeedback.text = message
        binding.patternFeedback.visibility = View.VISIBLE
    }

    private fun hideFeedback() {
        binding.patternFeedback.visibility = View.GONE
    }
}
