package com.riftbound.recon.domain.repository

import com.riftbound.recon.data.local.CollectionOccurrence
import com.riftbound.recon.data.local.SearchCardResult
import com.riftbound.recon.domain.model.Card
import com.riftbound.recon.domain.model.Collection
import com.riftbound.recon.domain.model.CollectionCard
import kotlinx.coroutines.flow.Flow

interface CardRepository {
    fun getAllCards(): Flow<List<Card>>
    suspend fun getCardById(id: Int): Card?
    
    fun getAllCollections(): Flow<List<Collection>>
    suspend fun createCollection(name: String, description: String, cards: List<Card>): Long
    suspend fun updateCollection(collectionId: Long, name: String, description: String)
    suspend fun deleteCollection(collectionId: Long)
    
    fun getCardsInCollection(collectionId: Long): Flow<List<CollectionCard>>
    suspend fun addCardToCollection(collectionId: Long, cardId: Int): Long
    suspend fun removeCardFromCollection(collectionCardId: Long)
    suspend fun reorderCardsInCollection(collectionId: Long, orderedCollectionCardIds: List<Long>)
    
    fun searchCardInCollections(query: String): Flow<List<SearchCardResult>>
    fun getCollectionsWithCard(cardId: Int): Flow<List<CollectionOccurrence>>
    fun getCardCountInCollections(cardId: Int): Flow<Int>
}
