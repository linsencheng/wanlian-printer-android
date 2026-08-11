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
import com.wanlian.printer.model.PrintUnits
import com.wanlian.printer.model.TextWeight
import kotlin.math.max

data class FooterLabelMetrics(
    val textHeightDots: Float,
    val totalHeightDots: Float,
    val flowerSizeDots: Float,
    val textWidthDots: Float = 0f,
    val bodyTextHeightDots: Float = 0f,
    val bodyTextWidthDots: Float = 0f,
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

        val contentHeight = bodyHeight
        val contentWidth = bodyWidth
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
        const val FLOWER_GAP_MM = 3f
    }
}
