package com.riftbound.recon.data.backup

import com.riftbound.recon.domain.model.CloudBackupSnapshot
import com.riftbound.recon.domain.model.CloudCardDto
import com.riftbound.recon.domain.model.CloudCollectionDto
import com.riftbound.recon.domain.model.ConflictResolutionStrategy
import com.riftbound.recon.domain.repository.BackupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class FakeBackupRepository : BackupRepository {
    private val _backupSnapshots = MutableStateFlow<List<CloudBackupSnapshot>>(emptyList())
    override val backupSnapshots: StateFlow<List<CloudBackupSnapshot>> = _backupSnapshots.asStateFlow()

    private val _isOperationInProgress = MutableStateFlow(false)
    override val isOperationInProgress: StateFlow<Boolean> = _isOperationInProgress.asStateFlow()

    val snapshotsMap = mutableMapOf<String, CloudBackupSnapshot>()
    var lastAppliedStrategy: ConflictResolutionStrategy? = null
    var restoredBackupId: String? = null

    override suspend fun loadBackupSnapshots(userId: String): Result<List<CloudBackupSnapshot>> {
        val list = snapshotsMap.values.sortedByDescending { it.timestamp }
        _backupSnapshots.value = list
        return Result.success(list)
    }

    override suspend fun createBackupSnapshot(userId: String, title: String): Result<CloudBackupSnapshot> {
        val id = UUID.randomUUID().toString()
        val snapshot = CloudBackupSnapshot(
            id = id,
            title = title,
            timestamp = System.currentTimeMillis(),
            collectionsCount = 2,
            totalCardsCount = 60,
            collections = listOf(
                CloudCollectionDto(id = 1L, name = "Main Deck", cards = listOf(CloudCardDto(101, 1)))
            )
        )
        snapshotsMap[id] = snapshot
        loadBackupSnapshots(userId)
        return Result.success(snapshot)
    }

    override suspend fun restoreBackupSnapshot(userId: String, backupId: String): Result<Unit> {
        if (!snapshotsMap.containsKey(backupId)) {
            return Result.failure(IllegalStateException("Backup não encontrado"))
        }
        restoredBackupId = backupId
        return Result.success(Unit)
    }

    override suspend fun deleteBackupSnapshot(userId: String, backupId: String): Result<Unit> {
        snapshotsMap.remove(backupId)
        loadBackupSnapshots(userId)
        return Result.success(Unit)
    }

    override suspend fun resolveLoginConflict(userId: String, strategy: ConflictResolutionStrategy): Result<Unit> {
        lastAppliedStrategy = strategy
        return Result.success(Unit)
    }

    override suspend fun checkCloudDataExists(userId: String): Result<Boolean> {
        return Result.success(snapshotsMap.isNotEmpty())
    }
}

class BackupRepositoryTest {

    private lateinit var fakeBackupRepository: FakeBackupRepository

    @Before
    fun setUp() {
        fakeBackupRepository = FakeBackupRepository()
    }

    @Test
    fun `createBackupSnapshot adds snapshot to repository`() = runTest {
        val result = fakeBackupRepository.createBackupSnapshot("user1", "Backup Torneio")
        assertTrue(result.isSuccess)
        val snapshot = result.getOrThrow()
        assertEquals("Backup Torneio", snapshot.title)
        assertEquals(1, fakeBackupRepository.backupSnapshots.value.size)
    }

    @Test
    fun `restoreBackupSnapshot applies snapshot destructively`() = runTest {
        val createResult = fakeBackupRepository.createBackupSnapshot("user1", "Snapshot Antigo")
        val snapshotId = createResult.getOrThrow().id

        val restoreResult = fakeBackupRepository.restoreBackupSnapshot("user1", snapshotId)
        assertTrue(restoreResult.isSuccess)
        assertEquals(snapshotId, fakeBackupRepository.restoredBackupId)
    }

    @Test
    fun `deleteBackupSnapshot removes snapshot successfully`() = runTest {
        val createResult = fakeBackupRepository.createBackupSnapshot("user1", "Backup Temporario")
        val snapshotId = createResult.getOrThrow().id
        assertEquals(1, fakeBackupRepository.backupSnapshots.value.size)

        val deleteResult = fakeBackupRepository.deleteBackupSnapshot("user1", snapshotId)
        assertTrue(deleteResult.isSuccess)
        assertEquals(0, fakeBackupRepository.backupSnapshots.value.size)
    }

    @Test
    fun `resolveLoginConflict sets conflict resolution strategy`() = runTest {
        val result = fakeBackupRepository.resolveLoginConflict("user1", ConflictResolutionStrategy.KEEP_CLOUD)
        assertTrue(result.isSuccess)
        assertEquals(ConflictResolutionStrategy.KEEP_CLOUD, fakeBackupRepository.lastAppliedStrategy)
    }

    @Test
    fun `checkCloudDataExists returns true when snapshots exist`() = runTest {
        var exists = fakeBackupRepository.checkCloudDataExists("user1").getOrThrow()
        assertEquals(false, exists)

        fakeBackupRepository.createBackupSnapshot("user1", "Backup 1")
        exists = fakeBackupRepository.checkCloudDataExists("user1").getOrThrow()
        assertEquals(true, exists)
    }
}
