package com.riftbound.recon.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

data class CardWithScanOrder(
    val collectionCardId: Long,
    val cardId: Int,
    val name: String,
    val cardSet: String,
    val setCode: String,
    val collectorNumber: String,
    val energyCost: Int,
    val power: Int,
    val tags: String,
    val text: String,
    val imageUrl: String,
    val scanOrder: Int
)

data class CollectionOccurrence(
    val collectionId: Long,
    val collectionName: String,
    val quantity: Int
)

data class SearchCardResult(
    val collectionId: Long,
    val collectionName: String,
    val collectionCardId: Long,
    val scanOrder: Int,
    val cardId: Int,
    val cardName: String
)

data class CollectionWithCount(
    val id: Long,
    val name: String,
    val description: String,
    val createdAt: Long,
    val cardsCount: Int
)

@Dao
interface CardDao {

    // --- CARDS ---
    @Query("SELECT * FROM cards")
    fun getAllCards(): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun getCardById(id: Int): CardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<CardEntity>)

    // --- COLLECTIONS ---
    @Query("""
        SELECT c.id, c.name, c.description, c.createdAt, COUNT(cc.id) as cardsCount
        FROM collections c
        LEFT JOIN collection_cards cc ON c.id = cc.collectionId
        GROUP BY c.id
        ORDER BY c.createdAt DESC
    """)
    fun getAllCollectionsWithCount(): Flow<List<CollectionWithCount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(collection: CollectionEntity): Long

    @Update
    suspend fun updateCollection(collection: CollectionEntity)

    @Delete
    suspend fun deleteCollection(collection: CollectionEntity)

    // --- COLLECTION CARDS (Scanning Records) ---
    @Query("""
        SELECT cc.id as collectionCardId, c.id as cardId, c.name, c.cardSet, c.setCode, c.collectorNumber, 
               c.energyCost, c.power, c.tags, c.text, c.imageUrl, cc.scanOrder
        FROM collection_cards cc
        INNER JOIN cards c ON cc.cardId = c.id
        WHERE cc.collectionId = :collectionId
        ORDER BY cc.scanOrder ASC
    """)
    fun getCardsInCollection(collectionId: Long): Flow<List<CardWithScanOrder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollectionCard(cc: CollectionCardEntity): Long

    @Query("DELETE FROM collection_cards WHERE id = :id")
    suspend fun deleteCollectionCardById(id: Long)

    @Query("DELETE FROM collection_cards WHERE collectionId = :collectionId")
    suspend fun clearCollection(collectionId: Long)

    @Query("""
        SELECT COUNT(*) 
        FROM collection_cards 
        WHERE cardId = :cardId
    """)
    fun getCardCountInCollections(cardId: Int): Flow<Int>

    @Query("""
        SELECT col.id as collectionId, col.name as collectionName, COUNT(cc.id) as quantity
        FROM collection_cards cc
        INNER JOIN collections col ON cc.collectionId = col.id
        WHERE cc.cardId = :cardId
        GROUP BY cc.collectionId
    """)
    fun getCollectionsWithCard(cardId: Int): Flow<List<CollectionOccurrence>>

    @Query("""
        SELECT col.id as collectionId, col.name as collectionName, cc.id as collectionCardId, 
               cc.scanOrder as scanOrder, c.id as cardId, c.name as cardName
        FROM collection_cards cc
        INNER JOIN collections col ON cc.collectionId = col.id
        INNER JOIN cards c ON cc.cardId = c.id
        WHERE c.name LIKE '%' || :query || '%'
        ORDER BY col.name ASC, cc.scanOrder ASC
    """)
    fun searchCardInCollections(query: String): Flow<List<SearchCardResult>>
}
