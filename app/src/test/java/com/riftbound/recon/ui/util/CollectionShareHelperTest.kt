package com.riftbound.recon.ui.util

import com.riftbound.recon.domain.model.Card
import com.riftbound.recon.domain.model.Collection
import com.riftbound.recon.domain.model.CollectionCard
import org.junit.Assert.assertTrue
import org.junit.Test

class CollectionShareHelperTest {

    @Test
    fun `buildShareText formats collection name, quantity, collector number, set code and card name correctly`() {
        val collection = Collection(
            id = 1L,
            name = "Guardiões de Aether",
            description = "Deck de Controle",
            createdAt = System.currentTimeMillis()
        )

        val card1 = Card(
            id = 101,
            name = "Dragon's Breath",
            set = "Origins",
            setCode = "OGN",
            collectorNumber = "012",
            energyCost = 3,
            power = 5,
            tags = listOf("Spell", "Fire"),
            text = "Deal 5 damage.",
            imageUrl = ""
        )

        val card2 = Card(
            id = 102,
            name = "Mystic Barrier",
            set = "Spiritforged",
            setCode = "SPF",
            collectorNumber = "045",
            energyCost = 2,
            power = 0,
            tags = listOf("Spell"),
            text = "Prevent 4 damage.",
            imageUrl = ""
        )

        val collectionCards = listOf(
            CollectionCard(id = 1L, collectionId = 1L, card = card1, scanOrder = 1),
            CollectionCard(id = 2L, collectionId = 1L, card = card1, scanOrder = 2),
            CollectionCard(id = 3L, collectionId = 1L, card = card2, scanOrder = 3)
        )

        val shareText = CollectionShareHelper.buildShareText(collection, collectionCards)

        // Assert collection name is present
        assertTrue("Contains collection name", shareText.contains("Coleção: Guardiões de Aether"))

        // Assert total count
        assertTrue("Contains total count", shareText.contains("Total de cartas: 3 (2 únicas)"))

        // Assert Card 1 formatted with: Quantity (2x), Number (#012), Set Code (OGN), Name (Dragon's Breath)
        assertTrue("Contains card 1 quantity and details", shareText.contains("2x - #012 OGN - Dragon's Breath"))

        // Assert Card 2 formatted with: Quantity (1x), Number (#045), Set Code (SPF), Name (Mystic Barrier)
        assertTrue("Contains card 2 quantity and details", shareText.contains("1x - #045 SPF - Mystic Barrier"))
    }
}
