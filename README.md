# Custom Keyboard — Android IME

A fully customisable Android custom keyboard built with Kotlin, `InputMethodService`, and the classic `KeyboardView` API. Comes with GitHub Actions CI/CD that automatically builds and uploads a debug APK on every push.

---

## Features

| Feature | Details |
|---|---|
| **QWERTY layout** | 10-key rows, shift, delete, enter, space |
| **Symbols page** | Numbers + common punctuation (`?123`) |
| **Shifted symbols** | Extended characters (€, °, √, …) |
| **Shift / Caps Lock** | Single tap = shift; double-tap = caps lock |
| **Auto-capitalise** | After `. ` (period + space) |
| **Sound feedback** | System key-click sounds (respects silent mode) |
| **Haptic feedback** | Configurable vibration length |
| **Key previews** | Pop-up bubble on key press |
| **Settings Activity** | 2-step onboarding guide |
| **GitHub Actions** | Auto-builds debug APK + lint/test reports |

---

## Project Structure

```
CustomKeyboard/
├── .github/
│   └── workflows/
│       └── android.yml          ← CI/CD pipeline
├── app/
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/customkeyboard/
│       │   ├── MyCustomKeyboardService.kt   ← Core IME logic
│       │   ├── KeyboardConfig.kt            ← All tuneable settings
│       │   └── SettingsActivity.kt          ← Launcher + onboarding
│       └── res/
│           ├── drawable/        ← Vector icons + key backgrounds
│           ├── layout/          ← keyboard_view.xml, activity_settings.xml
│           ├── values/          ← colors.xml, dimens.xml, strings.xml, themes.xml
│           └── xml/             ← qwerty.xml, symbols.xml, symbols_shift.xml, input_method.xml
├── build.gradle                 ← Project-level
└── app/build.gradle             ← App-level
```

---

## Quick Start

### 1 — Clone & open in Android Studio

```bash
git clone https://github.com/<your-username>/CustomKeyboard.git
```

Open the root folder in Android Studio (Hedgehog / Iguana or later). Gradle sync runs automatically.

### 2 — Run on device / emulator

1. Hit **Run ▶** (or `./gradlew installDebug`).
2. The **Settings Activity** opens with a 2-step onboarding guide:
   - **Step 1** → taps into *Language & Input* → enable *Custom Keyboard*.
   - **Step 2** → opens the IME picker → select *Custom Keyboard*.

### 3 — Start typing

Open any text field. The keyboard appears automatically.

---

## Customisation Guide

### Colour theme

Edit **`res/values/colors.xml`** — every colour token is documented inline:

```xml
<color name="keyboard_background">#1C1C1E</color>  <!-- panel -->
<color name="key_normal_background">#2C2C2E</color> <!-- key resting -->
<color name="key_pressed_background">#48484A</color><!-- key pressed -->
<color name="key_text">#FFFFFF</color>              <!-- key labels -->
<color name="accent">#4F8EF7</color>                <!-- brand colour -->
```

For **dark/light mode** support, create `res/values-night/colors.xml` and override any values.

### Key sizes

Edit **`res/values/dimens.xml`**:

```xml
<dimen name="key_height">52dp</dimen>
<dimen name="key_horizontal_gap">4dp</dimen>
<dimen name="key_text_size">20sp</dimen>
```

Override in `res/values-sw600dp/dimens.xml` for tablets.

### Keyboard layout

Edit **`res/xml/qwerty.xml`** (or `symbols.xml`, `symbols_shift.xml`):

- `android:codes` → Unicode code point(s) sent on tap.
- `android:keyLabel` → text shown on key face.
- `android:keyWidth` → percentage of row width (`"10%p"`).
- `android:keyHeight` → row height in dp.
- `android:isRepeatable="true"` → keep firing while held (backspace).

### Sound & haptic

Edit **`KeyboardConfig.kt`**:

```kotlin
data class KeyboardConfig(
    val soundEnabled: Boolean  = true,
    val hapticEnabled: Boolean = true,
    val vibrationMs: Long      = 25L,
    val showKeyPreview: Boolean = true,
)
```

To make these **user-adjustable at runtime**, wire them to SharedPreferences via a `PreferenceScreen` in `SettingsActivity`.

---

## GitHub Actions — CI/CD

The workflow at `.github/workflows/android.yml`:

| Trigger | Job | Output |
|---|---|---|
| Push to `main`, `develop`, `release/**` | Build debug APK + lint + unit tests | Downloadable APK artifact (30 days) |
| Pull request to `main` | Same as above | Artifacts on the PR |
| Tag push (`v*.*.*`) | Build **release** APK (unsigned) | Release APK artifact (90 days) |
| Manual (`workflow_dispatch`) | Build debug | On-demand APK |

### Download the APK

1. Go to **Actions** tab on GitHub.
2. Click the latest successful workflow run.
3. Scroll to **Artifacts** and download `CustomKeyboard-debug-N.zip`.
4. Unzip → install the `.apk` on any Android device (enable *Unknown sources*).

---

## Signing for Production

To sign the release APK add these GitHub **Repository Secrets**:

| Secret | Value |
|---|---|
| `KEYSTORE_BASE64` | `base64 -i my-release-key.jks` |
| `KEY_ALIAS` | Your key alias |
| `KEY_PASSWORD` | Key password |
| `STORE_PASSWORD` | Keystore password |

Then add a signing step to the `release` job in `android.yml` using `gradle-sign` or the standard Gradle `signingConfigs` block.

---

## Requirements

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android Gradle Plugin 8.2.x
- Min SDK 21 (Android 5.0 Lollipop)
- Target SDK 34 (Android 14)

---

## Licence

MIT — do whatever you want with this code.
