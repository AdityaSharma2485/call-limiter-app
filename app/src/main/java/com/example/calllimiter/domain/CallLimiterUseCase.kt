package com.example.calllimiter.domain

import android.provider.CallLog.Calls
import android.util.Log
import com.example.calllimiter.data.AppDao
import com.example.calllimiter.data.CallLog
import com.example.calllimiter.data.ContactRule
import com.example.calllimiter.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CallLimiterUseCase @Inject constructor(
    private val dao: AppDao,
    private val settingsRepository: SettingsRepository
) {

    data class CallResult(
        val canCall: Boolean,
        val shouldRedirect: Boolean = false,
        val reason: String = "",
        val attemptNumber: Int = 0 // Track which attempt this is
    )

    private fun normalizeNumber(number: String): String {
        // Remove all non-digits first
        val digitsOnly = number.replace(Regex("[^0-9]"), "")

        // Handle different formats
        return when {
            digitsOnly.startsWith("91") && digitsOnly.length > 10 -> digitsOnly.substring(2)
            digitsOnly.length > 10 -> digitsOnly.takeLast(10)
            else -> digitsOnly
        }
    }

    suspend fun checkCallPermission(number: String): CallResult = withContext(Dispatchers.IO) {
        val normalized = normalizeNumber(number)
        Log.d("LIMITER", "Checking permission for original: '$number', normalized: '$normalized'")

        val rule: ContactRule? = dao.getRuleByNumber(normalized)

        if (rule == null || !rule.isManaged) {
            Log.d("LIMITER", "No rule or not managed - allowing call")
            return@withContext CallResult(canCall = true, reason = "Not managed")
        }

        val now = System.currentTimeMillis()
        val windowStart = now - rule.timeWindowHours * 60 * 60 * 1000

        val successfulCalls = dao.getRecentCallCount(normalized, windowStart)
        val blockedCalls = dao.getRecentBlockedCallCount(normalized, windowStart)
        val totalAttempts = successfulCalls + blockedCalls
        val nextAttempt = totalAttempts + 1

        Log.d("LIMITER", "Recent calls - Successful: $successfulCalls, Blocked: $blockedCalls, Total: $totalAttempts, Next: $nextAttempt, Limit: ${rule.callLimit}")

        // --- FINAL CORRECTED LOGIC ---
        when {
            // 1. Allow if successful calls are less than the limit.
            //    (For limit=2, this allows call #1 [0<2] and call #2 [1<2])
            successfulCalls < rule.callLimit -> {
                Log.d("LIMITER", "Call allowed - under limit ($successfulCalls/${rule.callLimit})")
                CallResult(canCall = true, reason = "Under limit", attemptNumber = nextAttempt)
            }
            // 2. Block if it's the 3rd or 4th attempt.
            //    The total attempts so far will be 2 or 3.
            totalAttempts < rule.callLimit + 2 -> {
                Log.d("LIMITER", "Call blocked - attempt $nextAttempt")
                CallResult(canCall = false, reason = "Over limit, blocking", attemptNumber = nextAttempt)
            }
            // 3. Redirect on the 5th attempt and beyond.
            //    The total attempts so far will be 4 or more.
            else -> {
                Log.d("LIMITER", "Call blocked with redirection - attempt $nextAttempt")
                CallResult(
                    canCall = false,
                    shouldRedirect = settingsRepository.isRedirectEnabled(),
                    reason = "Redirecting to helper",
                    attemptNumber = nextAttempt
                )
            }
        }
    }

    suspend fun logCall(number: String, success: Boolean, callType: Int = Calls.OUTGOING_TYPE) = withContext(Dispatchers.IO) {
        val normalized = normalizeNumber(number)
        val now = System.currentTimeMillis()

        val phoneNumberToLog = if (success) normalized else "blocked_$normalized"
        val logType = when {
            success -> callType
            else -> CallLog.TYPE_BLOCKED
        }

        val log = CallLog(
            phoneNumber = phoneNumberToLog,
            timestamp = now,
            type = logType
        )

        dao.insertCallLog(log)
        Log.d("LIMITER", "Logged call to $normalized (success=$success, type=$logType)")
    }

    suspend fun logRedirectedCall(originalNumber: String, helperNumber: String) = withContext(Dispatchers.IO) {
        val normalized = normalizeNumber(originalNumber)
        val normalizedHelper = normalizeNumber(helperNumber)
        val now = System.currentTimeMillis()

        val log = CallLog(
            phoneNumber = "redirected_${normalized}_to_$normalizedHelper",
            timestamp = now,
            type = CallLog.TYPE_REDIRECTED
        )

        dao.insertCallLog(log)
        Log.d("LIMITER", "Logged redirected call from $normalized to $normalizedHelper")
    }

    suspend fun canCall(number: String): Boolean {
        return checkCallPermission(number).canCall
    }
}