package com.riftbound.recon.data.repository

import com.riftbound.recon.data.local.*
import com.riftbound.recon.domain.model.Card
import com.riftbound.recon.domain.model.Collection
import com.riftbound.recon.domain.model.CollectionCard
import com.riftbound.recon.domain.repository.CardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray

class CardRepositoryImpl @Inject constructor(
    private val cardDao: CardDao,
    @ApplicationContext private val context: Context
) : CardRepository {

    init {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentCards = cardDao.getAllCards().first()
                val seed = getSeedCardsFromAssets(context)
                if (currentCards.size < seed.size) {
                    if (seed.isNotEmpty()) {
                        cardDao.insertCards(seed)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getSeedCardsFromAssets(context: Context): List<CardEntity> {
        val list = mutableListOf<CardEntity>()
        try {
            val jsonString = context.assets.open("all_cards.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val setCode = obj.optString("setCode", "Unknown")
                val collectorNumber = obj.optString("collectorNumber", "0")

                list.add(
                    CardEntity(
                        id = obj.getInt("id"),
                        name = obj.getString("name"),
                        cardSet = obj.getString("setName"),
                        setCode = setCode,
                        collectorNumber = collectorNumber,
                        energyCost = obj.getInt("energyCost"),
                        power = obj.getInt("power"),
                        tags = obj.getString("tags"),
                        text = obj.getString("text"),
                        imageUrl = obj.getString("imageUrl")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun CardEntity.toDomain(): Card {
        return Card(
            id = id,
            name = name,
            set = cardSet,
            setCode = setCode,
            collectorNumber = collectorNumber,
            energyCost = energyCost,
            power = power,
            tags = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            text = text,
            imageUrl = imageUrl
        )
    }

    override fun getAllCards(): Flow<List<Card>> {
        return cardDao.getAllCards().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getCardById(id: Int): Card? {
        return cardDao.getCardById(id)?.toDomain()
    }

    override fun getAllCollections(): Flow<List<Collection>> {
        return cardDao.getAllCollectionsWithCount().map { list ->
            list.map {
                Collection(
                    id = it.id,
                    name = it.name,
                    description = it.description,
                    createdAt = it.createdAt,
                    cardsCount = it.cardsCount
                )
            }
        }
    }

    override suspend fun createCollection(name: String, description: String, cards: List<Card>): Long {
        val collection = CollectionEntity(
            name = name,
            description = description,
            createdAt = System.currentTimeMillis()
        )
        val collectionId = cardDao.insertCollection(collection)
        
        cards.forEachIndexed { index, card ->
            cardDao.insertCollectionCard(
                CollectionCardEntity(
                    collectionId = collectionId,
                    cardId = card.id,
                    scanOrder = index + 1
                )
            )
        }
        return collectionId
    }

    override suspend fun updateCollection(collectionId: Long, name: String, description: String) {
        val current = cardDao.getAllCollectionsWithCount().first().find { it.id == collectionId }
        if (current != null) {
            cardDao.updateCollection(
                CollectionEntity(
                    id = collectionId,
                    name = name,
                    description = description,
                    createdAt = current.createdAt
                )
            )
        }
    }

    override suspend fun deleteCollection(collectionId: Long) {
        val current = cardDao.getAllCollectionsWithCount().first().find { it.id == collectionId }
        if (current != null) {
            cardDao.deleteCollection(
                CollectionEntity(
                    id = collectionId,
                    name = current.name,
                    description = current.description,
                    createdAt = current.createdAt
                )
            )
        }
    }

    override fun getCardsInCollection(collectionId: Long): Flow<List<CollectionCard>> {
        return cardDao.getCardsInCollection(collectionId).map { list ->
            list.map { item ->
                CollectionCard(
                    id = item.collectionCardId,
                    collectionId = collectionId,
                    card = Card(
                        id = item.cardId,
                        name = item.name,
                        set = item.cardSet,
                        setCode = item.setCode,
                        collectorNumber = item.collectorNumber,
                        energyCost = item.energyCost,
                        power = item.power,
                        tags = item.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                        text = item.text,
                        imageUrl = item.imageUrl
                    ),
                    scanOrder = item.scanOrder
                )
            }
        }
    }

    override suspend fun addCardToCollection(collectionId: Long, cardId: Int): Long {
        val currentCards = cardDao.getCardsInCollection(collectionId).first()
        val nextScanOrder = (currentCards.maxOfOrNull { it.scanOrder } ?: 0) + 1
        return cardDao.insertCollectionCard(
            CollectionCardEntity(
                collectionId = collectionId,
                cardId = cardId,
                scanOrder = nextScanOrder
            )
        )
    }

    override suspend fun removeCardFromCollection(collectionCardId: Long) {
        cardDao.deleteCollectionCardById(collectionCardId)
    }

    override suspend fun reorderCardsInCollection(collectionId: Long, orderedCollectionCardIds: List<Long>) {
        orderedCollectionCardIds.forEachIndexed { index, id ->
            cardDao.updateCollectionCardOrder(id, index + 1)
        }
    }

    override fun searchCardInCollections(query: String): Flow<List<SearchCardResult>> {
        return cardDao.searchCardInCollections(query)
    }

    override fun getCollectionsWithCard(cardId: Int): Flow<List<CollectionOccurrence>> {
        return cardDao.getCollectionsWithCard(cardId)
    }

    override fun getCardCountInCollections(cardId: Int): Flow<Int> {
        return cardDao.getCardCountInCollections(cardId)
    }
}
