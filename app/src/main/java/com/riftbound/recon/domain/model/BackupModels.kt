package com.riftbound.recon.domain.model

data class CloudBackupSnapshot(
    val id: String = "",
    val title: String = "",
    val timestamp: Long = 0L,
    val collectionsCount: Int = 0,
    val totalCardsCount: Int = 0,
    val collections: List<CloudCollectionDto> = emptyList()
)

enum class ConflictResolutionStrategy {
    MERGE_ALL,
    KEEP_CLOUD,
    KEEP_LOCAL
}
