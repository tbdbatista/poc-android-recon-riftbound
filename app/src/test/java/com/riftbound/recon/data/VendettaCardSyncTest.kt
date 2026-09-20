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

        assertEquals("Vendetta cards with setName Vendetta must be 131", 131, vendettaSetNameCount)
        assertEquals("Vendetta cards with setCode VEN must be 131", 131, vendettaSetCodeCount)

        // Verify notable Vendetta cards exist
        val fileContent = assetFile.readText()
        assertTrue("Contains Ahri, Inquisitive", fileContent.contains("\"name\": \"Ahri, Inquisitive\""))
        assertTrue("Contains Akali, Deadly Weapon", fileContent.contains("\"name\": \"Akali, Deadly Weapon\""))
        assertTrue("Contains Zed, From the Shadows", fileContent.contains("\"name\": \"Zed, From the Shadows\""))
    }
}
