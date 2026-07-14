package com.riftbound.recon.domain.model

data class Card(
    val id: Int,
    val name: String,
    val set: String,
    val setCode: String,
    val collectorNumber: String,
    val energyCost: Int,
    val power: Int,
    val tags: List<String>,
    val text: String,
    val imageUrl: String
)

data class Collection(
    val id: Long,
    val name: String,
    val description: String,
    val createdAt: Long,
    val cardsCount: Int = 0
)

data class CollectionCard(
    val id: Long,
    val collectionId: Long,
    val card: Card,
    val scanOrder: Int
)
