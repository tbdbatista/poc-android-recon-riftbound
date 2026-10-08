package com.riftbound.recon.data.sync

import com.google.firebase.firestore.FirebaseFirestore
import com.riftbound.recon.data.local.AppPreferences
import com.riftbound.recon.data.local.CardDao
import com.riftbound.recon.data.local.CollectionCardEntity
import com.riftbound.recon.data.local.CollectionEntity
import com.riftbound.recon.domain.model.CloudCardDto
import com.riftbound.recon.domain.model.CloudCollectionDto
import com.riftbound.recon.domain.model.SyncStatus
import com.riftbound.recon.domain.repository.SyncRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreSyncRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val cardDao: CardDao,
    private val appPreferences: AppPreferences
) : SyncRepository {

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    override val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<Long?>(
        if (appPreferences.lastSyncTimestamp > 0L) appPreferences.lastSyncTimestamp else null
    )
    override val lastSyncTime: StateFlow<Long?> = _lastSyncTime.asStateFlow()

    override suspend fun syncAll(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) {
            val error = "Usuário inválido para sincronização"
            _syncStatus.value = SyncStatus.Error(error)
            return@withContext Result.failure(IllegalArgumentException(error))
        }

        _syncStatus.value = SyncStatus.Syncing

        try {
            val userCollectionsRef = firestore
                .collection("users")
                .document(userId)
                .collection("collections")

            // 1. Fetch remote collections
            val snapshot = userCollectionsRef.get().await()
            val remoteCollections = snapshot.documents.mapNotNull { it.toObject(CloudCollectionDto::class.java) }

            // 2. Fetch local collections
            val localCollections = cardDao.getCollectionsList()
            val localMap = localCollections.associateBy { it.id }

            // 3. Upload local collections to remote
            for (local in localCollections) {
                val cardEntities = cardDao.getCollectionCardEntities(local.id)
                val cloudCards = cardEntities.map { CloudCardDto(cardId = it.cardId, scanOrder = it.scanOrder) }
                val cloudDto = CloudCollectionDto(
                    id = local.id,
                    name = local.name,
                    description = local.description,
                    createdAt = local.createdAt,
                    updatedAt = System.currentTimeMillis(),
                    cards = cloudCards,
                    isDeleted = false
                )
                userCollectionsRef.document(local.id.toString()).set(cloudDto).await()
            }

            // 4. Download remote collections not present locally
            for (remote in remoteCollections) {
                if (!remote.isDeleted && !localMap.containsKey(remote.id)) {
                    val localEntity = CollectionEntity(
                        id = remote.id,
                        name = remote.name,
                        description = remote.description,
                        createdAt = remote.createdAt
                    )
                    cardDao.insertCollection(localEntity)

                    val cardEntities = remote.cards.map { card ->
                        CollectionCardEntity(
                            collectionId = remote.id,
                            cardId = card.cardId,
                            scanOrder = card.scanOrder
                        )
                    }
                    if (cardEntities.isNotEmpty()) {
                        cardDao.insertCollectionCards(cardEntities)
                    }
                }
            }

            val now = System.currentTimeMillis()
            appPreferences.lastSyncTimestamp = now
            _lastSyncTime.value = now
            _syncStatus.value = SyncStatus.Success(now)
            Result.success(Unit)
        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: "Erro na sincronização em nuvem"
            _syncStatus.value = SyncStatus.Error(errorMsg)
            Result.failure(e)
        }
    }

    override suspend fun syncCollection(userId: String, collectionId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext Result.failure(IllegalArgumentException("ID de usuário inválido"))

        try {
            val local = cardDao.getCollectionById(collectionId)
            if (local != null) {
                val cardEntities = cardDao.getCollectionCardEntities(collectionId)
                val cloudCards = cardEntities.map { CloudCardDto(cardId = it.cardId, scanOrder = it.scanOrder) }
                val cloudDto = CloudCollectionDto(
                    id = local.id,
                    name = local.name,
                    description = local.description,
                    createdAt = local.createdAt,
                    updatedAt = System.currentTimeMillis(),
                    cards = cloudCards,
                    isDeleted = false
                )
                firestore.collection("users")
                    .document(userId)
                    .collection("collections")
                    .document(collectionId.toString())
                    .set(cloudDto)
                    .await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteRemoteCollection(userId: String, collectionId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext Result.failure(IllegalArgumentException("ID de usuário inválido"))

        try {
            firestore.collection("users")
                .document(userId)
                .collection("collections")
                .document(collectionId.toString())
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
