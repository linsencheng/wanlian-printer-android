package com.wanlian.printer.printing

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.wanlian.printer.model.FlowerAdjustmentLimits
import com.wanlian.printer.model.FlowerStyle
import com.wanlian.printer.model.FooterLabelPosition
import com.wanlian.printer.model.FooterLabelSettings
import com.wanlian.printer.model.FooterTextLayoutRules
import com.wanlian.printer.model.FooterTextOrientation
import com.wanlian.printer.model.PersonLayout
import com.wanlian.printer.model.PersonLayoutRules
import com.wanlian.printer.model.PrintUnits
import com.wanlian.printer.model.ResolvedPersonColumns
import com.wanlian.printer.model.TextWeight
import kotlin.math.max

data class FooterLabelMetrics(
    val textHeightDots: Float,
    val totalHeightDots: Float,
    val flowerSizeDots: Float,
    val textWidthDots: Float = 0f,
    val bodyTextHeightDots: Float = 0f,
    val bodyTextWidthDots: Float = 0f,
    val personTopGapDots: Float = 0f,
    val personGroupTopDots: Float = 0f,
    val personGroupBottomDots: Float = 0f,
    val personHeightDots: Float = 0f,
    val personWidthDots: Float = 0f,
    val effectivePersonFontSizeDots: Float = 0f,
    val resolvedPersonColumns: ResolvedPersonColumns? = null,
)

