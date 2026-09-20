package com.riftbound.recon.ui.util

import android.content.Context
import android.content.Intent
import com.riftbound.recon.domain.model.Collection
import com.riftbound.recon.domain.model.CollectionCard

object CollectionShareHelper {

    /**
     * Formata a lista de cartas de uma coleção com as seguintes informações:
     * - Nome da coleção
     * - Quantidade de cada carta
     * - Número da carta (collectorNumber)
     * - Código do set de lançamento (ex: OGN, SPF)
     * - Nome da carta
     */
    fun buildShareText(collection: Collection, cards: List<CollectionCard>): String {
        val sb = StringBuilder()
        sb.append("📦 Coleção: ").append(collection.name).append("\n")
        if (collection.description.isNotBlank()) {
            sb.append("📝 ").append(collection.description).append("\n")
        }
        sb.append("🃏 Total de cartas: ").append(cards.size)

        val grouped = cards.groupBy { it.card.id }
        sb.append(" (").append(grouped.size).append(" únicas)\n\n")
        sb.append("--- Lista de Cartas ---\n")

        for ((_, cardList) in grouped) {
            val count = cardList.size
            val sampleCard = cardList.first().card
            val cardNum = if (sampleCard.collectorNumber.isNotBlank()) sampleCard.collectorNumber else "${sampleCard.id}"
            val setCode = if (sampleCard.setCode.isNotBlank()) sampleCard.setCode else "N/A"
            val name = sampleCard.name

            sb.append("• ")
                .append(count).append("x - ")
                .append("#").append(cardNum).append(" ")
                .append(setCode).append(" - ")
                .append(name).append("\n")
        }

        sb.append("\nExportado via Riftbound Recon")
        return sb.toString()
    }

    /**
     * Dispara o Intent de compartilhamento do Android (ACTION_SEND),
     * abrindo o Share Sheet do sistema operacional para enviar via
     * WhatsApp, Google Drive, Gmail, Notas ou outros apps disponíveis.
     */
    fun shareCollection(context: Context, collection: Collection, cards: List<CollectionCard>) {
        val textToShare = buildShareText(collection, cards)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Coleção: ${collection.name}")
            putExtra(Intent.EXTRA_TEXT, textToShare)
        }
        val shareChooser = Intent.createChooser(sendIntent, "Compartilhar Coleção")
        context.startActivity(shareChooser)
    }
}
