package com.riftbound.recon.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val cardSet: String, // Origins, Proving Grounds, Spiritforged, Unleashed
    val setCode: String,
    val collectorNumber: String,
    val energyCost: Int,
    val power: Int,
    val tags: String, // Comma-separated
    val text: String,
    val imageUrl: String
)

@Entity(tableName = "collections")
data class CollectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val createdAt: Long
)

@Entity(
    tableName = "collection_cards",
    foreignKeys = [
        ForeignKey(
            entity = CollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CardEntity::class,
            parentColumns = ["id"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("collectionId"),
        Index("cardId")
    ]
)
data class CollectionCardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val collectionId: Long,
    val cardId: Int,
    val scanOrder: Int
)
