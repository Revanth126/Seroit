package com.msu.mfalocker

sealed class PinFlowResult {
    data class Advance(val label: String) : PinFlowResult()
    object Success : PinFlowResult()
    data class Mismatch(val error: String) : PinFlowResult()
    data class Digit(val length: Int) : PinFlowResult()
}

class PinFlowLogic(private val setPin: (String) -> Unit) {

    var step: Int = 1
        private set

    val currentPin = StringBuilder()
    var firstPin: String = ""
        private set

    fun onDigit(d: String): PinFlowResult {
        if (currentPin.length >= 6) return PinFlowResult.Digit(currentPin.length)
        currentPin.append(d)
        if (currentPin.length < 6) return PinFlowResult.Digit(currentPin.length)

        return if (step == 1) {
            firstPin = currentPin.toString()
            currentPin.clear()
            step = 2
            PinFlowResult.Advance("Confirm new PIN")
        } else {
            val entered = currentPin.toString()
            if (entered == firstPin) {
                setPin(entered)
                PinFlowResult.Success
            } else {
                firstPin = ""
                currentPin.clear()
                step = 1
                PinFlowResult.Mismatch("PINs do not match. Please try again.")
            }
        }
    }

    fun onBackspace(): Int {
        if (currentPin.isNotEmpty()) {
            currentPin.deleteCharAt(currentPin.length - 1)
        }
        return currentPin.length
    }
}
