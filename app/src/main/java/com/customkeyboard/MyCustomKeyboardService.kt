package com.customkeyboard

import android.content.Context
import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection

/**
 * MyCustomKeyboardService
 *
 * The core InputMethodService that drives the custom keyboard.
 * Handles:
 *  - Keyboard view inflation and state management (letters / symbols / shifted)
 *  - Key press events → text commit / delete / enter / shift / symbols
 *  - Sound and haptic feedback (respects system settings)
 *  - Auto-capitalisation at sentence start
 *
 * ─── Customisation quick-reference ───────────────────────────────────────────
 *  • Swap key layout  → edit res/xml/qwerty.xml (or add a new XML and load it below)
 *  • Change theme     → edit res/layout/keyboard_view.xml attributes / styles.xml
 *  • Toggle sound     → KeyboardConfig.soundEnabled
 *  • Toggle haptic    → KeyboardConfig.hapticEnabled
 *  • Vibration length → KeyboardConfig.vibrationMs
 * ─────────────────────────────────────────────────────────────────────────────
 */
class MyCustomKeyboardService : InputMethodService(), KeyboardView.OnKeyboardActionListener {

    // ── Views ─────────────────────────────────────────────────────────────────
    private lateinit var keyboardView: KeyboardView

    // ── Keyboard layouts ──────────────────────────────────────────────────────
    private lateinit var qwertyKeyboard: Keyboard
    private lateinit var symbolsKeyboard: Keyboard
    private lateinit var symbolsShiftedKeyboard: Keyboard

    // ── State ─────────────────────────────────────────────────────────────────
    private var currentKeyboard: Keyboard? = null
    private var isCapsLock = false          // double-tap shift → caps lock
    private var isShifted = false
    private var isSymbolsMode = false
    private var lastShiftTime = 0L          // used for double-tap detection

    // ── System services ───────────────────────────────────────────────────────
    private lateinit var audioManager: AudioManager

