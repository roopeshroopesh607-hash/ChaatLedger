package com.example.data.sync

sealed interface SyncStatus {
    data object OnlineSynced : SyncStatus
    data class Syncing(val pendingCount: Int = 0) : SyncStatus
    data class OfflinePending(val pendingCount: Int) : SyncStatus
    data object OfflineSynced : SyncStatus
}
