package com.riftbound.recon.domain.scanner

import com.riftbound.recon.data.scanner.OcrLine
import com.riftbound.recon.domain.model.Card

object CardScannerMatcher {

    private val cjkRegex = Regex("""[\u4e00-\u9fff]""")

    fun normalize(s: String): String {
        return s.lowercase()
            .replace(Regex("[^a-z0-9 ]"), "")
            .trim()
    }

    fun normalizeZh(s: String): String {
        return s.replace(Regex("""[^\u4e00-\u9fffa-zA-Z0-9]"""), "").trim()
    }

    fun getSimilarity(s1: String, s2: String): Double {
        val longer = normalize(s1)
        val shorter = normalize(s2)
        if (longer.length < shorter.length) {
            return getSimilarity(s2, s1)
        }
        if (longer.isEmpty()) {
            return 1.0
        }
        val distance = editDistance(longer, shorter)
        return (longer.length - distance).toDouble() / longer.length.toDouble()
    }

    private fun editDistance(s1: String, s2: String): Int {
        val costs = IntArray(s2.length + 1)
        for (i in 0..s1.length) {
            var lastValue = i
            for (j in 0..s2.length) {
                if (i == 0) {
                    costs[j] = j
                } else {
                    if (j > 0) {
                        var newValue = costs[j - 1]
                        if (s1[i - 1] != s2[j - 1]) {
                            newValue = Math.min(Math.min(newValue, lastValue), costs[j]) + 1
                        }
                        costs[j - 1] = lastValue
                        lastValue = newValue
                    }
                }
            }
            if (i > 0) {
                costs[s2.length] = lastValue
            }
        }
        return costs[s2.length]
    }

    fun getBaseName(name: String): String {
        val delimiters = listOf(",", " - ", "(")
        val minIndex = delimiters.map { name.indexOf(it) }
            .filter { it >= 0 }
            .minOrNull()

        return if (minIndex != null && minIndex >= 0 && minIndex <= name.length) {
            name.substring(0, minIndex).trim()
        } else {
            name.trim()
        }
    }

    fun getBaseNameZh(nameZh: String): String {
        val delimiters = listOf(" - ", " · ", "(", "（")
        val minIndex = delimiters.map { nameZh.indexOf(it) }
            .filter { it >= 0 }
            .minOrNull()

        return if (minIndex != null && minIndex >= 0 && minIndex <= nameZh.length) {
            nameZh.substring(0, minIndex).trim()
        } else {
            nameZh.trim()
        }
    }

