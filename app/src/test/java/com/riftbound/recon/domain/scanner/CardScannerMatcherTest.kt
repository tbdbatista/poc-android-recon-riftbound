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
            nameZh = "阿狸 - 好奇探求者",
            set = "VEN",
            setCode = "VEN",
            collectorNumber = "sp3",
            energyCost = 3,
            power = 3,
            tags = listOf("Unit"),
            text = "",
            imageUrl = ""
        ),
        Card(
            id = 4,
            name = "Trifarian War Camp",
            nameZh = "崔法利军营",
            set = "OGN",
            setCode = "OGN",
            collectorNumber = "045",
            energyCost = 2,
            power = 0,
            tags = listOf("Landmark"),
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

    @Test
    fun `matchCard recognizes Chinese card names by Chinese text`() {
        // Test matching full Chinese card title
        val ocrFullZh = listOf(
            OcrLine("崔法利军营", 50, 50, 200, 70)
        )
        val matchedWarCamp = CardScannerMatcher.matchCard(ocrFullZh, sampleCards)
        assertNotNull("Should match Trifarian War Camp via Chinese title", matchedWarCamp)
        assertEquals("Trifarian War Camp", matchedWarCamp?.name)
        assertEquals("崔法利军营", matchedWarCamp?.nameZh)

        // Test matching Champion base Chinese name
        val ocrChampionZh = listOf(
            OcrLine("阿狸", 50, 50, 150, 70)
        )
        val matchedAhri = CardScannerMatcher.matchCard(ocrChampionZh, sampleCards)
        assertNotNull("Should match Ahri via Chinese base name", matchedAhri)
        assertEquals("Ahri, Inquisitive", matchedAhri?.name)
    }

    @Test
    fun `getBaseName and getBaseNameZh handle compound titles and parentheses safely`() {
        assertEquals("提莫", CardScannerMatcher.getBaseNameZh("提莫 - 迅捷斥候 (GG EZ)"))
        assertEquals("德莱厄斯", CardScannerMatcher.getBaseNameZh("德莱厄斯 - 崔法利之首 (异画)"))
        assertEquals("阿莱", CardScannerMatcher.getBaseNameZh("阿莱 · 热心仰慕者"))
        assertEquals("Teemo", CardScannerMatcher.getBaseName("Teemo - Scout (GG EZ)"))
        assertEquals("Darius", CardScannerMatcher.getBaseName("Darius - Trifarian (Alternate Art)"))
    }
}
