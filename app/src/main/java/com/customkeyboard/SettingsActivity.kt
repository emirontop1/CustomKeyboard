package com.customkeyboard

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import com.customkeyboard.databinding.ActivitySettingsBinding

/**
 * SettingsActivity
 *
 * The launcher Activity that:
 *  1. Guides the user through enabling the keyboard in system settings
 *  2. Guides the user through selecting it as the active input method
 *  3. Will host preference fragments for runtime keyboard customisation
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        // Refresh button states whenever we come back from Settings
        updateButtonStates()
    }

    private fun setupClickListeners() {
        // Step 1: Open Language & Input settings so user can enable our IME
        binding.btnEnableKeyboard.setOnClickListener {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }

        // Step 2: Open IME picker so user can switch to our keyboard
        binding.btnSelectKeyboard.setOnClickListener {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
        }
    }

    private fun updateButtonStates() {
        val isEnabled = isKeyboardEnabled()
        binding.btnEnableKeyboard.isEnabled = !isEnabled
        binding.tvStep1Status.text = if (isEnabled)
            getString(R.string.status_enabled)
        else
            getString(R.string.status_not_enabled)
        binding.btnSelectKeyboard.isEnabled = isEnabled
    }

    /** Check whether our IME is in the system's enabled IME list */
    private fun isKeyboardEnabled(): Boolean {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        return imm.enabledInputMethodList.any {
            it.packageName == packageName
        }
    }
}
