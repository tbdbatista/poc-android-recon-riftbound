package com.riftbound.recon.domain.repository

import com.riftbound.recon.domain.model.SyncStatus
import kotlinx.coroutines.flow.StateFlow

interface SyncRepository {
    val syncStatus: StateFlow<SyncStatus>
    val lastSyncTime: StateFlow<Long?>
    
    suspend fun syncAll(userId: String): Result<Unit>
    suspend fun syncCollection(userId: String, collectionId: Long): Result<Unit>
    suspend fun deleteRemoteCollection(userId: String, collectionId: Long): Result<Unit>
}
