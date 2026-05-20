package com.msu.mfalocker

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class PasswordFlowLogicTest : FunSpec({

    test("short password (< 6 chars) in step 1 returns ShortPassword with correct message") {
        val logic = PasswordFlowLogic {}
        val result = logic.onNext("abc", null)
        result.shouldBeInstanceOf<PasswordFlowResult.ShortPassword>()
        (result as PasswordFlowResult.ShortPassword).error shouldBe "Password must be at least 6 characters."
    }

    test("empty password returns ShortPassword") {
        val logic = PasswordFlowLogic {}
        val result = logic.onNext("", null)
        result.shouldBeInstanceOf<PasswordFlowResult.ShortPassword>()
        (result as PasswordFlowResult.ShortPassword).error shouldBe "Password must be at least 6 characters."
    }

    test("valid password (>= 6 chars) in step 1 returns AdvanceToConfirm") {
        val logic = PasswordFlowLogic {}
        val result = logic.onNext("secure1", null)
        result shouldBe PasswordFlowResult.AdvanceToConfirm
    }

    test("matching passwords in step 2 calls setPassword and returns Success") {
        var savedPassword: String? = null
        val logic = PasswordFlowLogic { pw -> savedPassword = pw }
        val result = logic.onNext("secure1", "secure1")
        result shouldBe PasswordFlowResult.Success
        savedPassword shouldBe "secure1"
    }

    test("mismatched passwords in step 2 returns Mismatch with correct message") {
        var setPasswordCalled = false
        val logic = PasswordFlowLogic { setPasswordCalled = true }
        val result = logic.onNext("secure1", "secure2")
        result.shouldBeInstanceOf<PasswordFlowResult.Mismatch>()
        (result as PasswordFlowResult.Mismatch).error shouldBe "Passwords do not match. Please try again."
        setPasswordCalled shouldBe false
    }
})
