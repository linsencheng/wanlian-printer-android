package com.wanlian.printer.model

import java.util.UUID
import kotlin.math.max
import kotlin.math.min

data class FooterPerson(
    val relation: String = "",
    val name: String = "",
    val id: String = UUID.randomUUID().toString(),
) {
    val verticalText: String get() = relation.trim() + name.trim()
}

enum class PersonLayout(val label: String) {
    SEQUENTIAL("依次"),
    PARALLEL_COLUMNS("并排"),
}

data class ResolvedPersonColumns(
    val effectiveFontSizeDots: Float,
    val columnWidthDots: Float,
    val columnGapDots: Float,
    val groupWidthDots: Float,
    val centersFromGroupStart: List<Float>,
    val wasAutoReduced: Boolean,
)

object PersonLayoutRules {
    const val MIN_PERSONS = 1
    const val MAX_PERSONS = 6
    const val MIN_FONT_SIZE_DOTS = 20f
    const val MAX_FONT_SIZE_DOTS = 120f
    const val MIN_COLUMN_GAP_MM = 0f
    const val MAX_COLUMN_GAP_MM = 20f
    const val MIN_GROUP_OFFSET_MM = -100f
    const val MAX_GROUP_OFFSET_MM = 100f

    fun resolveParallelColumns(
        personCount: Int,
        requestedFontSizeDots: Float,
        requestedColumnGapMm: Float,
        availableWidthDots: Float,
        fontWidthScale: Float,
    ): ResolvedPersonColumns {
        val count = personCount.coerceIn(MIN_PERSONS, MAX_PERSONS)
        val available = availableWidthDots.coerceAtLeast(1f)
        val scale = max(0.65f, fontWidthScale)
        val requestedSize = requestedFontSizeDots.coerceIn(MIN_FONT_SIZE_DOTS, MAX_FONT_SIZE_DOTS)
        val requestedGap = PrintUnits.mmToDots(
            requestedColumnGapMm.coerceIn(MIN_COLUMN_GAP_MM, MAX_COLUMN_GAP_MM),
        ).toFloat()
        val requestedColumnWidth = requestedSize * scale
        val maximumGapAtRequestedSize = if (count == 1) {
            0f
        } else {
            ((available - count * requestedColumnWidth) / (count - 1)).coerceAtLeast(0f)
        }
        var gap = if (count == 1) 0f else min(requestedGap, maximumGapAtRequestedSize)
        var effectiveSize = requestedSize
        var columnWidth = effectiveSize * scale
        if (count * columnWidth + (count - 1) * gap > available) {
            effectiveSize = ((available - (count - 1) * gap) / (count * scale))
                .coerceIn(8f, requestedSize)
            columnWidth = effectiveSize * scale
        }
        if (count * columnWidth + (count - 1) * gap > available) {
            gap = 0f
            effectiveSize = (available / (count * scale)).coerceIn(8f, requestedSize)
            columnWidth = effectiveSize * scale
        }
        val groupWidth = count * columnWidth + (count - 1) * gap
        val centers = List(count) { index ->
            columnWidth / 2f + index * (columnWidth + gap)
        }
        return ResolvedPersonColumns(
            effectiveFontSizeDots = effectiveSize,
            columnWidthDots = columnWidth,
            columnGapDots = gap,
            groupWidthDots = groupWidth,
            centersFromGroupStart = centers,
            wasAutoReduced = effectiveSize < requestedSize || gap < requestedGap,
        )
    }
}

/** Pure snapshot used by the local template repository for lossless person-layout persistence. */
data class PersonLayoutState(
    val persons: List<FooterPerson>,
    val layout: PersonLayout,
    val columnGapMm: Float,
    val groupOffsetXMm: Float,
    val groupOffsetYMm: Float,
    val fontId: String,
    val fontSizeDots: Float,
)

fun FooterLabelSettings.personLayoutState(): PersonLayoutState = PersonLayoutState(
    persons = persons,
    layout = personLayout,
    columnGapMm = personColumnGapMm,
    groupOffsetXMm = personGroupOffsetXMm,
    groupOffsetYMm = personGroupOffsetYMm,
    fontId = personFontId,
    fontSizeDots = personFontSizeDots,
)

fun FooterLabelSettings.withPersonLayoutState(state: PersonLayoutState): FooterLabelSettings = copy(
    persons = state.persons,
    personLayout = state.layout,
    personColumnGapMm = state.columnGapMm,
    personGroupOffsetXMm = state.groupOffsetXMm,
    personGroupOffsetYMm = state.groupOffsetYMm,
    personFontId = state.fontId,
    personFontSizeDots = state.fontSizeDots,
)
