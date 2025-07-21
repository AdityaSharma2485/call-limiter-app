package com.example.calllimiter.service

import android.os.Build
import android.telecom.Call
import androidx.annotation.RequiresApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A singleton object to manage the state of an ongoing call.
 *
 * This object holds the active call and its state, allowing different components
 * of the app (like the InCallService and the InCallActivity) to observe
 * and interact with the call.
 */
@RequiresApi(Build.VERSION_CODES.M)
object CallManager {

    private val _call = MutableStateFlow<Call?>(null)
    val call = _call.asStateFlow()

    private val _callState = MutableStateFlow<Int?>(null)
    val callState = _callState.asStateFlow()

    // This callback will be registered with the call to listen for state changes.
    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call?, state: Int) {
            _callState.value = state
        }
    }

    fun onCallAdded(newCall: Call) {
        _call.value = newCall
        _callState.value = newCall.state
        newCall.registerCallback(callCallback)
    }

    fun onCallRemoved(removedCall: Call) {
        // Ensure we are cleaning up the correct call.
        if (_call.value == removedCall) {
            _call.value = null
            _callState.value = null
            removedCall.unregisterCallback(callCallback)
        }
    }

    fun hangup() {
        _call.value?.disconnect()
    }
}
