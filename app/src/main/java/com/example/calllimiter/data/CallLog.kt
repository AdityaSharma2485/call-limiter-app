package com.example.calllimiter.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import android.provider.CallLog as AndroidCallLog

/**
 * Represents a single entry in the call log database.
 */
@Entity(tableName = "call_logs")
data class CallLog(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val phoneNumber: String,
    val timestamp: Long,
    val type: Int
) {
    companion object {
        // Define our custom blocked type since Android doesn't have one
        const val TYPE_BLOCKED = 100
        const val TYPE_REDIRECTED = 101
    }
}