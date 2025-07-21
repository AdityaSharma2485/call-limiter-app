package com.example.calllimiter.ui.viewmodels

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.calllimiter.data.AppDao
import com.example.calllimiter.data.ContactRule
import com.example.calllimiter.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dao: AppDao,
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val rules: Flow<List<ContactRule>> = dao.getAllRulesAsFlow().flowOn(Dispatchers.IO)

    private val _redirectEnabled = MutableStateFlow(false)
    val redirectEnabled = _redirectEnabled.asStateFlow()

    private val _redirectNumber = MutableStateFlow("")
    val redirectNumber = _redirectNumber.asStateFlow()

    init {
        viewModelScope.launch {
            _redirectEnabled.value = settingsRepository.isRedirectEnabled()
            _redirectNumber.value = settingsRepository.getRedirectNumber()
        }
    }

    fun setRedirectEnabled(isEnabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.setRedirectEnabled(isEnabled)
            _redirectEnabled.value = isEnabled
            Log.d("SettingsViewModel", "Redirect enabled set to: $isEnabled")
        }
    }

    // Called on every key press to update the text field
    fun onRedirectNumberChanged(number: String) {
        _redirectNumber.value = number
    }

    // Called when the 'Set as Default Dialer' button is pressed
    fun saveRedirectNumber() {
        viewModelScope.launch(Dispatchers.IO) {
            val numberToSave = _redirectNumber.value
            if (numberToSave.isNotBlank()) {
                settingsRepository.setRedirectNumber(numberToSave)
                Log.d("SettingsViewModel", "Redirect number SAVED: $numberToSave")
                showHelperNumberSavedToast() // Show toast only after saving
            }
        }
    }

    private fun showHelperNumberSavedToast() {
        viewModelScope.launch(Dispatchers.Main) {
            Toast.makeText(context, "Helper number saved successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    fun setAllManagedStatus(isManaged: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            Log.d("SettingsViewModel", "Setting all rules managed status to: $isManaged")
            dao.setAllManagedStatus(isManaged)
        }
    }

    fun save(rule: ContactRule) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertOrUpdateRule(rule)
        }
    }
}