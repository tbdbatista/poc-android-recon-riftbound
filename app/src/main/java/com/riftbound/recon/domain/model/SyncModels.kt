package com.riftbound.recon.domain.model

sealed interface SyncStatus {
    object Idle : SyncStatus
    object Syncing : SyncStatus
    data class Success(val lastSyncTime: Long) : SyncStatus
    data class Error(val message: String) : SyncStatus
}

data class CloudCollectionDto(
    val id: Long = 0L,
    val name: String = "",
    val description: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val cards: List<CloudCardDto> = emptyList(),
    val isDeleted: Boolean = false
)

data class CloudCardDto(
    val cardId: Int = 0,
    val scanOrder: Int = 0
)
