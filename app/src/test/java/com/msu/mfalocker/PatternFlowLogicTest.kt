package com.msu.mfalocker

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class PatternFlowLogicTest : FunSpec({

    test("too-short pattern (< 4 dots) returns TooShort with correct message") {
        val logic = PatternFlowLogic {}
        val result = logic.onPatternComplete(listOf(0, 1, 2))
        result.shouldBeInstanceOf<PatternFlowResult.TooShort>()
        (result as PatternFlowResult.TooShort).error shouldBe "Pattern must connect at least 4 dots."
    }

    test("empty pattern returns TooShort") {
        val logic = PatternFlowLogic {}
        val result = logic.onPatternComplete(emptyList())
        result.shouldBeInstanceOf<PatternFlowResult.TooShort>()
    }

    test("valid pattern in step 1 advances to step 2 with Confirm label") {
        val logic = PatternFlowLogic {}
        val result = logic.onPatternComplete(listOf(0, 1, 2, 3))
        result.shouldBeInstanceOf<PatternFlowResult.Advance>()
        (result as PatternFlowResult.Advance).label shouldBe "Confirm pattern"
        logic.step shouldBe 2
    }

    test("matching patterns in step 2 calls setPattern and returns Success") {
        var savedPattern: List<Int>? = null
        val logic = PatternFlowLogic { dots -> savedPattern = dots }
        logic.onPatternComplete(listOf(0, 1, 2, 3))
        val result = logic.onPatternComplete(listOf(0, 1, 2, 3))
        result shouldBe PatternFlowResult.Success
        savedPattern shouldBe listOf(0, 1, 2, 3)
    }

    test("mismatched patterns in step 2 returns Mismatch and resets to step 1") {
        var setPatternCalled = false
        val logic = PatternFlowLogic { setPatternCalled = true }
        logic.onPatternComplete(listOf(0, 1, 2, 3))
        val result = logic.onPatternComplete(listOf(3, 2, 1, 0))
        result.shouldBeInstanceOf<PatternFlowResult.Mismatch>()
        (result as PatternFlowResult.Mismatch).error shouldBe "Patterns do not match. Please try again."
        logic.step shouldBe 1
        setPatternCalled shouldBe false
    }

    test("reset() resets step to 1") {
        val logic = PatternFlowLogic {}
        logic.onPatternComplete(listOf(0, 1, 2, 3))
        logic.step shouldBe 2
        logic.reset()
        logic.step shouldBe 1
    }
})
