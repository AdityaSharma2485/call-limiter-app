package com.example.calllimiter.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    companion object {
        const val KEY_REDIRECT_ENABLED = "redirect_enabled"
        const val KEY_REDIRECT_NUMBER = "redirect_number"
        // Removed KEY_REDIRECT_ATTEMPTS as it's not directly used in the CallLimiterUseCase logic for redirection attempt count.
    }

    fun setRedirectEnabled(isEnabled: Boolean) {
        prefs.edit().putBoolean(KEY_REDIRECT_ENABLED, isEnabled).apply()
    }

    fun isRedirectEnabled(): Boolean {
        return prefs.getBoolean(KEY_REDIRECT_ENABLED, false)
    }

    fun setRedirectNumber(number: String) {
        prefs.edit().putString(KEY_REDIRECT_NUMBER, number).apply()
    }

    fun getRedirectNumber(): String {
        return prefs.getString(KEY_REDIRECT_NUMBER, "") ?: ""
    }

    // Removed setRedirectAttempts and getRedirectAttempts as KEY_REDIRECT_ATTEMPTS is no longer used.
}
