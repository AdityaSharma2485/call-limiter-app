package com.example.calllimiter.ui.viewmodels

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calllimiter.data.SettingsRepository
import com.example.calllimiter.domain.CallLimiterUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DialerViewModel @Inject constructor(
    private val limiter: CallLimiterUseCase,
    private val settingsRepository: SettingsRepository,
    application: Application
) : AndroidViewModel(application) {

    private val context = application.applicationContext

    fun placeCall(rawNumber: String) {
        viewModelScope.launch {
            val cleaned = rawNumber.trim()
            if (cleaned.isEmpty()) return@launch

            val result = limiter.checkCallPermission(cleaned)

            when {
                result.canCall -> {
                    // Allow the call
                    limiter.logCall(cleaned, success = true)
                    launchCall(cleaned)
                }
                result.shouldRedirect -> {
                    // Block and redirect
                    limiter.logCall(cleaned, success = false)
                    val helperNumber = settingsRepository.getRedirectNumber()
                    if (helperNumber.isNotBlank()) {
                        Toast.makeText(context, "Redirecting to helper...", Toast.LENGTH_SHORT).show()
                        limiter.logRedirectedCall(cleaned, helperNumber)
                        launchCall(helperNumber)
                    } else {
                        Toast.makeText(context, "Call limit reached. Please set helper number in settings.", Toast.LENGTH_LONG).show()
                    }
                }
                else -> {
                    // Block the call
                    limiter.logCall(cleaned, success = false)
                    Toast.makeText(context, "Call blocked: ${result.reason}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun launchCall(number: String) {
        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(context, intent, null)
    }
}