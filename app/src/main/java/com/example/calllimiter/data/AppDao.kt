package com.example.calllimiter.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Update
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    // --- ContactRule Queries ---

    @Query("SELECT * FROM contact_rules ORDER BY name ASC")
    fun getAllRulesAsFlow(): Flow<List<ContactRule>>

    @Query("SELECT * FROM contact_rules WHERE phoneNumber = :phoneNumber LIMIT 1")
    suspend fun getRuleByNumber(phoneNumber: String): ContactRule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateRule(rule: ContactRule)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAllRules(rules: List<ContactRule>)

    @Delete
    suspend fun deleteRule(rule: ContactRule)

    @Update
    suspend fun updateRule(rule: ContactRule)

    @Query("SELECT * FROM contact_rules")
    suspend fun getAllRulesSync(): List<ContactRule>

    @Query("UPDATE contact_rules SET isManaged = :isManaged")
    suspend fun setAllManagedStatus(isManaged: Boolean)

    // --- CallLog Queries ---

    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC")
    fun getAllCallLogs(): Flow<List<CallLog>>

    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC")
    suspend fun getAllCallLogsSync(): List<CallLog>

    @Query("SELECT COUNT(*) FROM call_logs WHERE phoneNumber = :phoneNumber AND timestamp >= :sinceTimestamp")
    suspend fun getRecentCallCount(phoneNumber: String, sinceTimestamp: Long): Int

    // Fixed: Count blocked calls with exact prefix match
    @Query("SELECT COUNT(*) FROM call_logs WHERE phoneNumber = 'blocked_' || :phoneNumber AND timestamp >= :sinceTimestamp")
    suspend fun getRecentBlockedCallCount(phoneNumber: String, sinceTimestamp: Long): Int

    @Insert
    suspend fun insertCallLog(log: CallLog)

    @Query("DELETE FROM call_logs WHERE id = :logId")
    suspend fun deleteCallLog(logId: Int)
}