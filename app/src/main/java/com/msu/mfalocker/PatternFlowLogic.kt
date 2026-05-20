package com.msu.mfalocker

sealed class PatternFlowResult {
    data class TooShort(val error: String) : PatternFlowResult()
    data class Advance(val label: String) : PatternFlowResult()
    object Success : PatternFlowResult()
    data class Mismatch(val error: String) : PatternFlowResult()
}

class PatternFlowLogic(private val setPattern: (List<Int>) -> Unit) {

    var step: Int = 1
        private set

    private var firstPattern: List<Int> = emptyList()

    fun onPatternComplete(dots: List<Int>): PatternFlowResult {
        if (dots.size < 4) {
            return PatternFlowResult.TooShort("Pattern must connect at least 4 dots.")
        }
        return if (step == 1) {
            firstPattern = dots
            step = 2
            PatternFlowResult.Advance("Confirm pattern")
        } else {
            if (dots == firstPattern) {
                setPattern(dots)
                PatternFlowResult.Success
            } else {
                reset()
                PatternFlowResult.Mismatch("Patterns do not match. Please try again.")
            }
        }
    }

    fun reset() {
        step = 1
        firstPattern = emptyList()
    }
}
