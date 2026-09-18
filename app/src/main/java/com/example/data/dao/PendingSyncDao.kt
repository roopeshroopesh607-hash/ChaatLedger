package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.PendingSyncAction
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingSyncDao {
    @Query("SELECT * FROM pending_sync_actions ORDER BY id ASC")
    fun getAllPendingActionsFlow(): Flow<List<PendingSyncAction>>

    @Query("SELECT * FROM pending_sync_actions ORDER BY id ASC")
    suspend fun getAllPendingActions(): List<PendingSyncAction>

    @Query("SELECT COUNT(*) FROM pending_sync_actions")
    fun getPendingCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM pending_sync_actions")
    suspend fun getPendingCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAction(action: PendingSyncAction): Long

    @Query("DELETE FROM pending_sync_actions WHERE id = :id")
    suspend fun deleteActionById(id: Long)

    @Query("DELETE FROM pending_sync_actions WHERE documentId = :documentId AND collection = :collection")
    suspend fun deleteActionsForDoc(collection: String, documentId: String)

    @Query("DELETE FROM pending_sync_actions")
    suspend fun clearAll()
}
