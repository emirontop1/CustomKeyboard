package com.customkeyboard

/**
 * KeyboardConfig
 *
 * Central configuration object for the keyboard.
 * Modify these defaults, or read them from SharedPreferences
 * in SettingsActivity to make them user-adjustable at runtime.
 *
 * ──────────────────────────────────────────────────────────────────
 *  HOW TO WIRE THIS TO USER PREFERENCES
 *  1. Add a PreferenceScreen in res/xml/preferences.xml
 *  2. In SettingsActivity, use PreferenceManager to read values
 *  3. Pass a context here and read SharedPreferences in init {}
 * ──────────────────────────────────────────────────────────────────
 */
data class KeyboardConfig(

    // ── Sound feedback ────────────────────────────────────────────────────────
    /** Play system key-click sound on each keystroke */
    val soundEnabled: Boolean = true,

    /** Volume multiplier for key clicks (0.0 – 1.0, -1 = system default) */
    val soundVolume: Float = -1f,

    // ── Haptic feedback ───────────────────────────────────────────────────────
    /** Vibrate on each keystroke */
    val hapticEnabled: Boolean = true,

    /** Vibration duration in milliseconds */
    val vibrationMs: Long = 25L,

    // ── Auto-correction / suggestions ─────────────────────────────────────────
    /** Show suggestion strip above keyboard (future expansion) */
    val showSuggestions: Boolean = false,

    /** Auto-correct typed words to dictionary matches (future expansion) */
    val autoCorrectEnabled: Boolean = false,

    // ── Visual ────────────────────────────────────────────────────────────────
    /** Show character pop-up preview on key press */
    val showKeyPreview: Boolean = true,
)
