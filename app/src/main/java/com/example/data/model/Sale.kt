package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Daily aggregate sale entity for ChaatLedger.
 * Tracks total cash and paytm collection for each business day.
 */
@Entity(
    tableName = "sales",
    indices = [Index(value = ["date"], unique = true)]
)
data class Sale(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: Long, // Business day timestamp (normalized to start-of-day millis)
    val cashTotal: Double,
    val paytmTotal: Double,
    val totalAmount: Double = cashTotal + paytmTotal, // Auto-calculated total
    val timestamp: Long = System.currentTimeMillis(), // When entry was created/edited
    val enteredBy: String = "",
    val firestoreId: String = ""
)

