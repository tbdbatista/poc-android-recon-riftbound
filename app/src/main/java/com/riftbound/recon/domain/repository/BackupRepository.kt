package com.riftbound.recon.domain.repository

import com.riftbound.recon.domain.model.CloudBackupSnapshot
import com.riftbound.recon.domain.model.ConflictResolutionStrategy
import kotlinx.coroutines.flow.StateFlow

interface BackupRepository {
    val backupSnapshots: StateFlow<List<CloudBackupSnapshot>>
    val isOperationInProgress: StateFlow<Boolean>

    suspend fun loadBackupSnapshots(userId: String): Result<List<CloudBackupSnapshot>>
    suspend fun createBackupSnapshot(userId: String, title: String): Result<CloudBackupSnapshot>
    suspend fun restoreBackupSnapshot(userId: String, backupId: String): Result<Unit>
    suspend fun deleteBackupSnapshot(userId: String, backupId: String): Result<Unit>
    suspend fun resolveLoginConflict(userId: String, strategy: ConflictResolutionStrategy): Result<Unit>
    suspend fun checkCloudDataExists(userId: String): Result<Boolean>
}
