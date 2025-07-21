package com.example.calllimiter.ui.viewmodels

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calllimiter.data.SettingsRepository
import com.example.calllimiter.domain.CallLimiterUseCase
import com.example.calllimiter.service.CallProcessManager // Keep this import
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CallHistoryViewModel @Inject constructor(
    private val limiter: CallLimiterUseCase,
    private val settingsRepository: SettingsRepository,
    application: Application
) : AndroidViewModel(application) {

    private val context = application.applicationContext

    fun placeCallFromHistory(rawNumber: String) {
        viewModelScope.launch {
            val cleaned = rawNumber.trim()
            if (cleaned.isEmpty()) return@launch

            val result = limiter.checkCallPermission(cleaned)

            when {
                // --- ALLOW a call ---
                result.canCall -> {
                    limiter.logCall(cleaned, success = true)
                    // Mark as processed so the service ignores it
                    CallProcessManager.add(cleaned)
                    launchCall(cleaned)
                }

                // --- REDIRECT a call ---
                result.shouldRedirect -> {
                    limiter.logCall(cleaned, success = false)
                    val helperNumber = settingsRepository.getRedirectNumber()
                    if (helperNumber.isNotBlank()) {
                        limiter.logRedirectedCall(cleaned, helperNumber)
                        // Mark the HELPER number as processed so the service ignores it
                        CallProcessManager.add(helperNumber)
                        launchCall(helperNumber)
                    } else {
                        Toast.makeText(context, "Call limit reached. Please set helper number in settings.", Toast.LENGTH_LONG).show()
                    }
                }

                // --- BLOCK a call ---
                else -> {
                    // Log the blocked attempt
                    limiter.logCall(cleaned, success = false)
                    // Show the "Please wait" toast directly from here
                    Toast.makeText(context, "Please wait for some time before calling again.", Toast.LENGTH_SHORT).show()
                    // **Crucially, DO NOT launch the call.** This is what effectively blocks it.
                }
            }
        }
    }

    private fun launchCall(number: String) {
        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            ContextCompat.startActivity(context, intent, null)
        } catch (e: SecurityException) {
            Toast.makeText(context, "Permission to make calls is not granted.", Toast.LENGTH_SHORT).show()
        }
    }
}