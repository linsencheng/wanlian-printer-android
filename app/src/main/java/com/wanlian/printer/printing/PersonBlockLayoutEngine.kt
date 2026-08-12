package com.wanlian.printer.printing

import com.wanlian.printer.model.CharacterSpacingRules
import com.wanlian.printer.model.PersonBlockSettings
import com.wanlian.printer.model.PersonPlacementMode
import com.wanlian.printer.model.PersonLayout
import com.wanlian.printer.model.PersonLayoutRules
import com.wanlian.printer.model.PrintUnits
import kotlin.math.max

data class PersonBlockBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

data class PersonBlockLayoutResult(
    val coordinates: PersonCoordinateLayout,
    val bounds: PersonBlockBounds,
    val effectiveFontSizeDots: Float,
    val wasPositionClamped: Boolean,
    val wasFontAutoReduced: Boolean,
)

/** Produces one bounded person group for both body overlay and inline text-flow placement. */
object PersonBlockLayoutEngine {
    fun layout(
        settings: PersonBlockSettings,
        safeLeft: Float,
        safeTop: Float,
        safeRight: Float,
        safeBottom: Float,
        requestedGlyphHeightDots: Float,
        requestedBaselineOffsetDots: Float,
        fontWidthScale: Float,
        inlineTop: Float? = null,
    ): PersonBlockLayoutResult? {
        if (!settings.enabled) return null
        val persons = settings.persons
            .take(PersonLayoutRules.MAX_PERSONS)
            .filter { it.verticalText.isNotBlank() }
        if (persons.isEmpty()) return null

        val left = minOf(safeLeft, safeRight)
        val right = maxOf(safeLeft, safeRight)
        val top = minOf(safeTop, safeBottom)
        val bottom = maxOf(safeTop, safeBottom)
        val safeWidth = (right - left).coerceAtLeast(1f)
        val safeHeight = (bottom - top).coerceAtLeast(1f)
        val requestedSize = settings.fontSizeDots.coerceIn(
            PersonLayoutRules.MIN_FONT_SIZE_DOTS,
            PersonLayoutRules.MAX_FONT_SIZE_DOTS,
        )
        val widthResolved = PersonLayoutRules.resolveParallelColumns(
            personCount = if (settings.layout == PersonLayout.PARALLEL_COLUMNS) persons.size else 1,
            requestedFontSizeDots = requestedSize,
            requestedColumnGapMm = settings.columnGapMm,
            availableWidthDots = safeWidth,
            fontWidthScale = fontWidthScale,
        )
        val unitCount = when (settings.layout) {
            PersonLayout.SEQUENTIAL -> persons.sumOf { it.verticalText.codePointCount(0, it.verticalText.length) }
            PersonLayout.PARALLEL_COLUMNS -> persons.maxOf {
                it.verticalText.codePointCount(0, it.verticalText.length)
            }
        }.coerceAtLeast(1)
        val glyphHeightPerSize = requestedGlyphHeightDots / requestedSize.coerceAtLeast(1f)
        val spacing = CharacterSpacingRules.clamp(settings.characterSpacingDots)
        val maximumSizeByHeight = (
            (safeHeight - spacing * (unitCount - 1).coerceAtLeast(0)) /
                (unitCount * glyphHeightPerSize).coerceAtLeast(0.001f)
            ).coerceAtLeast(8f)
        val effectiveSize = minOf(widthResolved.effectiveFontSizeDots, maximumSizeByHeight, requestedSize)
        val finalColumns = if (settings.layout == PersonLayout.PARALLEL_COLUMNS) {
            PersonLayoutRules.resolveParallelColumns(
                personCount = persons.size,
                requestedFontSizeDots = effectiveSize,
                requestedColumnGapMm = settings.columnGapMm,
                availableWidthDots = safeWidth,
                fontWidthScale = fontWidthScale,
            )
        } else {
            null
        }
        val scale = effectiveSize / requestedSize.coerceAtLeast(1f)
        val glyphHeight = requestedGlyphHeightDots * scale
        val glyphAdvance = CharacterSpacingRules.glyphAdvanceDots(glyphHeight, spacing)
        val baselineOffset = requestedBaselineOffsetDots * scale
        val sequentialWidth = effectiveSize * max(0.65f, fontWidthScale)
        val groupWidth = finalColumns?.groupWidthDots ?: sequentialWidth

        val preliminary = PersonLayoutEngine.layout(
            persons = persons,
            layout = settings.layout,
            areaStartX = 0f,
            areaWidthDots = groupWidth,
            groupTop = 0f,
            glyphAdvanceDots = glyphAdvance,
            glyphHeightDots = glyphHeight,
            baselineOffsetDots = baselineOffset,
            sequentialColumnWidthDots = sequentialWidth,
            parallelColumns = finalColumns,
            groupOffsetXDots = 0f,
        )
        val groupHeight = preliminary.groupBottom.coerceAtLeast(glyphHeight)
        val desiredLeft = when (settings.placementMode) {
            PersonPlacementMode.SIDE_OVERLAY -> {
                left + safeWidth * settings.positionXNorm.coerceIn(0f, 1f) -
                    groupWidth / 2f + PrintUnits.mmToDots(
                    settings.offsetXMm.coerceIn(
                        PersonBlockSettings.MIN_OFFSET_X_MM,
                        PersonBlockSettings.MAX_OFFSET_X_MM,
                    ),
                )
            }
            PersonPlacementMode.INLINE_INSERT -> left + (safeWidth - groupWidth) / 2f
        }
        val desiredTop = when (settings.placementMode) {
            PersonPlacementMode.SIDE_OVERLAY -> {
                top + safeHeight * settings.positionYNorm.coerceIn(0f, 1f) -
                    groupHeight / 2f + PrintUnits.mmToDots(
                    settings.offsetYMm.coerceIn(
                        PersonBlockSettings.MIN_OFFSET_Y_MM,
                        PersonBlockSettings.MAX_OFFSET_Y_MM,
                    ),
                )
            }
            PersonPlacementMode.INLINE_INSERT -> inlineTop ?: top
        }
        val clampedLeft = desiredLeft.coerceIn(left, (right - groupWidth).coerceAtLeast(left))
        val clampedTop = desiredTop.coerceIn(top, (bottom - groupHeight).coerceAtLeast(top))
        val coordinates = PersonLayoutEngine.layout(
            persons = persons,
            layout = settings.layout,
            areaStartX = clampedLeft,
            areaWidthDots = groupWidth,
            groupTop = clampedTop,
            glyphAdvanceDots = glyphAdvance,
            glyphHeightDots = glyphHeight,
            baselineOffsetDots = baselineOffset,
            sequentialColumnWidthDots = sequentialWidth,
            parallelColumns = finalColumns,
            groupOffsetXDots = 0f,
        )
        return PersonBlockLayoutResult(
            coordinates = coordinates,
            bounds = PersonBlockBounds(
                left = clampedLeft,
                top = clampedTop,
                right = clampedLeft + groupWidth,
                bottom = clampedTop + groupHeight,
            ),
            effectiveFontSizeDots = effectiveSize,
            wasPositionClamped = clampedLeft != desiredLeft || clampedTop != desiredTop,
            wasFontAutoReduced = effectiveSize < requestedSize || widthResolved.wasAutoReduced,
        )
    }
}
