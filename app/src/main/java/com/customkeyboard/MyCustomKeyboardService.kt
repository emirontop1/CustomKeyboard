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
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection

@Suppress("DEPRECATION")
class MyCustomKeyboardService : InputMethodService(), KeyboardView.OnKeyboardActionListener {

    private lateinit var keyboardView: KeyboardView
    private lateinit var qwertyKeyboard: Keyboard
    private lateinit var symbolsKeyboard: Keyboard
    private lateinit var symbolsShiftedKeyboard: Keyboard

    private var isCapsLock = false
    private var isShifted = false
    private var isSymbolsMode = false
    private var lastShiftTime = 0L

    private lateinit var audioManager: AudioManager
    private val config = KeyboardConfig()

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        qwertyKeyboard         = Keyboard(this, R.xml.qwerty)
        symbolsKeyboard        = Keyboard(this, R.xml.symbols)
        symbolsShiftedKeyboard = Keyboard(this, R.xml.symbols_shift)
    }

    override fun onCreateInputView(): View {
        keyboardView = layoutInflater.inflate(R.layout.keyboard_view, null) as KeyboardView
        keyboardView.setOnKeyboardActionListener(this)
        setQwertyKeyboard()
        return keyboardView
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        if (attribute != null) {
            val caps = currentInputConnection?.getCursorCapsMode(attribute.inputType) ?: 0
            isShifted = caps != 0
            updateShiftState()
        }
    }

    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        val ic: InputConnection = currentInputConnection ?: return
        playClickSound()
        triggerHaptic()

        when (primaryCode) {
            Keyboard.KEYCODE_DELETE -> {
                val selection = ic.getSelectedText(0)
                if (selection.isNullOrEmpty()) {
                    ic.deleteSurroundingText(1, 0)
                } else {
                    ic.commitText("", 1)
                }
            }
            Keyboard.KEYCODE_DONE -> {
                val imeOptions = currentInputEditorInfo?.imeOptions
                    ?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
                when (imeOptions) {
                    EditorInfo.IME_ACTION_SEARCH -> ic.performEditorAction(EditorInfo.IME_ACTION_SEARCH)
                    EditorInfo.IME_ACTION_GO     -> ic.performEditorAction(EditorInfo.IME_ACTION_GO)
                    EditorInfo.IME_ACTION_SEND   -> ic.performEditorAction(EditorInfo.IME_ACTION_SEND)
                    EditorInfo.IME_ACTION_NEXT   -> ic.performEditorAction(EditorInfo.IME_ACTION_NEXT)
                    else -> ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                }
            }
            Keyboard.KEYCODE_SHIFT -> handleShift()
            Keyboard.KEYCODE_MODE_CHANGE -> toggleSymbolsKeyboard()
            else -> {
                if (primaryCode > 0) {
                    var code = primaryCode.toChar()
                    if (Character.isLetter(code) && (isShifted || isCapsLock)) {
                        code = code.uppercaseChar()
                    }
                    ic.commitText(code.toString(), 1)
                    if (isShifted && !isCapsLock) {
                        isShifted = false
                        updateShiftState()
                    }
                    autoCapitalise(ic)
                }
            }
        }
    }

    override fun onText(text: CharSequence?) { currentInputConnection?.commitText(text, 1) }
    override fun swipeLeft()  {}
    override fun swipeRight() {}
    override fun swipeDown()  {}
    override fun swipeUp()    {}
    override fun onPress(primaryCode: Int) {}
    override fun onRelease(primaryCode: Int) {}

    private fun setQwertyKeyboard() {
        isSymbolsMode = false
        keyboardView.keyboard = qwertyKeyboard
        keyboardView.isShifted = isShifted
        keyboardView.invalidateAllKeys()
    }

    private fun toggleSymbolsKeyboard() {
        if (isSymbolsMode) {
            setQwertyKeyboard()
        } else {
            isSymbolsMode = true
            keyboardView.keyboard = symbolsKeyboard
            keyboardView.isShifted = false
            keyboardView.invalidateAllKeys()
        }
    }

    private fun handleShift() {
        val now = System.currentTimeMillis()
        if (isSymbolsMode) {
            val next = if (keyboardView.keyboard === symbolsKeyboard) symbolsShiftedKeyboard else symbolsKeyboard
            keyboardView.keyboard = next
            keyboardView.invalidateAllKeys()
            return
        }
        when {
            isCapsLock -> { isCapsLock = false; isShifted = false }
            isShifted && (now - lastShiftTime) < 400L -> { isCapsLock = true; isShifted = true }
            else -> { isShifted = !isShifted; lastShiftTime = now }
        }
        updateShiftState()
    }

    private fun updateShiftState() {
        keyboardView.isShifted = isShifted || isCapsLock
        keyboardView.invalidateAllKeys()
    }

    private fun autoCapitalise(ic: InputConnection) {
        if (!isShifted && !isCapsLock) {
            val text = ic.getTextBeforeCursor(2, 0)?.toString() ?: return
            if (text.length >= 2 && text.last() == ' ' && text[text.length - 2] == '.') {
                isShifted = true
                updateShiftState()
            }
        }
    }

    private fun playClickSound() {
        if (!config.soundEnabled) return
        audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, config.soundVolume)
    }

    @Suppress("DEPRECATION")
    private fun triggerHaptic() {
        if (!config.hapticEnabled) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator.vibrate(VibrationEffect.createOneShot(config.vibrationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(config.vibrationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                v.vibrate(config.vibrationMs)
            }
        }
    }
}
