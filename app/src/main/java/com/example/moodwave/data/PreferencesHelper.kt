package com.example.moodwave.data

import android.content.Context
import android.content.SharedPreferences

object PreferencesHelper {
    private const val PREF_NAME = "moodwave_prefs"
    private const val KEY_LOCK_MODE = "lock_mode" // "PIN", "NONE"
    private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    private const val KEY_PIN_CODE = "pin_code"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_PROFILE_IMAGE = "profile_image"
    private const val KEY_CUSTOM_ACTIVITIES = "custom_activities"
    private const val KEY_DARK_MODE = "dark_mode"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    enum class Appearance(val title: String) { SYSTEM("Sistema"), LIGHT("Claro"), DARK("Oscuro") }

    fun appearance(context: Context): Appearance {
        val prefs = getPrefs(context)
        val previous = if (!prefs.contains(KEY_DARK_MODE)) Appearance.SYSTEM
            else if (prefs.getBoolean(KEY_DARK_MODE, false)) Appearance.DARK else Appearance.LIGHT
        return Appearance.entries.firstOrNull { it.name == prefs.getString("appearance", null) } ?: previous
    }

    fun setAppearance(context: Context, mode: Appearance) {
        getPrefs(context).edit().putString("appearance", mode.name).apply()
    }

    fun getLockMode(context: Context): String {
        return getPrefs(context).getString(KEY_LOCK_MODE, "NONE") ?: "NONE"
    }

    fun setLockMode(context: Context, mode: String) {
        getPrefs(context).edit().putString(KEY_LOCK_MODE, mode).apply()
    }

    fun isBiometricEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BIOMETRIC_ENABLED, true)
    }

    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun getPin(context: Context): String {
        return getPrefs(context).getString(KEY_PIN_CODE, "1234") ?: "1234"
    }

    fun setPin(context: Context, pin: String) {
        getPrefs(context).edit().putString(KEY_PIN_CODE, pin).apply()
    }

    fun getUserName(context: Context): String {
        return getPrefs(context).getString(KEY_USER_NAME, "Hermoso Ser") ?: "Hermoso Ser"
    }

    fun setUserName(context: Context, name: String) {
        getPrefs(context).edit().putString(KEY_USER_NAME, name).apply()
    }

    fun getProfileImage(context: Context): String {
        return getPrefs(context).getString(KEY_PROFILE_IMAGE, "") ?: ""
    }

    fun setProfileImage(context: Context, uri: String) {
        getPrefs(context).edit().putString(KEY_PROFILE_IMAGE, uri).apply()
    }

    fun getCustomActivities(context: Context): List<String> {
        val saved = getPrefs(context).getString(KEY_CUSTOM_ACTIVITIES, "") ?: ""
        return if (saved.isBlank()) emptyList() else saved.split(",").map { it.trim() }
    }

    fun addCustomActivity(context: Context, activity: String) {
        val current = getCustomActivities(context).toMutableList()
        if (!current.contains(activity)) {
            current.add(activity)
            getPrefs(context).edit().putString(KEY_CUSTOM_ACTIVITIES, current.joinToString(",")).apply()
        }
    }
}
