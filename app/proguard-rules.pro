# Custom Keyboard ProGuard Rules

# Keep InputMethodService subclasses
-keep class * extends android.inputmethodservice.InputMethodService { *; }
-keep class * extends android.inputmethodservice.Keyboard { *; }
-keep class * extends android.inputmethodservice.KeyboardView { *; }

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep preference classes
-keep class androidx.preference.** { *; }

# General Android rules
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
