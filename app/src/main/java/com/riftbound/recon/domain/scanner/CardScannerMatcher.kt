package com.riftbound.recon.domain.scanner

import com.riftbound.recon.data.scanner.OcrLine
import com.riftbound.recon.domain.model.Card

object CardScannerMatcher {

    fun normalize(s: String): String {
        return s.lowercase()
            .replace(Regex("[^a-z0-9 ]"), "")
            .trim()
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
        val commaIndex = name.indexOf(",")
        val parenIndex = name.indexOf("(")
        var base = name
        if (commaIndex != -1 && (parenIndex == -1 || commaIndex < parenIndex)) {
            base = name.substring(0, commaIndex)
        } else if (parenIndex != -1) {
            base = name.substring(0, parenIndex)
        }
        return base.trim()
    }

    fun matchCard(ocrLines: List<OcrLine>, cards: List<Card>): Card? {
        if (ocrLines.isEmpty()) return null

        // 1. Precise Set Code + Collector Number Regex Match (100% accurate)
        val codeRegex = Regex("""\b(OGN|SFD|UNL|OGS|OPP|JDG|PR|VEN)\b[^\d]*?\b([a-z]?[0-9]{1,4}[a-z]?)\b""", RegexOption.IGNORE_CASE)
        for (line in ocrLines) {
            val match = codeRegex.find(line.text)
            if (match != null) {
                val setCode = match.groupValues[1].uppercase()
                val collectorNumStr = match.groupValues[2].lowercase()
                val card = cards.find { it.setCode.equals(setCode, ignoreCase = true) && it.collectorNumber.lowercase() == collectorNumStr }
                if (card != null) {
                    return card
                }
            }
        }

        // 2. Identify Energy Cost Candidate from Top-Left Quadrant
        // Find coordinate bounds of the detected text frame
        val minTop = ocrLines.minOf { it.top }
        val maxBottom = ocrLines.maxOf { it.bottom }
        val minLeft = ocrLines.minOf { it.left }
        val maxRight = ocrLines.maxOf { it.right }
        
        val totalHeight = maxBottom - minTop
        val totalWidth = maxRight - minLeft
        
        // Energy cost is located at the top-left of the card:
        // Top 35% vertically, Left 40% horizontally
        val topLimit = minTop + (totalHeight * 0.35).toInt()
        val leftLimit = minLeft + (totalWidth * 0.40).toInt()
        
        var detectedEnergyCost: Int? = null
        val numberRegex = Regex("""\b([1-9]|10)\b""")
        
        for (line in ocrLines) {
            if (line.top <= topLimit && line.left <= leftLimit) {
                val match = numberRegex.find(line.text.trim())
                if (match != null) {
                    detectedEnergyCost = match.groupValues[1].toIntOrNull()
                    break // Capture first match in quadrant
                }
            }
        }

        // 3. Token-Based Name Matching (Fuzzy and Substring with Base-Name extraction)
        for (card in cards) {
            val baseName = getBaseName(card.name)
            val normalizedBaseName = normalize(baseName)
            if (normalizedBaseName.length < 3) continue

            for (line in ocrLines) {
                val normalizedLine = normalize(line.text)
                
                // Exclude common card rules vocabulary
                if (isBlacklisted(normalizedLine)) continue
                
                var isNameMatched = false
                
                // Exact normalized match
                if (normalizedLine == normalizedBaseName) {
                    isNameMatched = true
                }
                
                // Substring match with word protection (avoid matching short words like "Lux" inside larger words)
                if (!isNameMatched && normalizedBaseName.length >= 4) {
                    // Only match if the OCR line contains the full card title (not vice versa to avoid false positive rules matches)
                    if (normalizedLine.contains(normalizedBaseName)) {
                        isNameMatched = true
                    }
                }
                
                // Fuzzy edit-distance similarity match
                if (!isNameMatched && getSimilarity(normalizedLine, normalizedBaseName) > 0.88) {
                    isNameMatched = true
                }
                
                if (isNameMatched) {
                    // Validation Stage: If energy cost was found in the top-left quadrant, it must match!
                    if (detectedEnergyCost != null && card.energyCost != detectedEnergyCost) {
                        continue // Reject this card match
                    }
                    return card
                }
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
