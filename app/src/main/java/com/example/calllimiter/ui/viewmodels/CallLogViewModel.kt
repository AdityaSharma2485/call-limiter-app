package com.example.calllimiter.ui.viewmodels

import android.app.Application
import android.provider.CallLog
import android.provider.ContactsContract
import android.telephony.PhoneNumberUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calllimiter.data.AppDao
import com.example.calllimiter.data.CallLog as AppCallLog // Alias our custom CallLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class CallLogViewModel @Inject constructor(
    application: Application,
    private val dao: AppDao
) : AndroidViewModel(application) {

    data class CallLogDisplay(
        val phoneNumber: String, // Keep this as the display number
        val originalNumber: String = phoneNumber, // Add this to store the original
        val callerName: String?,
        val timestamp: Long,
        val type: Int,
        val isAppManaged: Boolean = false
    )

    private val _allCombinedCallLogs = MutableStateFlow<List<CallLogDisplay>>(emptyList())
    val searchQuery = MutableStateFlow("")

    val filteredCallLogs = combine(_allCombinedCallLogs, searchQuery) { logs, query ->
        if (query.isBlank()) logs
        else logs.filter {
            it.phoneNumber.contains(query, ignoreCase = true) ||
                    it.callerName?.contains(query, ignoreCase = true) == true
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadCallLogs() = viewModelScope.launch {
        val resolver = getApplication<Application>().contentResolver

        val (systemLogs, contactNameMap) = withContext(Dispatchers.IO) {
            // 1. Load system call logs
            val systemLogs = mutableListOf<CallLogDisplay>()
            val projection = arrayOf(
                CallLog.Calls.NUMBER,
                CallLog.Calls.DATE,
                CallLog.Calls.TYPE
            )
            val sortOrder = "${CallLog.Calls.DATE} DESC"
            val limitedUri = CallLog.Calls.CONTENT_URI.buildUpon()
                .appendQueryParameter("limit", "200")
                .build()

            resolver.query(
                limitedUri,
                projection,
                null,
                null,
                sortOrder
            )?.use { c ->
                val numberIdx = c.getColumnIndex(CallLog.Calls.NUMBER)
                val dateIdx = c.getColumnIndex(CallLog.Calls.DATE)
                val typeIdx = c.getColumnIndex(CallLog.Calls.TYPE)

                while (c.moveToNext()) {
                    val rawNumber = c.getString(numberIdx) ?: ""
                    systemLogs += CallLogDisplay(
                        phoneNumber = rawNumber,
                        originalNumber = rawNumber, // Store original for calling
                        callerName = null,
                        timestamp = c.getLong(dateIdx),
                        type = c.getInt(typeIdx),
                        isAppManaged = false
                    )
                }
            }

            // 2. Build contact name map
            val nameMap = mutableMapOf<String, String?>()
            if (systemLogs.isNotEmpty()) {
                val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                val contactProjection = arrayOf(
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                )
                resolver.query(uri, contactProjection, null, null, null)?.use { c ->
                    val numberIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val nameIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    while (c.moveToNext()) {
                        val rawNumber = c.getString(numberIdx) ?: continue
                        val displayName = c.getString(nameIdx)
                        nameMap[rawNumber] = displayName
                    }
                }
            }
            systemLogs to nameMap
        }

        // 3. Load app's custom call logs
        val appLogs = withContext(Dispatchers.IO) {
            dao.getAllCallLogsSync().map { appLog ->
                val originalNum = when {
                    appLog.phoneNumber.startsWith("blocked_") ->
                        appLog.phoneNumber.removePrefix("blocked_")
                    appLog.phoneNumber.startsWith("redirected_") ->
                        appLog.phoneNumber.removePrefix("redirected_").substringBefore("_to_")
                    else -> appLog.phoneNumber
                }

                CallLogDisplay(
                    phoneNumber = appLog.phoneNumber,
                    originalNumber = originalNum, // Extract original number
                    callerName = null,
                    timestamp = appLog.timestamp,
                    type = appLog.type,
                    isAppManaged = true
                )
            }
        }

        // 4. Combine and deduplicate logs
        val combinedLogs = mutableListOf<CallLogDisplay>()
        val processedNumbers = mutableSetOf<String>() // To help with deduplication based on type and number

        // Add app-managed logs first, as they have priority for our custom types
        appLogs.forEach { appLog ->
            combinedLogs.add(appLog)
            // Store the normalized number to avoid adding system logs for the same number if our app managed it
            processedNumbers.add(normalizeNumberForDeduplication(appLog.phoneNumber))
        }

        // Add system logs, but only if we haven't already added a custom log for that number
        systemLogs.forEach { systemLog ->
            if (!processedNumbers.contains(normalizeNumberForDeduplication(systemLog.phoneNumber))) {
                combinedLogs.add(systemLog)
            }
        }

        // 5. Resolve caller names for all combined logs
        // Also get names from our local ContactRules as a fallback
        val rules = dao.getAllRulesSync()
        val ruleNameMap = rules.associate { it.phoneNumber to it.name }

        _allCombinedCallLogs.value = combinedLogs.map { log ->
            val nameFromContacts = contactNameMap.entries.firstOrNull {
                PhoneNumberUtils.compare(it.key, log.phoneNumber)
            }?.value

            val nameFromRules = ruleNameMap.entries.firstOrNull {
                PhoneNumberUtils.compare(it.key, log.phoneNumber)
            }?.value

            // Prioritize system contact name, then local rule name, then app-managed tag
            val finalName = nameFromContacts ?: nameFromRules ?:
            if (log.isAppManaged) {
                when (log.type) {
                    AppCallLog.TYPE_BLOCKED -> "Blocked Call"
                    AppCallLog.TYPE_REDIRECTED -> "Redirected Call"
                    else -> log.phoneNumber // Fallback for app-managed if no name found
                }
            } else {
                log.phoneNumber
            }

            log.copy(callerName = finalName)
        }.sortedByDescending { it.timestamp } // Sort all logs by timestamp
    }

    // Helper to normalize numbers for deduplication, considering "blocked_" or "redirected_" prefixes
    private fun normalizeNumberForDeduplication(number: String): String {
        return normalizeNumber(number)
            .removePrefix("blocked_")
            .removePrefix("redirected_")
            .substringAfterLast("_to_") // For redirected_original_to_helper
    }

    // Existing normalizeNumber, ensure it's robust
    private fun normalizeNumber(number: String): String {
        // More robust normalization
        val cleaned = number.replace(Regex("[^0-9+]"), "")

        // Handle different number formats
        return when {
            cleaned.startsWith("+91") -> cleaned.substring(3).takeLast(10)
            cleaned.startsWith("91") && cleaned.length > 10 -> cleaned.substring(2).takeLast(10)
            cleaned.length > 10 -> cleaned.takeLast(10)
            else -> cleaned
        }
    }
}
