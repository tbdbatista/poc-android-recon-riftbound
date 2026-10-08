package com.riftbound.recon.ui.screens

import com.riftbound.recon.domain.model.Card
import com.riftbound.recon.ui.ScanFeedback
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanFeedbackTest {

    private val sampleCard = Card(
        id = 1,
        name = "Ahri, Inquisitive",
        set = "Vendetta",
        setCode = "VEN",
        collectorNumber = "sp3",
        energyCost = 3,
        power = 3,
        tags = listOf("Ahri", "Unit"),
        text = "When I enter...",
        imageUrl = "images/sample.webp"
    )

    @Test
    fun `ScanFeedback Success holds card reference correctly`() {
        val feedback = ScanFeedback.Success(sampleCard)
        assertEquals("Ahri, Inquisitive", feedback.card.name)
        assertEquals("VEN", feedback.card.setCode)
        assertTrue(feedback.timestamp > 0)
    }

    @Test
    fun `ScanFeedback Error holds error message correctly`() {
        val errorMessage = "Nenhum texto detectado. Centralize a carta na moldura."
        val feedback = ScanFeedback.Error(errorMessage)
        assertEquals(errorMessage, feedback.message)
        assertTrue(feedback.timestamp > 0)
    }
}