    // ── Config (change these to customise behaviour) ──────────────────────────
    private val config = KeyboardConfig()

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Initialise all keyboard layouts once
        qwertyKeyboard         = Keyboard(this, R.xml.qwerty)
        symbolsKeyboard        = Keyboard(this, R.xml.symbols)
        symbolsShiftedKeyboard = Keyboard(this, R.xml.symbols_shift)
    }

    override fun onCreateInputView(): View {
        keyboardView = layoutInflater
            .inflate(R.layout.keyboard_view, null) as KeyboardView

        keyboardView.setOnKeyboardActionListener(this)
        setQwertyKeyboard()

        return keyboardView
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        // Auto-capitalise for text fields that request it
        if (attribute != null) {
            val caps = currentInputConnection?.getCursorCapsMode(attribute.inputType) ?: 0
            isShifted = caps != 0
            updateShiftState()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // KeyboardView.OnKeyboardActionListener implementation
    // ─────────────────────────────────────────────────────────────────────────

    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        val ic: InputConnection = currentInputConnection ?: return
        playClickSound()
        triggerHaptic()

        when (primaryCode) {

            // ── Delete ────────────────────────────────────────────────────────
            Keyboard.KEYCODE_DELETE -> {
                val selection = ic.getSelectedText(0)
                if (selection.isNullOrEmpty()) {
                    ic.deleteSurroundingText(1, 0)
                } else {
                    ic.commitText("", 1)
                }
            }

            // ── Enter / Done ──────────────────────────────────────────────────
            Keyboard.KEYCODE_DONE -> {
                val imeOptions = currentInputEditorInfo?.imeOptions
                    ?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE

                when (imeOptions) {
                    EditorInfo.IME_ACTION_SEARCH  -> ic.performEditorAction(EditorInfo.IME_ACTION_SEARCH)
                    EditorInfo.IME_ACTION_GO      -> ic.performEditorAction(EditorInfo.IME_ACTION_GO)
                    EditorInfo.IME_ACTION_SEND    -> ic.performEditorAction(EditorInfo.IME_ACTION_SEND)
                    EditorInfo.IME_ACTION_NEXT    -> ic.performEditorAction(EditorInfo.IME_ACTION_NEXT)
                    else                          -> ic.sendKeyEvent(
                        KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)
                    )
                }
            }

            // ── Shift ─────────────────────────────────────────────────────────
            Keyboard.KEYCODE_SHIFT -> handleShift()

            // ── Switch to symbols ─────────────────────────────────────────────
            Keyboard.KEYCODE_MODE_CHANGE -> toggleSymbolsKeyboard()

            // ── Regular character ─────────────────────────────────────────────
            else -> {
                if (primaryCode > 0) {
                    var code = primaryCode.toChar()
                    if (Character.isLetter(code) && (isShifted || isCapsLock)) {
                        code = code.uppercaseChar()
                    }
                    ic.commitText(code.toString(), 1)

                    // Reset single-shift after one letter (caps lock keeps going)
                    if (isShifted && !isCapsLock) {
                        isShifted = false
                        updateShiftState()
                    }

                    // Auto-capitalise after period + space
                    autoCapitalise(ic)
                }
            }
        }
    }

    override fun onText(text: CharSequence?) {
        currentInputConnection?.commitText(text, 1)
    }

    override fun swipeLeft()  { /* optional: could trigger delete-word */ }
    override fun swipeRight() { /* optional: could trigger cursor-right */ }
    override fun swipeDown()  { /* optional: could dismiss keyboard */ }
    override fun swipeUp()    { /* optional: could open emoji picker */ }

    override fun onPress(primaryCode: Int) {
        // Key preview is handled by KeyboardView automatically
    }

    override fun onRelease(primaryCode: Int) {
        // No-op — add long-press handling here if needed
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Switch to the QWERTY letter layout */
    private fun setQwertyKeyboard() {
        isSymbolsMode = false
        currentKeyboard = qwertyKeyboard
        keyboardView.keyboard = qwertyKeyboard
        keyboardView.isShifted = isShifted
        keyboardView.invalidateAllKeys()
    }

    /** Toggle between QWERTY and Symbols */
    private fun toggleSymbolsKeyboard() {
        if (isSymbolsMode) {
            setQwertyKeyboard()
        } else {
            isSymbolsMode = true
            currentKeyboard = symbolsKeyboard
            keyboardView.keyboard = symbolsKeyboard
            keyboardView.isShifted = false
            keyboardView.invalidateAllKeys()
        }
    }

    /**
     * Handle shift key:
     *  - Single tap  → shift ON (next letter capitalised)
     *  - Double tap  → caps lock ON
     *  - Tap while caps lock → caps lock OFF
     */
    private fun handleShift() {
        val now = System.currentTimeMillis()

        if (isSymbolsMode) {
            // In symbols mode: toggle between symbols and shifted-symbols
            val nextKeyboard = if (keyboardView.keyboard === symbolsKeyboard) {
                symbolsShiftedKeyboard
            } else {
                symbolsKeyboard
            }
            currentKeyboard = nextKeyboard
            keyboardView.keyboard = nextKeyboard
            keyboardView.invalidateAllKeys()
            return
        }

        when {
            isCapsLock -> {
                isCapsLock = false
                isShifted  = false
            }
            isShifted && (now - lastShiftTime) < DOUBLE_TAP_THRESHOLD_MS -> {
                // Second tap within threshold → caps lock
                isCapsLock = true
                isShifted  = true
            }
            else -> {
                isShifted = !isShifted
                lastShiftTime = now
            }
        }
        updateShiftState()
    }

    /** Push current shift/caps state to the view */
    private fun updateShiftState() {
        keyboardView.isShifted = isShifted || isCapsLock
        keyboardView.invalidateAllKeys()
    }

    /** Auto-capitalise after ". " (period + space) */
    private fun autoCapitalise(ic: InputConnection) {
        if (!isShifted && !isCapsLock) {
            val textBefore = ic.getTextBeforeCursor(2, 0)?.toString() ?: return
            if (textBefore.length >= 2 &&
                textBefore[textBefore.length - 1] == ' ' &&
                textBefore[textBefore.length - 2] == '.'
            ) {
                isShifted = true
                updateShiftState()
            }
        }
    }

    /** Play key click sound respecting system volume */
    private fun playClickSound() {
        if (!config.soundEnabled) return
        audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, config.soundVolume)
    }

    /** Trigger vibration haptic feedback */
    private fun triggerHaptic() {
        if (!config.hapticEnabled) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator.vibrate(
                VibrationEffect.createOneShot(
                    config.vibrationMs,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            @Suppress("DEPRECATION")
            val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(
                    VibrationEffect.createOneShot(
                        config.vibrationMs,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(config.vibrationMs)
            }
        }
    }

    companion object {
        /** Max milliseconds between two shift taps to count as "double tap" */
        private const val DOUBLE_TAP_THRESHOLD_MS = 400L
    }
}
