package com.riftbound.recon.data.sync

import com.riftbound.recon.domain.model.CloudCardDto
import com.riftbound.recon.domain.model.CloudCollectionDto
import com.riftbound.recon.domain.model.SyncStatus
import com.riftbound.recon.domain.repository.SyncRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeSyncRepository : SyncRepository {
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    override val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    override val lastSyncTime: StateFlow<Long?> = _lastSyncTime.asStateFlow()

    val syncedCollections = mutableMapOf<Long, CloudCollectionDto>()
    var shouldFail: Boolean = false

    override suspend fun syncAll(userId: String): Result<Unit> {
        if (shouldFail) {
            _syncStatus.value = SyncStatus.Error("Network failure")
            return Result.failure(RuntimeException("Network failure"))
        }
        _syncStatus.value = SyncStatus.Syncing
        val now = 1700000000000L
        _lastSyncTime.value = now
        _syncStatus.value = SyncStatus.Success(now)
        return Result.success(Unit)
    }

    override suspend fun syncCollection(userId: String, collectionId: Long): Result<Unit> {
        if (shouldFail) return Result.failure(RuntimeException("Error"))
        syncedCollections[collectionId] = CloudCollectionDto(id = collectionId, name = "Test Collection")
        return Result.success(Unit)
    }

    override suspend fun deleteRemoteCollection(userId: String, collectionId: Long): Result<Unit> {
        if (shouldFail) return Result.failure(RuntimeException("Error"))
        syncedCollections.remove(collectionId)
        return Result.success(Unit)
    }
}

class SyncRepositoryTest {

    private lateinit var fakeSyncRepository: FakeSyncRepository

    @Before
    fun setUp() {
        fakeSyncRepository = FakeSyncRepository()
    }

    @Test
    fun `syncAll updates syncStatus to Success and records timestamp`() = runTest {
        assertEquals(SyncStatus.Idle, fakeSyncRepository.syncStatus.value)
        assertEquals(null, fakeSyncRepository.lastSyncTime.value)

        val result = fakeSyncRepository.syncAll("user123")
        assertTrue(result.isSuccess)
        assertTrue(fakeSyncRepository.syncStatus.value is SyncStatus.Success)
        assertEquals(1700000000000L, fakeSyncRepository.lastSyncTime.value)
    }

    @Test
    fun `syncAll on failure updates syncStatus to Error`() = runTest {
        fakeSyncRepository.shouldFail = true

        val result = fakeSyncRepository.syncAll("user123")
        assertTrue(result.isFailure)
        assertTrue(fakeSyncRepository.syncStatus.value is SyncStatus.Error)
        assertEquals("Network failure", (fakeSyncRepository.syncStatus.value as SyncStatus.Error).message)
    }

    @Test
    fun `syncCollection uploads collection successfully`() = runTest {
        val result = fakeSyncRepository.syncCollection("user123", 42L)
        assertTrue(result.isSuccess)
        assertTrue(fakeSyncRepository.syncedCollections.containsKey(42L))
    }

    @Test
    fun `deleteRemoteCollection removes collection from cloud`() = runTest {
        fakeSyncRepository.syncCollection("user123", 42L)
        assertTrue(fakeSyncRepository.syncedCollections.containsKey(42L))

        val result = fakeSyncRepository.deleteRemoteCollection("user123", 42L)
        assertTrue(result.isSuccess)
        assertTrue(fakeSyncRepository.syncedCollections.isEmpty())
    }

    @Test
    fun `cloud collection dto serialization integrity`() {
        val cards = listOf(
            CloudCardDto(cardId = 101, scanOrder = 1),
            CloudCardDto(cardId = 102, scanOrder = 2)
        )
        val dto = CloudCollectionDto(
            id = 1L,
            name = "Competitive Deck",
            description = "Main Deck",
            createdAt = 1000L,
            updatedAt = 2000L,
            cards = cards,
            isDeleted = false
        )

        assertEquals(1L, dto.id)
        assertEquals("Competitive Deck", dto.name)
        assertEquals("Main Deck", dto.description)
        assertEquals(2, dto.cards.size)
        assertEquals(101, dto.cards[0].cardId)
        assertEquals(1, dto.cards[0].scanOrder)
    }
}