class FooterLabelRenderer(
    private val flowerRenderer: FlowerRenderer = FlowerRenderer(),
) {
    fun measure(
        settings: FooterLabelSettings,
        availableWidthDots: Float = PrintUnits.mmToDots(100f).toFloat(),
    ): FooterLabelMetrics {
        if (!settings.enabled) return FooterLabelMetrics(0f, 0f, 0f)

        val hasBodyText = settings.text.isNotBlank() || settings.secondaryText.isNotBlank()
        val bodyLines = if (hasBodyText) FooterTextLayoutRules.lines(settings) else emptyList()
        val bodyPaint = FontRepository.createPaint(
            settings.fontId,
            settings.fontSizeDots.coerceAtLeast(8f),
            TextWeight.NORMAL,
        )
        val bodySpacing = settings.spacingDots.coerceAtLeast(0f)
        val bodyDefinition = FontRepository.resolve(settings.fontId)
        val bodyGlyphHeight = bodyPaint.fontMetrics.descent - bodyPaint.fontMetrics.ascent
        val (bodyHeight, bodyWidth) = when {
            bodyLines.isEmpty() -> 0f to 0f
            settings.orientation == FooterTextOrientation.VERTICAL -> {
                val maxCharacters = bodyLines.maxOf { it.codePointCount(0, it.length) }.coerceAtLeast(1)
                val height = bodyGlyphHeight * maxCharacters +
                    bodySpacing * (maxCharacters - 1).coerceAtLeast(0)
                val columnWidth = bodyPaint.textSize * max(0.65f, bodyDefinition.textScaleX)
                val width = bodyLines.size * columnWidth +
                    (bodyLines.size - 1).coerceAtLeast(0) * bodyPaint.textSize * 0.24f
                height to width
            }
            else -> {
                val height = bodyGlyphHeight * bodyLines.size +
                    bodySpacing * (bodyLines.size - 1).coerceAtLeast(0)
                height to bodyLines.maxOf(bodyPaint::measureText)
            }
        }

        val persons = settings.persons
            .take(PersonLayoutRules.MAX_PERSONS)
            .filter { it.verticalText.isNotBlank() }
        val personTopGap = if (hasBodyText && persons.isNotEmpty()) {
            PrintUnits.mmToDots(PERSON_TOP_GAP_MM).toFloat()
        } else {
            0f
        }
        val personDefinition = FontRepository.resolve(settings.personFontId)
        val requestedPersonSize = settings.personFontSizeDots.coerceIn(
            PersonLayoutRules.MIN_FONT_SIZE_DOTS,
            PersonLayoutRules.MAX_FONT_SIZE_DOTS,
        )
        val resolvedColumns = if (persons.isNotEmpty() && settings.personLayout == PersonLayout.PARALLEL_COLUMNS) {
            PersonLayoutRules.resolveParallelColumns(
                personCount = persons.size,
                requestedFontSizeDots = requestedPersonSize,
                requestedColumnGapMm = settings.personColumnGapMm,
                availableWidthDots = availableWidthDots,
                fontWidthScale = personDefinition.textScaleX,
            )
        } else {
            null
        }
        val effectivePersonSize = resolvedColumns?.effectiveFontSizeDots ?: requestedPersonSize
        val personPaint = FontRepository.createPaint(
            settings.personFontId,
            effectivePersonSize,
            TextWeight.NORMAL,
        )
        val personGlyphHeight = personPaint.fontMetrics.descent - personPaint.fontMetrics.ascent
        val personSpacing = settings.spacingDots.coerceAtLeast(0f)
        val (personHeight, personWidth) = when {
            persons.isEmpty() -> 0f to 0f
            settings.personLayout == PersonLayout.SEQUENTIAL -> {
                val characterCount = persons.sumOf { it.verticalText.codePointCount(0, it.verticalText.length) }
                    .coerceAtLeast(1)
                val height = personGlyphHeight * characterCount +
                    personSpacing * (characterCount - 1).coerceAtLeast(0)
                height to effectivePersonSize * max(0.65f, personDefinition.textScaleX)
            }
            else -> {
                val maximumCharacters = persons.maxOf {
                    it.verticalText.codePointCount(0, it.verticalText.length)
                }.coerceAtLeast(1)
                val height = personGlyphHeight * maximumCharacters +
                    personSpacing * (maximumCharacters - 1).coerceAtLeast(0)
                height to requireNotNull(resolvedColumns).groupWidthDots
            }
        }

        val personGroupTop = if (persons.isEmpty()) {
            bodyHeight
        } else {
            bodyHeight + personTopGap + PrintUnits.mmToDots(
                settings.personGroupOffsetYMm.coerceIn(
                    PersonLayoutRules.MIN_GROUP_OFFSET_MM,
                    PersonLayoutRules.MAX_GROUP_OFFSET_MM,
                ),
            )
        }
        val personGroupBottom = personGroupTop + personHeight
        val contentHeight = max(bodyHeight, personGroupBottom).coerceAtLeast(0f)
        val contentWidth = max(bodyWidth, personWidth)
        val flowerSize = if (!settings.flower.enabled || settings.flower.style == FlowerStyle.NONE) {
            0f
        } else {
            PrintUnits.mmToDots(
                settings.flower.sizeMm.coerceIn(
                    FlowerAdjustmentLimits.MIN_SIZE_MM,
                    FlowerAdjustmentLimits.MAX_SIZE_MM,
                ),
            ).toFloat()
        }
        val flowerGap = if (flowerSize > 0f) PrintUnits.mmToDots(FLOWER_GAP_MM).toFloat() else 0f
        val positiveFlowerOffset = if (flowerSize > 0f) {
            PrintUnits.mmToDots(
                settings.flower.offsetYmm.coerceIn(0f, FlowerAdjustmentLimits.MAX_OFFSET_MM),
            ).toFloat()
        } else {
            0f
        }
        return FooterLabelMetrics(
            textHeightDots = contentHeight,
            totalHeightDots = contentHeight + flowerGap + flowerSize + positiveFlowerOffset,
            flowerSizeDots = flowerSize,
            textWidthDots = contentWidth,
            bodyTextHeightDots = bodyHeight,
            bodyTextWidthDots = bodyWidth,
            personTopGapDots = personTopGap,
            personGroupTopDots = personGroupTop,
            personGroupBottomDots = personGroupBottom,
            personHeightDots = personHeight,
            personWidthDots = personWidth,
            effectivePersonFontSizeDots = if (persons.isEmpty()) 0f else effectivePersonSize,
            resolvedPersonColumns = resolvedColumns,
        )
    }

    fun draw(
        canvas: Canvas,
        settings: FooterLabelSettings,
        areaStartX: Float,
        areaWidthDots: Float,
        topDots: Float,
        color: Int = Color.WHITE,
    ) {
        if (!settings.enabled) return
        val measured = measure(settings, areaWidthDots)
        drawBodyText(canvas, settings, measured, areaStartX, areaWidthDots, topDots, color)
        drawPersons(canvas, settings, measured, areaStartX, areaWidthDots, topDots, color)
        drawFlower(canvas, settings, measured, areaStartX, areaWidthDots, topDots, color)
    }

    private fun drawBodyText(
        canvas: Canvas,
        settings: FooterLabelSettings,
        measured: FooterLabelMetrics,
        areaStartX: Float,
        areaWidthDots: Float,
        topDots: Float,
        color: Int,
    ) {
        if (measured.bodyTextHeightDots <= 0f) return
        val lines = FooterTextLayoutRules.lines(settings)
        val columns = lines.map(::codePoints)
        val fontSize = settings.fontSizeDots.coerceAtLeast(8f)
        val paint = FontRepository.createPaint(
            settings.fontId,
            fontSize,
            TextWeight.NORMAL,
            color = color,
        )
        val definition = FontRepository.resolve(settings.fontId)
        val columnWidth = fontSize * max(0.65f, definition.textScaleX)
        val columnGap = fontSize * 0.24f
        val left = alignedLeft(
            settings.position,
            areaStartX,
            areaWidthDots,
            measured.bodyTextWidthDots,
        )
        val fontMetrics = paint.fontMetrics
        val glyphAdvance = fontMetrics.descent - fontMetrics.ascent + settings.spacingDots.coerceAtLeast(0f)
        when (settings.orientation) {
            FooterTextOrientation.VERTICAL -> {
                val rightCenter = left + measured.bodyTextWidthDots - columnWidth / 2f
                columns.forEachIndexed { columnIndex, characters ->
                    val x = rightCenter - columnIndex * (columnWidth + columnGap)
                    characters.forEachIndexed { characterIndex, character ->
                        val baseline = topDots + characterIndex * glyphAdvance - fontMetrics.ascent
                        canvas.drawText(character, x, baseline, paint)
                    }
                }
            }
            FooterTextOrientation.HORIZONTAL -> {
                val centerX = left + measured.bodyTextWidthDots / 2f
                lines.forEachIndexed { lineIndex, line ->
                    val baseline = topDots + lineIndex * glyphAdvance - fontMetrics.ascent
                    canvas.drawText(line, centerX, baseline, paint)
                }
            }
        }
    }

    private fun drawPersons(
        canvas: Canvas,
        settings: FooterLabelSettings,
        measured: FooterLabelMetrics,
        areaStartX: Float,
        areaWidthDots: Float,
        topDots: Float,
        color: Int,
    ) {
        val persons = settings.persons
            .take(PersonLayoutRules.MAX_PERSONS)
            .filter { it.verticalText.isNotBlank() }
        if (persons.isEmpty() || measured.personHeightDots <= 0f) return
        val paint = FontRepository.createPaint(
            settings.personFontId,
            measured.effectivePersonFontSizeDots,
            TextWeight.NORMAL,
            color = color,
        )
        val metrics = paint.fontMetrics
        val glyphAdvance = metrics.descent - metrics.ascent + settings.spacingDots.coerceAtLeast(0f)
        val personLayout = PersonLayoutEngine.layout(
            persons = persons,
            layout = settings.personLayout,
            areaStartX = areaStartX,
            areaWidthDots = areaWidthDots,
            groupTop = topDots + measured.personGroupTopDots,
            glyphAdvanceDots = glyphAdvance,
            glyphHeightDots = metrics.descent - metrics.ascent,
            baselineOffsetDots = -metrics.ascent,
            sequentialColumnWidthDots = measured.personWidthDots,
            parallelColumns = measured.resolvedPersonColumns,
            groupOffsetXDots = PrintUnits.mmToDots(
                settings.personGroupOffsetXMm.coerceIn(
                    PersonLayoutRules.MIN_GROUP_OFFSET_MM,
                    PersonLayoutRules.MAX_GROUP_OFFSET_MM,
                ),
            ).toFloat(),
        )
        personLayout.glyphs.forEach { glyph ->
            canvas.drawText(glyph.text, glyph.centerX, glyph.baselineY, paint)
        }
    }

    private fun drawFlower(
        canvas: Canvas,
        settings: FooterLabelSettings,
        measured: FooterLabelMetrics,
        areaStartX: Float,
        areaWidthDots: Float,
        topDots: Float,
        color: Int,
    ) {
        if (measured.flowerSizeDots <= 0f) return
        val contentWidth = measured.textWidthDots.coerceAtLeast(measured.flowerSizeDots)
        val contentLeft = alignedLeft(settings.position, areaStartX, areaWidthDots, contentWidth)
        val flowerTop = topDots + measured.textHeightDots + PrintUnits.mmToDots(FLOWER_GAP_MM) +
            PrintUnits.mmToDots(
                settings.flower.offsetYmm.coerceIn(
                    FlowerAdjustmentLimits.MIN_OFFSET_MM,
                    FlowerAdjustmentLimits.MAX_OFFSET_MM,
                ),
            )
        val flowerCenterX = contentLeft + contentWidth / 2f +
            PrintUnits.mmToDots(
                settings.flower.offsetXmm.coerceIn(
                    FlowerAdjustmentLimits.MIN_OFFSET_MM,
                    FlowerAdjustmentLimits.MAX_OFFSET_MM,
                ),
            )
        canvas.save()
        if (settings.flower.rotationDegrees != 0f) {
            canvas.rotate(
                settings.flower.rotationDegrees.coerceIn(
                    FlowerAdjustmentLimits.MIN_ROTATION_DEGREES,
                    FlowerAdjustmentLimits.MAX_ROTATION_DEGREES,
                ),
                flowerCenterX,
                flowerTop + measured.flowerSizeDots / 2f,
            )
        }
        flowerRenderer.draw(
            canvas = canvas,
            style = settings.flower.style,
            centerX = flowerCenterX,
            topY = flowerTop,
            sizeDots = measured.flowerSizeDots,
            sourcePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color },
        )
        canvas.restore()
    }

    private fun alignedLeft(
        position: FooterLabelPosition,
        areaStartX: Float,
        areaWidthDots: Float,
        contentWidth: Float,
    ): Float = when (position) {
        FooterLabelPosition.LEFT -> areaStartX
        FooterLabelPosition.CENTER -> areaStartX + (areaWidthDots - contentWidth) / 2f
        FooterLabelPosition.RIGHT -> areaStartX + areaWidthDots - contentWidth
    }

    private fun codePoints(text: String): List<String> = buildList {
        var offset = 0
        while (offset < text.length) {
            val codePoint = text.codePointAt(offset)
            add(String(Character.toChars(codePoint)))
            offset += Character.charCount(codePoint)
        }
    }

    private companion object {
        const val PERSON_TOP_GAP_MM = 3f
        const val FLOWER_GAP_MM = 3f
    }
}
