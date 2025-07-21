package com.example.calllimiter.service

import android.content.Intent
import android.net.Uri
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import android.widget.Toast
import com.example.calllimiter.data.SettingsRepository
import com.example.calllimiter.domain.CallLimiterUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

private const val LIMITER_TAG = "LIMITER_SCREENING"

@AndroidEntryPoint
class MyCallScreeningService : CallScreeningService() {

    @Inject
    lateinit var callLimiterUseCase: CallLimiterUseCase

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onScreenCall(callDetails: Call.Details) {
        serviceScope.launch {
            val number = callDetails.handle?.schemeSpecificPart
            val response = CallResponse.Builder()

            if (number != null && callDetails.callDirection == Call.Details.DIRECTION_OUTGOING) {

                // **CHECK IF ALREADY PROCESSED**
                if (CallProcessManager.isRecentlyProcessed(number)) {
                    Log.d(LIMITER_TAG, "Call to $number already processed by ViewModel. Allowing.")
                    respondToCall(callDetails, response.build()) // Allow call without further checks
                    return@launch
                }

                // If not processed by ViewModel, run the full logic
                val callResult = callLimiterUseCase.checkCallPermission(number)
                Log.d(LIMITER_TAG, "Screening result for $number: canCall=${callResult.canCall}, shouldRedirect=${callResult.shouldRedirect}, reason=${callResult.reason}, attempt=${callResult.attemptNumber}")

                when {
                    callResult.canCall -> {
                        callLimiterUseCase.logCall(number, success = true)
                    }
                    callResult.shouldRedirect -> {
                        response.setDisallowCall(true)
                        response.setRejectCall(true)
                        callLimiterUseCase.logCall(number, success = false)
                        val helperNumber = settingsRepository.getRedirectNumber()
                        if (helperNumber.isNotBlank()) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(applicationContext, "Redirecting to ...", Toast.LENGTH_LONG).show()
                            }
                            callLimiterUseCase.logRedirectedCall(number, helperNumber)
                            redirectToHelperNumber(helperNumber)
                        }
                    }
                    else -> {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(applicationContext, "Please wait for some time before calling again.", Toast.LENGTH_LONG).show()
                        }
                        response.setDisallowCall(true)
                        response.setRejectCall(true)
                        callLimiterUseCase.logCall(number, success = false)
                    }
                }
            }

            respondToCall(callDetails, response.build())
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
}