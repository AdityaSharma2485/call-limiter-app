package com.example.calllimiter.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A simple in-memory cache to prevent double-processing of outgoing calls
 * by the ViewModel and the background services.
 */
object CallProcessManager {
    private val recentlyProcessedNumbers = mutableSetOf<String>()
    private val scope = CoroutineScope(Dispatchers.Default)

    fun add(number: String) {
        recentlyProcessedNumbers.add(number)
        // Remove the number after a few seconds to allow for subsequent legitimate checks.
        scope.launch {
            delay(5000) // Keep it in memory for 5 seconds
            remove(number)
        }
    }

    private fun remove(number: String) {
        recentlyProcessedNumbers.remove(number)
    }

    fun isRecentlyProcessed(number: String): Boolean {
        return recentlyProcessedNumbers.contains(number)
    }
}