    fun matchCard(ocrLines: List<OcrLine>, cards: List<Card>): Card? {
        if (ocrLines.isEmpty()) return null

        // 1. Precise Set Code + Collector Number Regex Match (100% accurate across EN & ZH prints)
        val codeRegex = Regex("""\b(OGN|SFD|UNL|OGS|OPP|JDG|PR|VEN)\b[^\d]*?\b([a-z]{0,2}[0-9]{1,4}[a-z]?)\b""", RegexOption.IGNORE_CASE)
        for (line in ocrLines) {
            val match = codeRegex.find(line.text)
            if (match != null) {
                val setCode = match.groupValues[1].uppercase()
                val collectorNumStr = match.groupValues[2].lowercase()
                val card = cards.find {
                    it.setCode.equals(setCode, ignoreCase = true) &&
                    (it.collectorNumber.lowercase() == collectorNumStr || it.collectorNumber.lowercase().trimStart('0') == collectorNumStr.trimStart('0'))
                }
                if (card != null) {
                    return card
                }
            }
        }

        // 1.1 Collector Number Fraction Matching (e.g. 204/298, 021/227 even if set code is covered)
        val fractionRegex = Regex("""\b([a-zA-Z]{0,2}\d{1,4})/(\d{2,3})\b""")
        val setDenominators = mapOf(
            "298" to "OGN",
            "227" to "VEN",
            "250" to "SFD",
            "220" to "UNL"
        )
        for (line in ocrLines) {
            val match = fractionRegex.find(line.text)
            if (match != null) {
                val numStr = match.groupValues[1].lowercase()
                val denomStr = match.groupValues[2]
                val inferredSet = setDenominators[denomStr]
                val card = cards.find {
                    (inferredSet == null || it.setCode.equals(inferredSet, ignoreCase = true)) &&
                    (it.collectorNumber.lowercase() == numStr || it.collectorNumber.lowercase().trimStart('0') == numStr.trimStart('0'))
                }
                if (card != null) {
                    return card
                }
            }
        }

        // 2. Identify Energy Cost Candidate from Top-Left Quadrant (for disambiguating variants)
        val minTop = ocrLines.minOf { it.top }
        val maxBottom = ocrLines.maxOf { it.bottom }
        val minLeft = ocrLines.minOf { it.left }
        val maxRight = ocrLines.maxOf { it.right }
        
        val totalHeight = maxBottom - minTop
        val totalWidth = maxRight - minLeft
        
        val topLimit = minTop + (totalHeight * 0.35).toInt()
        val leftLimit = minLeft + (totalWidth * 0.40).toInt()
        
        var detectedEnergyCost: Int? = null
        val numberRegex = Regex("""\b([0-9]|10)\b""")
        
        for (line in ocrLines) {
            if (line.top <= topLimit && line.left <= leftLimit) {
                val match = numberRegex.find(line.text.trim())
                if (match != null) {
                    detectedEnergyCost = match.groupValues[1].toIntOrNull()
                    break
                }
            }
        }

        // Sort OCR lines by vertical position (top-to-bottom) so titles near the top are prioritized
        val sortedLines = ocrLines.sortedBy { it.top }

        // 3. Chinese Name Matching (if OCR line contains CJK characters)
        for (line in sortedLines) {
            if (cjkRegex.containsMatchIn(line.text)) {
                val normalizedLineZh = normalizeZh(line.text)
                if (normalizedLineZh.length < 2) continue

                // 3.1 Exact Full Chinese Name Match (instant match)
                for (card in cards) {
                    val cardZh = card.nameZh ?: continue
                    val normalizedFullZh = normalizeZh(cardZh)
                    if (normalizedFullZh.isNotEmpty() && (normalizedLineZh == normalizedFullZh || (normalizedFullZh.length >= 2 && normalizedLineZh.contains(normalizedFullZh)))) {
                        return card
                    }
                }

                // 3.2 Base Chinese Name Match (with energy cost disambiguation if multiple candidates)
                val candidatesZh = mutableListOf<Card>()
                for (card in cards) {
                    val cardZh = card.nameZh ?: continue
                    val normalizedBaseZh = normalizeZh(getBaseNameZh(cardZh))
                    if (normalizedBaseZh.length >= 2 && (normalizedLineZh == normalizedBaseZh || normalizedLineZh.contains(normalizedBaseZh) || normalizedBaseZh.contains(normalizedLineZh))) {
                        candidatesZh.add(card)
                    }
                }

                if (candidatesZh.isNotEmpty()) {
                    if (candidatesZh.size == 1) {
                        return candidatesZh.first()
                    }
                    if (detectedEnergyCost != null) {
                        val energyMatched = candidatesZh.find { it.energyCost == detectedEnergyCost }
                        if (energyMatched != null) return energyMatched
                    }
                    return candidatesZh.first()
                }
            }
        }

        // 4. Token-Based English Name Matching
        for (line in sortedLines) {
            val normalizedLine = normalize(line.text)
            if (normalizedLine.length < 3 || isBlacklisted(normalizedLine)) continue

            // 4.1 Exact Full English Name Match (instant match)
            for (card in cards) {
                val normalizedFullName = normalize(card.name)
                if (normalizedFullName.length >= 3 && (normalizedLine == normalizedFullName || (normalizedFullName.length >= 4 && normalizedLine.contains(normalizedFullName)))) {
                    return card
                }
            }

            // 4.2 Base English Name Match (with energy cost disambiguation if multiple candidates)
            val candidatesEn = mutableListOf<Card>()
            for (card in cards) {
                val normalizedBase = normalize(getBaseName(card.name))
                if (normalizedBase.length >= 3) {
                    if (normalizedLine == normalizedBase || (normalizedBase.length >= 4 && normalizedLine.contains(normalizedBase))) {
                        candidatesEn.add(card)
                    } else if (getSimilarity(normalizedLine, normalizedBase) > 0.88) {
                        candidatesEn.add(card)
                    }
                }
            }

            if (candidatesEn.isNotEmpty()) {
                if (candidatesEn.size == 1) {
                    return candidatesEn.first()
                }
                if (detectedEnergyCost != null) {
                    val energyMatched = candidatesEn.find { it.energyCost == detectedEnergyCost }
                    if (energyMatched != null) return energyMatched
                }
                return candidatesEn.first()
            }
        }
        return null
    }

    private fun isBlacklisted(word: String): Boolean {
        val blacklist = setOf(
            "unit", "spell", "damage", "energy", "might", "cost", "play", "discard",
            "drawn", "round", "bonus", "spells", "abilities", "ready", "hidden",
            "accelerate", "spells and", "spells and abilities"
        )
        return blacklist.contains(word)
    }
}
