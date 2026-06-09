package com.aitranslator.ui.main

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.animation.AnimationUtils
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.aitranslator.R
import com.aitranslator.data.model.Language
import com.aitranslator.databinding.ActivityMainBinding
import com.aitranslator.ui.history.HistoryActivity
import com.aitranslator.utils.TextToSpeechManager
import com.aitranslator.utils.copyToClipboard
import com.aitranslator.utils.hide
import com.aitranslator.utils.show
import com.aitranslator.utils.showSnackbar
import com.aitranslator.viewmodel.MainViewModel
import com.google.android.material.snackbar.Snackbar

/**
 * Main screen of the AI Language Translator app.
 *
 * Responsibilities:
 *  - Renders the input text box, language selectors, and action buttons.
 *  - Observes [MainViewModel] LiveData to drive UI state changes.
 *  - Manages the [TextToSpeechManager] lifecycle.
 *  - Persists the dark-mode toggle preference.
 */
class MainActivity : AppCompatActivity() {

    // View binding instance — non-null after setContentView
    private lateinit var binding: ActivityMainBinding

    // ViewModel scoped to this Activity
    private val viewModel: MainViewModel by viewModels()

    // Text-to-speech engine wrapper
    private lateinit var ttsManager: TextToSpeechManager

    // Tracks the last successfully translated text for TTS and copy actions
    private var lastTranslatedText: String = ""

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize TTS engine
        ttsManager = TextToSpeechManager(this)

