package com.example.calllimiter.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.example.calllimiter.di.CallLimiterEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.*

class OutgoingCallReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_NEW_OUTGOING_CALL) return

        val rawNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER) ?: return
        val numberToCheck = normalizeNumber(rawNumber)
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            CallLimiterEntryPoint::class.java
        )
        val callLimiterUseCase = entryPoint.getCallLimiterUseCase()
        val settingsRepository = entryPoint.getSettingsRepository()
        val pendingResult = goAsync()

        scope.launch {
            try {
                val result = callLimiterUseCase.checkCallPermission(numberToCheck)
                when {
                    result.canCall -> {
                        pendingResult.resultData = rawNumber
                        callLimiterUseCase.logCall(numberToCheck, success = true)
                    }
                    result.shouldRedirect -> {
                        pendingResult.resultData = null
                        callLimiterUseCase.logCall(numberToCheck, success = false)
                        val helperNumber = settingsRepository.getRedirectNumber()
                        if (helperNumber.isNotBlank()) {
                            callLimiterUseCase.logRedirectedCall(numberToCheck, helperNumber)
                            // IGNORE result.reason and show the correct hardcoded toast
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Redirecting to ...", Toast.LENGTH_LONG).show()
                            }
                            val redirectIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$helperNumber")).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(redirectIntent)
                        }
                    }
                    else -> {
                        // IGNORE result.reason and show the correct hardcoded toast
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Please wait for some time before calling again.", Toast.LENGTH_LONG).show()
                        }
                        pendingResult.resultData = null
                        callLimiterUseCase.logCall(numberToCheck, success = false)
                    }
                }
            } catch (e: Exception) {
                Log.e("OutgoingCallReceiver", "Error in call limiting logic", e)
                pendingResult.resultData = rawNumber
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun normalizeNumber(number: String): String {
        val digitsOnly = number.replace(Regex("[^0-9]"), "")
        return when {
            digitsOnly.startsWith("91") && digitsOnly.length > 10 -> digitsOnly.substring(2)
            digitsOnly.length > 10 -> digitsOnly.takeLast(10)
            else -> digitsOnly
        }
    }
}