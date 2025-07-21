package com.example.calllimiter.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a contact and any associated rules.
 *
 * @property phoneNumber The contact's phone number, used as the primary key.
 * @property name The contact's display name.
 * @property isManaged Whether a call limit rule is active for this contact.
 * @property callLimit The number of calls allowed within the time window.
 * @property timeWindowHours The time window for the call limit in hours.
 * @property type A field to satisfy UI requirements, can be used for contact type in the future.
 * @property notes Additional notes for the contact rule.
 */
@Entity(tableName = "contact_rules")
data class ContactRule(
    @PrimaryKey
    val phoneNumber: String,
    val name: String,
    val isManaged: Boolean = false,
    val callLimit: Int = 2,
    val timeWindowHours: Int = 1,
    val type: Int = 0,
    val notes: String? = null
)