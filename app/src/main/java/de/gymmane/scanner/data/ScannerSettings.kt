package de.gymmane.scanner.data

import android.content.Context

interface AutoSendSettings { var autoSend: Boolean }

class ScannerSettings(context: Context) : AutoSendSettings {
    private val preferences = context.applicationContext.getSharedPreferences("scanner-settings", Context.MODE_PRIVATE)
    var language: String
        get() = preferences.getString("language", "de")?.takeIf { it in listOf("de", "en") } ?: "de"
        set(value) { require(value in listOf("de", "en")); preferences.edit().putString("language", value).apply() }
    var onboardingComplete: Boolean
        get() = preferences.getBoolean("onboarding-complete", false)
        set(value) { preferences.edit().putBoolean("onboarding-complete", value).apply() }
    override var autoSend: Boolean
        get() = preferences.getBoolean("auto-send-gymmane", false)
        set(value) { preferences.edit().putBoolean("auto-send-gymmane", value).apply() }
}