        setupToolbar()
        setupLanguageDropdowns()
        setupInputWatcher()
        setupButtons()
        observeViewModel()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release TTS resources to avoid memory leaks
        ttsManager.shutdown()
    }

    // -------------------------------------------------------------------------
    // Options menu (history + dark mode toggle)
    // -------------------------------------------------------------------------

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_history -> {
                startActivity(Intent(this, HistoryActivity::class.java))
                true
            }
            R.id.action_dark_mode -> {
                toggleDarkMode()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // -------------------------------------------------------------------------
    // Setup helpers
    // -------------------------------------------------------------------------

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    /** Populates both language spinners with [Language.SUPPORTED]. */
    private fun setupLanguageDropdowns() {
        val languageNames = Language.SUPPORTED.map { it.name }
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            languageNames
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        binding.spinnerSourceLanguage.adapter = adapter
        binding.spinnerTargetLanguage.adapter = adapter

        // Default: English → French
        binding.spinnerSourceLanguage.setSelection(0)
        binding.spinnerTargetLanguage.setSelection(4)

        // Notify ViewModel when spinner selection changes
        binding.spinnerSourceLanguage.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: android.view.View?,
                    position: Int,
                    id: Long
                ) { viewModel.setSourceLanguage(position) }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }

        binding.spinnerTargetLanguage.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: android.view.View?,
                    position: Int,
                    id: Long
                ) { viewModel.setTargetLanguage(position) }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }
    }

    /** Attaches a [TextWatcher] to the input field to drive the character counter. */
    private fun setupInputWatcher() {
        binding.etInputText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.updateCharacterCount(s?.toString() ?: "")
            }
        })
    }

    /** Wires all button click listeners. */
    private fun setupButtons() {
        // Translate
        binding.btnTranslate.setOnClickListener {
            val inputText = binding.etInputText.text.toString()
            viewModel.translate(inputText)
        }

        // Clear input and output
        binding.btnClear.setOnClickListener {
            binding.etInputText.text?.clear()
            binding.tvTranslatedText.text = ""
            lastTranslatedText = ""
            viewModel.clearTranslation()
            binding.cardTranslationOutput.hide()
        }

        // Swap source ↔ target language
        binding.btnSwapLanguages.setOnClickListener {
            val anim = AnimationUtils.loadAnimation(this, R.anim.rotate_swap)
            binding.btnSwapLanguages.startAnimation(anim)

            // Mirror spinner positions
            val sourcePos = binding.spinnerSourceLanguage.selectedItemPosition
            val targetPos = binding.spinnerTargetLanguage.selectedItemPosition
            binding.spinnerSourceLanguage.setSelection(targetPos)
            binding.spinnerTargetLanguage.setSelection(sourcePos)

            viewModel.swapLanguages()

            // If we had a translation, swap the text too
            val inputText = binding.etInputText.text.toString()
            if (lastTranslatedText.isNotEmpty()) {
                binding.etInputText.setText(lastTranslatedText)
                binding.tvTranslatedText.text = inputText
                lastTranslatedText = inputText
            }
        }

        // Copy translated text to clipboard
        binding.btnCopyTranslation.setOnClickListener {
            if (lastTranslatedText.isNotEmpty()) {
                copyToClipboard(lastTranslatedText)
            } else {
                binding.root.showSnackbar("Nothing to copy yet")
            }
        }

        // Speak the translated text via TTS
        binding.btnTextToSpeech.setOnClickListener {
            if (lastTranslatedText.isNotEmpty()) {
                val targetCode = viewModel.selectedTargetLanguage.value?.code ?: "en"
                if (ttsManager.isAvailable()) {
                    ttsManager.speak(lastTranslatedText, targetCode)
                } else {
                    binding.root.showSnackbar("Text-to-speech is not available on this device")
                }
            } else {
                binding.root.showSnackbar("Nothing to speak yet")
            }
        }
    }

    // -------------------------------------------------------------------------
    // ViewModel observers
    // -------------------------------------------------------------------------

    private fun observeViewModel() {
        // Translation state
        viewModel.translationState.observe(this) { state ->
            when (state) {
                is MainViewModel.TranslationState.Idle -> {
                    binding.progressBar.hide()
                    binding.btnTranslate.isEnabled = true
                }
                is MainViewModel.TranslationState.Loading -> {
                    binding.progressBar.show()
                    binding.btnTranslate.isEnabled = false
                    binding.btnTranslate.text = getString(R.string.translating)
                }
                is MainViewModel.TranslationState.Success -> {
                    binding.progressBar.hide()
                    binding.btnTranslate.isEnabled = true
                    binding.btnTranslate.text = getString(R.string.translate)

                    lastTranslatedText = state.translatedText
                    binding.tvTranslatedText.text = state.translatedText
                    binding.cardTranslationOutput.show()

                    // Animate the result card in
                    val anim = AnimationUtils.loadAnimation(this, R.anim.fade_in_slide_up)
                    binding.cardTranslationOutput.startAnimation(anim)
                }
                is MainViewModel.TranslationState.Error -> {
                    binding.progressBar.hide()
                    binding.btnTranslate.isEnabled = true
                    binding.btnTranslate.text = getString(R.string.translate)

                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG)
                        .setBackgroundTint(
                            ContextCompat.getColor(this, R.color.error_color)
                        )
                        .show()
                }
            }
        }

        // Character counter
        viewModel.characterCount.observe(this) { count ->
            binding.tvCharCount.text = getString(R.string.char_count_format, count)
        }

        // Keep spinner positions in sync with ViewModel (e.g. after swap)
        viewModel.selectedSourceLanguage.observe(this) { language ->
            val pos = Language.SUPPORTED.indexOf(language)
            if (pos >= 0 && binding.spinnerSourceLanguage.selectedItemPosition != pos) {
                binding.spinnerSourceLanguage.setSelection(pos)
            }
        }

        viewModel.selectedTargetLanguage.observe(this) { language ->
            val pos = Language.SUPPORTED.indexOf(language)
            if (pos >= 0 && binding.spinnerTargetLanguage.selectedItemPosition != pos) {
                binding.spinnerTargetLanguage.setSelection(pos)
            }
        }
    }

    // -------------------------------------------------------------------------
    // Dark mode
    // -------------------------------------------------------------------------

    private fun toggleDarkMode() {
        val currentMode = AppCompatDelegate.getDefaultNightMode()
        val newMode = if (currentMode == AppCompatDelegate.MODE_NIGHT_YES)
            AppCompatDelegate.MODE_NIGHT_NO
        else
            AppCompatDelegate.MODE_NIGHT_YES

        AppCompatDelegate.setDefaultNightMode(newMode)

        // Persist preference
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putInt(KEY_NIGHT_MODE, newMode)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "translator_prefs"
        private const val KEY_NIGHT_MODE = "night_mode"
    }
}
