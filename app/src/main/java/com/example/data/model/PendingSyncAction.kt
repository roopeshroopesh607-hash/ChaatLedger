package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_sync_actions")
data class PendingSyncAction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val collection: String, // "sales" or "expenses"
    val actionType: String, // "UPSERT" or "DELETE"
    val documentId: String,
    val payloadJson: String = "", // Serialized data for upsert
    val timestamp: Long = System.currentTimeMillis()
)

object SyncActionType {
    const val UPSERT = "UPSERT"
    const val DELETE = "DELETE"
}

object SyncCollection {
    const val SALES = "sales"
    const val EXPENSES = "expenses"
}
