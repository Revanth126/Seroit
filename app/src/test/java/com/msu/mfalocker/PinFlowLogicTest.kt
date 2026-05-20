package com.msu.mfalocker

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class PinFlowLogicTest : FunSpec({

    fun enterDigits(logic: PinFlowLogic, pin: String): PinFlowResult {
        var result: PinFlowResult = PinFlowResult.Digit(0)
        for (d in pin) result = logic.onDigit(d.toString())
        return result
    }

    test("entering 6 matching digits across both steps calls setPin and returns Success") {
        // Validates: Requirements 2.4
        var savedPin: String? = null
        val logic = PinFlowLogic { pin -> savedPin = pin }

        val step1Result = enterDigits(logic, "123456")
        step1Result.shouldBeInstanceOf<PinFlowResult.Advance>()
        (step1Result as PinFlowResult.Advance).label shouldBe "Confirm new PIN"
        logic.step shouldBe 2

        val step2Result = enterDigits(logic, "123456")
        step2Result shouldBe PinFlowResult.Success
        savedPin shouldBe "123456"
    }

    test("entering 6 mismatched digits in step 2 returns Mismatch with correct message and resets step to 1") {
        // Validates: Requirements 2.3
        var setPinCalled = false
        val logic = PinFlowLogic { setPinCalled = true }

        enterDigits(logic, "123456")
        val result = enterDigits(logic, "654321")

        result.shouldBeInstanceOf<PinFlowResult.Mismatch>()
        (result as PinFlowResult.Mismatch).error shouldBe "PINs do not match. Please try again."
        logic.step shouldBe 1
        setPinCalled shouldBe false
    }

    test("backspace reduces currentPin length") {
        val logic = PinFlowLogic {}
        logic.onDigit("1")
        logic.onDigit("2")
        logic.onDigit("3")
        val newLength = logic.onBackspace()
        newLength shouldBe 2
        logic.currentPin.length shouldBe 2
    }

    test("entering digits 1 through 5 returns Digit results with correct lengths") {
        val logic = PinFlowLogic {}
        for (i in 1..5) {
            val result = logic.onDigit(i.toString())
            result.shouldBeInstanceOf<PinFlowResult.Digit>()
            (result as PinFlowResult.Digit).length shouldBe i
        }
    }
})
