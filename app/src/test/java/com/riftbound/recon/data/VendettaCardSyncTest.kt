package com.riftbound.recon.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class VendettaCardSyncTest {

    @Test
    fun `verify Vendetta expansion cards are present and well-formed in assets json`() {
        val assetFile = File("src/main/assets/all_cards.json")
        assertTrue("all_cards.json asset file must exist", assetFile.exists())

        val lines = assetFile.readLines()

        // Count occurrences of "setName": "Vendetta"
        val vendettaSetNameCount = lines.count { it.contains("\"setName\": \"Vendetta\"") }
        val vendettaSetCodeCount = lines.count { it.contains("\"setCode\": \"VEN\"") }

        assertTrue("Vendetta cards with setName Vendetta should be exactly 227", vendettaSetNameCount == 227)
        assertEquals("Vendetta cards count by setName and setCode must match", vendettaSetNameCount, vendettaSetCodeCount)

        // Verify notable Vendetta cards exist
        val fileContent = assetFile.readText()
        assertTrue("Contains Ahri, Inquisitive", fileContent.contains("\"name\": \"Ahri, Inquisitive\""))
        assertTrue("Contains Akali, Deadly Weapon", fileContent.contains("\"name\": \"Akali, Deadly Weapon\""))
        assertTrue("Contains Zed, From the Shadows", fileContent.contains("\"name\": \"Zed, From the Shadows\""))
    }

    @Test
    fun `verify Vendetta expansion has no duplicate collector numbers`() {
        val assetFile = File("src/main/assets/all_cards.json")
        val jsonText = assetFile.readText()

        // Extract collector numbers for VEN cards
        val regex = Regex("""\{[^}]*?"setCode":\s*"VEN"[^}]*?"collectorNumber":\s*"([^"]+)"[^}]*?\}""")
        val collectorNumbers = regex.findAll(jsonText).map { it.groupValues[1] }.toList()

        assertEquals("Should extract exactly 227 collector numbers for VEN", 227, collectorNumbers.size)
        val duplicates = collectorNumbers.groupBy { it }.filter { it.value.size > 1 }.keys
        assertTrue("VEN should have 0 duplicate collector numbers, found: $duplicates", duplicates.isEmpty())
    }

    @Test
    fun `verify all cards have Chinese localization name_zh populated`() {
        val assetFile = File("src/main/assets/all_cards.json")
        val jsonText = assetFile.readText()

        val nameZhMatches = Regex(""""name_zh":\s*"([^"]+)"""").findAll(jsonText).toList()
        assertEquals("All 1341 cards must have a name_zh field", 1341, nameZhMatches.size)

        val cjkRegex = Regex("""[\u4e00-\u9fff]""")
        val cardsWithoutCjk = nameZhMatches.filterNot { cjkRegex.containsMatchIn(it.groupValues[1]) }
        assertTrue("All cards must contain valid Chinese characters in name_zh, found invalid: ${cardsWithoutCjk.map { it.groupValues[1] }}", cardsWithoutCjk.isEmpty())
    }
}

