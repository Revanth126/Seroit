@file:Suppress("DEPRECATION")

package com.msu.mfalocker

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.msu.mfalocker.databinding.ActivityQuizBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class QuizActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PACKAGE_NAME = "packageName"
    }

    private lateinit var binding: ActivityQuizBinding

    private var packageNameExtra: String = ""
    private var questions: List<QuizQuestion> = emptyList()
    private var currentIndex: Int = 0
    private var score: Int = 0
    private var retryCount: Int = 0
    private var onResultScreen: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Task 5.2: FLAG_SECURE — prevent screenshots/screen recording
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        binding = ActivityQuizBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Task 5.2: back press navigates home (keeps app locked)
        onBackPressedDispatcher.addCallback(
            this,
            object : androidx.activity.OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    goHome()
                }
            }
        )

        // Task 5.2: resolve packageName extra, display app icon and name
        packageNameExtra = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
        resolveAppInfo(packageNameExtra)?.let { appInfo ->
            binding.imgAppIcon.setImageDrawable(packageManager.getApplicationIcon(appInfo))
            binding.txtAppName.text = packageManager.getApplicationLabel(appInfo).toString()
        }

        // Task 5.3: start loading
        loadQuiz()
    }

    // ─── Task 5.3: Quiz loading ───────────────────────────────────────────────

    private fun loadQuiz() {
        showLoading()

        val config = QuizConfigStore(filesDir).getQuizConfig(packageNameExtra)
        val topic = config?.topic ?: "General Knowledge"
        val difficulty = config?.difficulty ?: "Medium"

        lifecycleScope.launch {
            try {
                val result = LlmClient.generateQuiz(topic, difficulty)
                questions = result
                currentIndex = 0
                score = 0
                retryCount = 0
                hideLoading()
                showQuestion(currentIndex)
            } catch (e: Exception) {
                android.util.Log.e("QuizActivity", "loadQuiz failed: ${e.message}", e)
                retryCount++
                if (retryCount >= 3) {
                    showFatalError(e.message)
                } else {
                    showRetryError(e.message)
                }
            }
        }
    }

    private fun showLoading() {
        binding.sectionLoading.visibility = View.VISIBLE
        binding.sectionQuestion.visibility = View.GONE
        binding.sectionExplanation.visibility = View.GONE
        binding.sectionResult.visibility = View.GONE
        binding.txtLoading.text = "Generating your quiz…"
        binding.btnRetry.visibility = View.GONE
    }

    private fun hideLoading() {
        binding.sectionLoading.visibility = View.GONE
    }

    private fun showRetryError(error: String? = null) {
        binding.sectionLoading.visibility = View.VISIBLE
        binding.sectionQuestion.visibility = View.GONE
        binding.sectionExplanation.visibility = View.GONE
        binding.sectionResult.visibility = View.GONE
        binding.txtLoading.text = "Failed to load quiz. Please try again." +
            if (error != null) "\n\nError: $error" else ""
        binding.btnRetry.text = "Retry"
        binding.btnRetry.visibility = View.VISIBLE
        binding.btnRetry.setOnClickListener { loadQuiz() }
    }

    private fun showFatalError(error: String? = null) {
        binding.sectionLoading.visibility = View.VISIBLE
        binding.sectionQuestion.visibility = View.GONE
        binding.sectionExplanation.visibility = View.GONE
        binding.sectionResult.visibility = View.GONE
        binding.txtLoading.text = "Unable to generate quiz. The app will remain locked." +
            if (error != null) "\n\nError: $error" else ""
        binding.btnRetry.text = "Close"
        binding.btnRetry.visibility = View.VISIBLE
        binding.btnRetry.setOnClickListener { goHome() }
    }

    // ─── Task 5.4: Question display ──────────────────────────────────────────

    private fun showQuestion(index: Int) {
        val question = questions[index]
        binding.sectionQuestion.visibility = View.VISIBLE
        binding.sectionExplanation.visibility = View.GONE

        // Progress label "N / 5"
        binding.txtProgress.text = "${index + 1} / 5"
        binding.txtQuestion.text = question.question

        val prefixes = listOf("A. ", "B. ", "C. ", "D. ")
        val optionButtons = listOf(
            binding.btnOptionA,
            binding.btnOptionB,
            binding.btnOptionC,
            binding.btnOptionD
        )

        optionButtons.forEachIndexed { i, btn ->
            btn.text = prefixes[i] + question.options[i]
            btn.isEnabled = true
            btn.backgroundTintList = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.TRANSPARENT
            )
            btn.setOnClickListener { onOptionSelected(i) }
        }
    }

    // ─── Task 6.2: Answer selection ──────────────────────────────────────────

    private fun onOptionSelected(selectedIndex: Int) {
        val question = questions[currentIndex]
        val optionButtons = listOf(
            binding.btnOptionA,
            binding.btnOptionB,
            binding.btnOptionC,
            binding.btnOptionD
        )

        // Disable all buttons
        optionButtons.forEach { it.isEnabled = false }

        // Highlight correct (green) and wrong selection (red)
        optionButtons[question.correctIndex].backgroundTintList =
            android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50"))

        if (selectedIndex != question.correctIndex) {
            optionButtons[selectedIndex].backgroundTintList =
                android.content.res.ColorStateList.valueOf(Color.parseColor("#F44336"))
        }

        // Increment score if correct
        if (selectedIndex == question.correctIndex) {
            score++
        }

        // Show explanation
        binding.txtExplanation.text = question.explanation
        binding.sectionExplanation.visibility = View.VISIBLE

        // Show Next or See Result
        val isLastQuestion = currentIndex == questions.size - 1
        binding.btnNext.text = if (isLastQuestion) "See Result" else "Next"
        binding.btnNext.setOnClickListener {
            if (isLastQuestion) {
                showResult()
            } else {
                goToNextQuestion()
            }
        }
    }

    // ─── Task 6.3: Next / See Result ─────────────────────────────────────────

    private fun goToNextQuestion() {
        currentIndex++
        // Reset button colours
        listOf(
            binding.btnOptionA,
            binding.btnOptionB,
            binding.btnOptionC,
            binding.btnOptionD
        ).forEach { it.backgroundTintList = android.content.res.ColorStateList.valueOf(
            android.graphics.Color.TRANSPARENT
        ) }
        binding.sectionExplanation.visibility = View.GONE
        showQuestion(currentIndex)
    }

    // ─── Task 7.2: Result logic ───────────────────────────────────────────────

    private fun showResult() {
        onResultScreen = true
        binding.sectionQuestion.visibility = View.GONE
        binding.sectionExplanation.visibility = View.GONE
        binding.sectionResult.visibility = View.VISIBLE

        binding.txtScore.text = "You scored $score / 5"

        if (score >= 4) {
            binding.txtResultMessage.text = "Unlocked!"
            binding.btnClose.visibility = View.GONE
            // Write lastApp BEFORE launching so the service guard is in place
            // before the target app appears in foreground.
            try {
                File(filesDir, "lastApp.txt").writeText(packageNameExtra)
            } catch (e: Exception) {
                // best-effort
            }
            lifecycleScope.launch {
                delay(1000)
                val launchIntent = packageManager.getLaunchIntentForPackage(packageNameExtra)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(launchIntent)
                }
                finish()
            }
        } else {
            binding.txtResultMessage.text =
                "Not enough correct answers. The app remains locked."
            binding.btnClose.visibility = View.VISIBLE
            binding.btnClose.setOnClickListener { goHome() }
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun goHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }

    private fun resolveAppInfo(pkg: String): android.content.pm.ApplicationInfo? {
        if (pkg.isEmpty()) return null
        return try {
            val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            packages.firstOrNull { it.packageName == pkg }
        } catch (e: Exception) {
            null
        }
    }
}
