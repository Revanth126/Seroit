package com.msu.mfalocker

enum class GeminiModel(val displayName: String, val apiName: String) {
    GEMINI_2_5_FLASH("Gemini 2.5 Flash", "gemini-2.5-flash"),
    GEMINI_3_FLASH("Gemini 3 Flash", "gemini-3-flash"),
    GEMINI_3_1_FLASH_LITE("Gemini 3.1 Flash Lite", "gemini-3.1-flash-lite"),
    GEMINI_2_5_FLASH_LITE("Gemini 2.5 Flash Lite", "gemini-2.5-flash-lite");

    companion object {
        val DEFAULT = GEMINI_2_5_FLASH
    }
}
