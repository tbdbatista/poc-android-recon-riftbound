package com.riftbound.recon.ui.screens

import com.riftbound.recon.domain.model.Card
import com.riftbound.recon.domain.model.CollectionCard
import org.junit.Assert.assertEquals
import org.junit.Test

class CollectionSortingTest {

    private fun createItem(scanOrder: Int, name: String, set: String, collectorNumber: String): CollectionCard {
        return CollectionCard(
            id = scanOrder.toLong(),
            collectionId = 1L,
            card = Card(
                id = scanOrder,
                name = name,
                set = set,
                setCode = set,
                collectorNumber = collectorNumber,
                energyCost = 1,
                power = 1,
                tags = emptyList(),
                text = "",
                imageUrl = ""
            ),
            scanOrder = scanOrder
        )
    }

    private val sampleList = listOf(
        createItem(scanOrder = 1, name = "Zephyr", set = "VEN", collectorNumber = "042"),
        createItem(scanOrder = 2, name = "Aatrox", set = "OGN", collectorNumber = "002"),
        createItem(scanOrder = 3, name = "Braum", set = "SFD", collectorNumber = "010"),
        createItem(scanOrder = 4, name = "Cassiopeia", set = "OGN", collectorNumber = "001"),
        createItem(scanOrder = 5, name = "Token Soldier", set = "SFD", collectorNumber = "t01")
    )

    @Test
    fun sort_scanOrderAsc() {
        val sorted = sampleList.sortedBy { it.scanOrder }
        assertEquals(listOf(1, 2, 3, 4, 5), sorted.map { it.scanOrder })
    }

    @Test
    fun sort_scanOrderDesc() {
        val sorted = sampleList.sortedByDescending { it.scanOrder }
        assertEquals(listOf(5, 4, 3, 2, 1), sorted.map { it.scanOrder })
    }

    @Test
    fun sort_nameAsc() {
        val sorted = sampleList.sortedBy { it.card.name.lowercase() }
        assertEquals(listOf("Aatrox", "Braum", "Cassiopeia", "Token Soldier", "Zephyr"), sorted.map { it.card.name })
    }

    @Test
    fun sort_setAsc() {
        val sorted = sampleList.sortedWith(
            compareBy<CollectionCard> { it.card.set.lowercase() }
                .thenComparator { a, b -> compareCollectorNumbers(a.card.collectorNumber, b.card.collectorNumber) }
        )
        // OGN ("001", "002"), SFD ("010", "t01"), VEN ("042")
        assertEquals(listOf("Cassiopeia", "Aatrox", "Braum", "Token Soldier", "Zephyr"), sorted.map { it.card.name })
    }

    @Test
    fun sort_collectorNumberAsc() {
        val sorted = sampleList.sortedWith { a, b ->
            compareCollectorNumbers(a.card.collectorNumber, b.card.collectorNumber)
        }
        // Numeric: 001 (Cassiopeia), 002 (Aatrox), 010 (Braum), 042 (Zephyr), t01 (Token Soldier)
        assertEquals(listOf("001", "002", "010", "042", "t01"), sorted.map { it.card.collectorNumber })
    }

    @Test
    fun compareCollectorNumbers_handlesPrefixesAndDigitsCorrectly() {
        assertEquals(-1, compareCollectorNumbers("001", "002"))
        assertEquals(-1, compareCollectorNumbers("2", "10"))
        assertEquals(-1, compareCollectorNumbers("009", "10"))
        assertEquals(1, compareCollectorNumbers("100", "042"))
        assertEquals(-1, compareCollectorNumbers("r01", "r02"))
        assertEquals(-1, compareCollectorNumbers("r02", "t01"))
    }

    @Test
    fun formatCardSetAndCode_formatsOriginsWithTotal() {
        val ahriCard = Card(
            id = 7,
            name = "Ahri - Alluring (Alternate Art)",
            set = "Origins",
            setCode = "OGN",
            collectorNumber = "066a",
            energyCost = 5,
            power = 4,
            tags = listOf("Ahri", "Ionia", "Unit", "Showcase", "Calm"),
            text = "",
            imageUrl = ""
        )
        assertEquals("Origins • 066a/298", formatCardSetAndCode(ahriCard))
    }

    @Test
    fun formatCardSetAndCode_formatsOtherSetsWithKnownTotals() {
        val sfdCard = Card(
            id = 10,
            name = "Braum",
            set = "Spiritforged",
            setCode = "SFD",
            collectorNumber = "001",
            energyCost = 2,
            power = 3,
            tags = emptyList(),
            text = "",
            imageUrl = ""
        )
        val venCard = Card(
            id = 11,
            name = "Zephyr",
            set = "Vendetta",
            setCode = "VEN",
            collectorNumber = "150",
            energyCost = 1,
            power = 1,
            tags = emptyList(),
            text = "",
            imageUrl = ""
        )
        assertEquals("Spiritforged • 001/221", formatCardSetAndCode(sfdCard))
        assertEquals("Vendetta • 150/166", formatCardSetAndCode(venCard))
    }
}
