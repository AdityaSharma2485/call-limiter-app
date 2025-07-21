package com.example.calllimiter.ui.viewmodels

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.calllimiter.data.AppDao
import com.example.calllimiter.data.ContactRule
import com.example.calllimiter.service.CallLimiterService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: AppDao
) : ViewModel() {

    val searchQuery = MutableStateFlow("")

    // This directly consumes the Flow from the DAO, making the UI reactive
    private val contactRules: StateFlow<List<ContactRule>> =
        dao.getAllRulesAsFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredContacts = combine(contactRules, searchQuery) { contacts, query ->
        if (query.isBlank()) {
            contacts
        } else {
            contacts.filter {
                it.name.contains(query, ignoreCase = true) || it.phoneNumber.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    private fun normalizeNumber(number: String): String {
        return number.replace(Regex("[^0-9]"), "").takeLast(10)
    }

    fun syncContacts(resolver: ContentResolver) {
        viewModelScope.launch(Dispatchers.IO) {
            val existingRules = dao.getAllRulesSync().associateBy { it.phoneNumber }
            val deviceContacts = mutableMapOf<String, String>()
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            resolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection, null, null,
                null
            )?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameIndex) ?: "Unknown"
                    val rawNumber = cursor.getString(numberIndex) ?: continue
                    val normalized = normalizeNumber(rawNumber)
                    if (normalized.isNotBlank()) {
                        deviceContacts[normalized] = name
                    }
                }
            }

            val finalRules = deviceContacts.map { (number, name) ->
                val existingRule = existingRules[number]
                existingRule?.copy(name = name) ?: ContactRule(phoneNumber = number, name = name)
            }
            dao.insertOrUpdateAllRules(finalRules)
        }
    }

    // This function is now correctly named and used by the UI
    fun updateContactRule(rule: ContactRule) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateRule(rule)
        }
    }

    fun startService() {
        val intent = Intent(context, CallLimiterService::class.java)
        ContextCompat.startForegroundService(context, intent)
    }

    fun stopService() {
        context.stopService(Intent(context, CallLimiterService::class.java))
    }

    fun setAllContactsManaged(isManaged: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.setAllManagedStatus(isManaged)
        }
    }
}