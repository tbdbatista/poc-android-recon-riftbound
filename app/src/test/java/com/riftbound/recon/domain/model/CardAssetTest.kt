package com.riftbound.recon.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CardAssetTest {

    @Test
    fun assetImageUrl_webpPath_returnsCorrectAssetUri() {
        val card = Card(
            id = 1,
            name = "Test Card",
            set = "JDG",
            setCode = "JDG",
            collectorNumber = "001",
            energyCost = 2,
            power = 3,
            tags = listOf("Unit"),
            text = "Some text",
            imageUrl = "images/sample_image.webp"
        )

        assertEquals("file:///android_asset/images/sample_image.webp", card.assetImageUrl)
    }

    @Test
    fun assetImageUrl_legacyPngPath_normalizesToWebpAssetUri() {
        val card = Card(
            id = 2,
            name = "Legacy PNG Card",
            set = "UNL",
            setCode = "UNL",
            collectorNumber = "002",
            energyCost = 1,
            power = 1,
            tags = listOf("Unit"),
            text = "Legacy text",
            imageUrl = "images/legacy_image.png"
        )

        assertEquals("file:///android_asset/images/legacy_image.webp", card.assetImageUrl)
    }
}
