package com.wanlian.printer.model

/**
 * Independent vertical adjustment for a recognised closing phrase such as 千古 or 叩挽.
 * The phrase itself remains in [PrintSettings.text]; only its resolved drawing position moves.
 */
data class ClosingTextBlockSettings(
    val offsetYMm: Float = 0f,
    /** Per-character vertical corrections created by pair-tail alignment. */
    val characterOffsetDots: List<Float> = emptyList(),
)

data class ClosingTextMatch(
    val phrase: String,
    val characterIndexes: Set<Int>,
)

data class ClosingTextBlockAlignmentGeometry(
    /** Center of the first recognised tail character before the block offset is applied. */
    val zeroOffsetFirstCharacterCenterYDots: Float,
    val minimumOffsetDots: Float,
    val maximumOffsetDots: Float,
)

object ClosingTextBlockRules {
    const val MIN_OFFSET_Y_MM = -500f
    const val MAX_OFFSET_Y_MM = 500f

    private val phrases = listOf(
        "音容宛在",
        "千古",
        "叩挽",
        "敬挽",
        "哀挽",
        "泣挽",
        "拜挽",
        "谨挽",
        "灵右",
        "安息",
        "千秋",
    ).map { phrase -> phrase to phrase.toCodePointStrings() }

    fun clampOffsetYMm(value: Float): Float = value.coerceIn(MIN_OFFSET_Y_MM, MAX_OFFSET_Y_MM)

    /**
     * Finds a known phrase at the visible end of a vertical column. Whitespace before the
     * phrase remains part of the original flow, so an offset of zero is pixel-compatible with
     * the previous layout.
     */
    fun matchColumn(characters: List<String>): ClosingTextMatch? {
        val visibleCharacters = characters.withIndex().filterNot { it.value.isBlank() }
        return phrases.firstNotNullOfOrNull { (phrase, phraseCharacters) ->
            if (visibleCharacters.size < phraseCharacters.size) {
                return@firstNotNullOfOrNull null
            }
            val suffix = visibleCharacters.takeLast(phraseCharacters.size)
            if (suffix.map { it.value } != phraseCharacters) {
                return@firstNotNullOfOrNull null
            }
            // A four-character phrase is treated as a closing block only when it is a suffix
            // of a longer column. This avoids moving an entire standalone main-text column.
            if (phraseCharacters.size > 2 && visibleCharacters.size == phraseCharacters.size) {
                return@firstNotNullOfOrNull null
            }
            ClosingTextMatch(
                phrase = phrase,
                characterIndexes = suffix.mapTo(linkedSetOf()) { it.index },
            )
        }
    }

    fun detectedPhrases(text: String): List<String> = text.lineSequence()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .map { line -> line.toCodePointStrings() }
        .mapNotNull(::matchColumn)
        .map(ClosingTextMatch::phrase)
        .distinct()
        .toList()

    /** Keeps the complete block inside its safe vertical area without moving other content. */
    fun resolveOffsetDots(
        requestedOffsetDots: Float,
        blockTopDots: Float,
        blockBottomDots: Float,
        safeTopDots: Float,
        safeBottomDots: Float,
    ): Float {
        // Preserve the legacy layout exactly until the user makes an adjustment.
        if (requestedOffsetDots == 0f) return 0f
        return safeOffsetBounds(
            blockTopDots = blockTopDots,
            blockBottomDots = blockBottomDots,
            safeTopDots = safeTopDots,
            safeBottomDots = safeBottomDots,
        )?.let { bounds -> requestedOffsetDots.coerceIn(bounds.first, bounds.second) } ?: 0f
    }

    fun alignmentOffsetDots(
        geometry: ClosingTextBlockAlignmentGeometry,
        anchorFirstCharacterCenterYDots: Float,
    ): Float = (anchorFirstCharacterCenterYDots - geometry.zeroOffsetFirstCharacterCenterYDots).coerceIn(
        geometry.minimumOffsetDots,
        geometry.maximumOffsetDots,
    )

    /**
     * Produces the independent corrections needed after the first tail character has been
     * translated as a block. A zero in the first position keeps that character as the anchor.
     */
    fun characterOffsetsForAlignment(
        movingZeroOffsetCenters: List<Float>,
        anchorCenters: List<Float>,
        resolvedBlockOffsetDots: Float = if (
            movingZeroOffsetCenters.isNotEmpty() && anchorCenters.isNotEmpty()
        ) {
            anchorCenters.first() - movingZeroOffsetCenters.first()
        } else {
            0f
        },
    ): List<Float>? {
        if (movingZeroOffsetCenters.isEmpty() || movingZeroOffsetCenters.size != anchorCenters.size) {
            return null
        }
        return movingZeroOffsetCenters.indices.map { index ->
            anchorCenters[index] - movingZeroOffsetCenters[index] - resolvedBlockOffsetDots
        }
    }

    fun residualCharacterCorrections(
        movingCenters: List<Float>,
        anchorCenters: List<Float>,
    ): List<Float>? {
        if (movingCenters.isEmpty() || movingCenters.size != anchorCenters.size) return null
        return movingCenters.indices.map { index -> anchorCenters[index] - movingCenters[index] }
    }

    fun safeOffsetBounds(
        blockTopDots: Float,
        blockBottomDots: Float,
        safeTopDots: Float,
        safeBottomDots: Float,
    ): Pair<Float, Float>? {
        val minimumOffset = safeTopDots - blockTopDots
        val maximumOffset = safeBottomDots - blockBottomDots
        return if (minimumOffset <= maximumOffset) {
            minimumOffset to maximumOffset
        } else {
            null
        }
    }
}

private fun String.toCodePointStrings(): List<String> = buildList {
    var offset = 0
    while (offset < length) {
        val codePoint = codePointAt(offset)
        add(String(Character.toChars(codePoint)))
        offset += Character.charCount(codePoint)
    }
}
