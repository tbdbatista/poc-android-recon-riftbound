package com.riftbound.recon.data.backup

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.riftbound.recon.data.local.AppPreferences
import com.riftbound.recon.data.local.CardDao
import com.riftbound.recon.data.local.CollectionCardEntity
import com.riftbound.recon.data.local.CollectionEntity
import com.riftbound.recon.domain.model.CloudBackupSnapshot
import com.riftbound.recon.domain.model.CloudCardDto
import com.riftbound.recon.domain.model.CloudCollectionDto
import com.riftbound.recon.domain.model.ConflictResolutionStrategy
import com.riftbound.recon.domain.repository.BackupRepository
import com.riftbound.recon.domain.repository.SyncRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreBackupRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val cardDao: CardDao,
    private val appPreferences: AppPreferences,
    private val syncRepository: SyncRepository
) : BackupRepository {

    private val _backupSnapshots = MutableStateFlow<List<CloudBackupSnapshot>>(emptyList())
    override val backupSnapshots: StateFlow<List<CloudBackupSnapshot>> = _backupSnapshots.asStateFlow()

    private val _isOperationInProgress = MutableStateFlow(false)
    override val isOperationInProgress: StateFlow<Boolean> = _isOperationInProgress.asStateFlow()

    override suspend fun loadBackupSnapshots(userId: String): Result<List<CloudBackupSnapshot>> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext Result.failure(IllegalArgumentException("ID de usuário inválido"))

        _isOperationInProgress.value = true
        try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("backups")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            val list = snapshot.documents.mapNotNull { it.toObject(CloudBackupSnapshot::class.java) }
            _backupSnapshots.value = list
            _isOperationInProgress.value = false
            Result.success(list)
        } catch (e: Exception) {
            _isOperationInProgress.value = false
            Result.failure(e)
        }
    }

    override suspend fun createBackupSnapshot(userId: String, title: String): Result<CloudBackupSnapshot> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext Result.failure(IllegalArgumentException("ID de usuário inválido"))

        _isOperationInProgress.value = true
        try {
            val localCollections = cardDao.getCollectionsList()
            var totalCards = 0
            val cloudCollections = mutableListOf<CloudCollectionDto>()

            for (col in localCollections) {
                val cardEntities = cardDao.getCollectionCardEntities(col.id)
                totalCards += cardEntities.size
                val cloudCards = cardEntities.map { CloudCardDto(cardId = it.cardId, scanOrder = it.scanOrder) }
                cloudCollections.add(
                    CloudCollectionDto(
                        id = col.id,
                        name = col.name,
                        description = col.description,
                        createdAt = col.createdAt,
                        updatedAt = System.currentTimeMillis(),
                        cards = cloudCards,
                        isDeleted = false
                    )
                )
            }

            val backupId = UUID.randomUUID().toString()
            val backupSnapshot = CloudBackupSnapshot(
                id = backupId,
                title = title.ifBlank { "Backup Manual" },
                timestamp = System.currentTimeMillis(),
                collectionsCount = localCollections.size,
                totalCardsCount = totalCards,
                collections = cloudCollections
            )

            firestore.collection("users")
                .document(userId)
                .collection("backups")
                .document(backupId)
                .set(backupSnapshot)
                .await()

            loadBackupSnapshots(userId)
            _isOperationInProgress.value = false
            Result.success(backupSnapshot)
        } catch (e: Exception) {
            _isOperationInProgress.value = false
            Result.failure(e)
        }
    }

    override suspend fun restoreBackupSnapshot(userId: String, backupId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext Result.failure(IllegalArgumentException("ID de usuário inválido"))

        _isOperationInProgress.value = true
        try {
            val doc = firestore.collection("users")
                .document(userId)
                .collection("backups")
                .document(backupId)
                .get()
                .await()

            val snapshot = doc.toObject(CloudBackupSnapshot::class.java)
                ?: return@withContext Result.failure(IllegalStateException("Backup não encontrado"))

            // 1. Wipe current local collections
            cardDao.deleteAllCollectionCards()
            cardDao.deleteAllCollections()

            // 2. Clear remote active collections
            val activeCollectionsRef = firestore.collection("users")
                .document(userId)
                .collection("collections")

            val activeSnap = activeCollectionsRef.get().await()
            for (activeDoc in activeSnap.documents) {
                activeDoc.reference.delete().await()
            }

            // 3. Restore snapshot collections into Room & active Firestore
            for (col in snapshot.collections) {
                val localEntity = CollectionEntity(
                    id = col.id,
                    name = col.name,
                    description = col.description,
                    createdAt = col.createdAt
                )
                cardDao.insertCollection(localEntity)

                val cardEntities = col.cards.map { card ->
                    CollectionCardEntity(
                        collectionId = col.id,
                        cardId = card.cardId,
                        scanOrder = card.scanOrder
                    )
                }
                if (cardEntities.isNotEmpty()) {
                    cardDao.insertCollectionCards(cardEntities)
                }

                // Update active collection in Firestore
                activeCollectionsRef.document(col.id.toString()).set(col).await()
            }

            val now = System.currentTimeMillis()
            appPreferences.lastSyncTimestamp = now
            _isOperationInProgress.value = false
            Result.success(Unit)
        } catch (e: Exception) {
            _isOperationInProgress.value = false
            Result.failure(e)
        }
    }

    override suspend fun deleteBackupSnapshot(userId: String, backupId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext Result.failure(IllegalArgumentException("ID de usuário inválido"))

        _isOperationInProgress.value = true
        try {
            firestore.collection("users")
                .document(userId)
                .collection("backups")
                .document(backupId)
                .delete()
                .await()

            loadBackupSnapshots(userId)
            _isOperationInProgress.value = false
            Result.success(Unit)
        } catch (e: Exception) {
            _isOperationInProgress.value = false
            Result.failure(e)
        }
    }

    override suspend fun resolveLoginConflict(userId: String, strategy: ConflictResolutionStrategy): Result<Unit> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext Result.failure(IllegalArgumentException("ID de usuário inválido"))

        _isOperationInProgress.value = true
        try {
            when (strategy) {
                ConflictResolutionStrategy.MERGE_ALL -> {
                    syncRepository.syncAll(userId)
                }
                ConflictResolutionStrategy.KEEP_CLOUD -> {
                    // Overwrite local with remote
                    val activeRef = firestore.collection("users")
                        .document(userId)
                        .collection("collections")

                    val snap = activeRef.get().await()
                    val remoteCols = snap.documents.mapNotNull { it.toObject(CloudCollectionDto::class.java) }

                    cardDao.deleteAllCollectionCards()
                    cardDao.deleteAllCollections()

                    for (col in remoteCols) {
                        if (!col.isDeleted) {
                            cardDao.insertCollection(
                                CollectionEntity(
                                    id = col.id,
                                    name = col.name,
                                    description = col.description,
                                    createdAt = col.createdAt
                                )
                            )
                            val cards = col.cards.map {
                                CollectionCardEntity(
                                    collectionId = col.id,
                                    cardId = it.cardId,
                                    scanOrder = it.scanOrder
                                )
                            }
                            if (cards.isNotEmpty()) {
                                cardDao.insertCollectionCards(cards)
                            }
                        }
                    }
                    appPreferences.lastSyncTimestamp = System.currentTimeMillis()
                }
                ConflictResolutionStrategy.KEEP_LOCAL -> {
                    // Overwrite remote with local
                    val activeRef = firestore.collection("users")
                        .document(userId)
                        .collection("collections")

                    val snap = activeRef.get().await()
                    for (doc in snap.documents) {
                        doc.reference.delete().await()
                    }

                    val localCollections = cardDao.getCollectionsList()
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
                        activeRef.document(local.id.toString()).set(cloudDto).await()
                    }
                    appPreferences.lastSyncTimestamp = System.currentTimeMillis()
                }
            }
            _isOperationInProgress.value = false
            Result.success(Unit)
        } catch (e: Exception) {
            _isOperationInProgress.value = false
            Result.failure(e)
        }
    }

    override suspend fun checkCloudDataExists(userId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext Result.success(false)

        try {
            val collectionsSnap = firestore.collection("users")
                .document(userId)
                .collection("collections")
                .limit(1)
                .get()
                .await()

            if (!collectionsSnap.isEmpty) return@withContext Result.success(true)

            val backupsSnap = firestore.collection("users")
                .document(userId)
                .collection("backups")
                .limit(1)
                .get()
                .await()

            Result.success(!backupsSnap.isEmpty)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
