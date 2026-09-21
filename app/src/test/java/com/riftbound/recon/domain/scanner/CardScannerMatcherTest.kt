package com.riftbound.recon.domain.scanner

import com.riftbound.recon.data.scanner.OcrLine
import com.riftbound.recon.domain.model.Card
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CardScannerMatcherTest {

    private val sampleCards = listOf(
        Card(
            id = 1,
            name = "Mech // Buff",
            set = "SFD",
            setCode = "SFD",
            collectorNumber = "t01",
            energyCost = 0,
            power = 3,
            tags = listOf("Token", "Mech"),
            text = "Attached",
            imageUrl = ""
        ),
        Card(
            id = 2,
            name = "Fury Rune",
            set = "UNL",
            setCode = "UNL",
            collectorNumber = "r01",
            energyCost = 0,
            power = 0,
            tags = listOf("Rune"),
            text = "",
            imageUrl = ""
        ),
        Card(
            id = 3,
            name = "Ahri, Inquisitive",
            set = "VEN",
            setCode = "VEN",
            collectorNumber = "sp3",
            energyCost = 3,
            power = 3,
            tags = listOf("Unit"),
            text = "",
            imageUrl = ""
        )
    )

    @Test
    fun `matchCard recognizes tokens and runes with letter prefix collector numbers`() {
        val ocrMechToken = listOf(
            OcrLine("SFD • t01", 0, 0, 100, 20)
        )
        val matchedToken = CardScannerMatcher.matchCard(ocrMechToken, sampleCards)
        assertNotNull("Should match Mech token", matchedToken)
        assertEquals("Mech // Buff", matchedToken?.name)

        val ocrRune = listOf(
            OcrLine("UNL r01", 0, 0, 100, 20)
        )
        val matchedRune = CardScannerMatcher.matchCard(ocrRune, sampleCards)
        assertNotNull("Should match Fury Rune", matchedRune)
        assertEquals("Fury Rune", matchedRune?.name)

        val ocrSpecial = listOf(
            OcrLine("VEN sp3", 0, 0, 100, 20)
        )
        val matchedSpecial = CardScannerMatcher.matchCard(ocrSpecial, sampleCards)
        assertNotNull("Should match Ahri special", matchedSpecial)
        assertEquals("Ahri, Inquisitive", matchedSpecial?.name)
    }
}
