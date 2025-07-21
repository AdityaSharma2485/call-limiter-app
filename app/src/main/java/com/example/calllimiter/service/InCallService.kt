package com.example.calllimiter.service

import android.content.Intent
import android.net.Uri
import android.telecom.Call
import android.telecom.InCallService
import android.util.Log
import android.widget.Toast
import com.example.calllimiter.data.SettingsRepository
import com.example.calllimiter.domain.CallLimiterUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

private const val TAG = "MyInCallService"
private const val LIMITER_TAG = "LIMITER"

@AndroidEntryPoint
class InCallService : InCallService() {

    @Inject
    lateinit var callLimiterUseCase: CallLimiterUseCase

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        val number = call.details.handle?.schemeSpecificPart ?: return

        serviceScope.launch {
            val result = callLimiterUseCase.checkCallPermission(number)
            when {
                result.canCall -> {
                    callLimiterUseCase.logCall(number, success = true)
                }
                result.shouldRedirect -> {
                    call.disconnect()
                    callLimiterUseCase.logCall(number, success = false)
                    val helperNumber = settingsRepository.getRedirectNumber()
                    if (helperNumber.isNotBlank()) {
                        // IGNORE result.reason and show the correct hardcoded toast
                        withContext(Dispatchers.Main) {
                            Toast.makeText(applicationContext, "Redirecting to ...", Toast.LENGTH_LONG).show()
                        }
                        callLimiterUseCase.logRedirectedCall(number, helperNumber)
                        redirectToHelperNumber(helperNumber)
                    }
                }
                else -> {
                    // IGNORE result.reason and show the correct hardcoded toast
                    withContext(Dispatchers.Main) {
                        Toast.makeText(applicationContext, "Please wait for some time before calling again.", Toast.LENGTH_LONG).show()
                    }
                    call.disconnect()
                    callLimiterUseCase.logCall(number, success = false)
                }
            }
        }
    }

    private fun redirectToHelperNumber(helperNumber: String) {
        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$helperNumber")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            startActivity(intent)
        } catch (e: SecurityException) {
            Log.e(LIMITER_TAG, "Permission denied for redirection.", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}