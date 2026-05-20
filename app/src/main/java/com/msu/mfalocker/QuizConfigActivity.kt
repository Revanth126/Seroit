@file:Suppress("DEPRECATION")

package com.msu.mfalocker

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.msu.mfalocker.databinding.ActivityQuizConfigBinding

class QuizConfigActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQuizConfigBinding
    private lateinit var quizConfigStore: QuizConfigStore
    private lateinit var lockTypeStore: LockTypeStore
    private lateinit var packageNameExtra: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuizConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)

        packageNameExtra = intent.getStringExtra("packageName") ?: run {
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        quizConfigStore = QuizConfigStore(filesDir)
        lockTypeStore = LockTypeStore(filesDir)

        setupToolbar()
        prepopulateIfExists()
        setupButtons()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private fun prepopulateIfExists() {
        val existing = quizConfigStore.getQuizConfig(packageNameExtra) ?: return

        // Pre-select topic
        val topicRadioId = when (existing.topic) {
            "English Grammar"   -> R.id.radioTopicEnglishGrammar
            "Aptitude"          -> R.id.radioTopicAptitude
            "Reasoning"         -> R.id.radioTopicReasoning
            "General Knowledge" -> R.id.radioTopicGeneralKnowledge
            "Science"           -> R.id.radioTopicScience
            "History"           -> R.id.radioTopicHistory
            "Geography"         -> R.id.radioTopicGeography
            "Mathematics"       -> R.id.radioTopicMathematics
            else                -> -1
        }
        if (topicRadioId != -1) binding.topicGroup.check(topicRadioId)

        // Pre-select difficulty
        val difficultyRadioId = when (existing.difficulty) {
            "Easy"   -> R.id.radioDifficultyEasy
            "Medium" -> R.id.radioDifficultyMedium
            "Hard"   -> R.id.radioDifficultyHard
            else     -> -1
        }
        if (difficultyRadioId != -1) binding.difficultyGroup.check(difficultyRadioId)
    }

    private fun setupButtons() {
        binding.btnConfirm.setOnClickListener { onConfirm() }
        binding.btnCancel.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private fun onConfirm() {
        val topic = when (binding.topicGroup.checkedRadioButtonId) {
            R.id.radioTopicEnglishGrammar   -> "English Grammar"
            R.id.radioTopicAptitude         -> "Aptitude"
            R.id.radioTopicReasoning        -> "Reasoning"
            R.id.radioTopicGeneralKnowledge -> "General Knowledge"
            R.id.radioTopicScience          -> "Science"
            R.id.radioTopicHistory          -> "History"
            R.id.radioTopicGeography        -> "Geography"
            R.id.radioTopicMathematics      -> "Mathematics"
            else                            -> null
        }

        val difficulty = when (binding.difficultyGroup.checkedRadioButtonId) {
            R.id.radioDifficultyEasy   -> "Easy"
            R.id.radioDifficultyMedium -> "Medium"
            R.id.radioDifficultyHard   -> "Hard"
            else                       -> null
        }

        if (topic == null || difficulty == null) {
            Toast.makeText(this, "Please select both a topic and a difficulty.", Toast.LENGTH_SHORT).show()
            return
        }

        quizConfigStore.setQuizConfig(packageNameExtra, QuizConfig(topic, difficulty))
        lockTypeStore.setPerAppLockType(packageNameExtra, LockType.QUIZ)
        setResult(RESULT_OK)
        finish()
    }

    override fun onBackPressed() {
        setResult(RESULT_CANCELED)
        finish()
    }
}
