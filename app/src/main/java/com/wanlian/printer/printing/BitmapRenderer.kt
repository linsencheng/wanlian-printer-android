package com.wanlian.printer.printing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.wanlian.printer.model.BorderPosition
import com.wanlian.printer.model.BorderStyle
import com.wanlian.printer.model.PrintSettings
import com.wanlian.printer.model.PrintUnits
import com.wanlian.printer.model.TextHorizontalAlignment
import com.wanlian.printer.model.TextWeight
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Preview and printer data are deliberately separate. [previewBitmap] simulates black
 * material with white ribbon. [printMask] uses transparent=NOT_PRINT and opaque=PRINT.
 */
data class RenderedBitmap(
    val previewBitmap: Bitmap,
    val printMask: Bitmap,
    val paperWidthMm: Float,
    val paperLengthMm: Float,
    val effectiveFontSizeDots: Float,
    val mainAreaWidthMm: Float = paperWidthMm,
)

class BitmapRenderer(
    private val borderRenderer: BorderRenderer = BorderRenderer(),
) {
    fun renderCouplet(settings: PrintSettings): RenderedBitmap {
        val paperWidthMm = settings.paperWidthMm.coerceIn(MIN_PAPER_WIDTH_MM, MAX_PAPER_WIDTH_MM)
        val widthDots = PrintUnits.mmToDots(paperWidthMm).coerceAtLeast(1)
        val borderGeometry = calculateBorderGeometry(settings, widthDots)
        val columns = normalizedColumns(settings.text)
        val textLayout = calculateTextLayout(columns, borderGeometry.textWidthDots, settings)
        val topMargin = PrintUnits.mmToDots(settings.topMarginMm.coerceAtLeast(0f))
        val bottomMargin = PrintUnits.mmToDots(settings.bottomMarginMm.coerceAtLeast(0f))
        val requiredHeight = ceil(
            topMargin + textLayout.contentHeightDots + bottomMargin,
        ).toInt().coerceAtLeast(1)
        val heightDots = if (settings.autoPaperLength) {
            requiredHeight
        } else {
            PrintUnits.mmToDots(settings.paperLengthMm.coerceIn(20f, 1500f))
        }.coerceIn(1, MAX_BITMAP_HEIGHT_DOTS)

        val surfaces = createSurfaces(widthDots, heightDots)
        drawVerticalText(
            surfaces = surfaces,
            columns = columns,
            layout = textLayout,
            settings = settings,
            areaStartX = borderGeometry.textStartX,
            areaWidthDots = borderGeometry.textWidthDots,
            topDots = topMargin.toFloat(),
        )
        drawBorders(surfaces, settings, borderGeometry, heightDots)

        return RenderedBitmap(
            previewBitmap = surfaces.preview,
            printMask = surfaces.mask,
            paperWidthMm = paperWidthMm,
            paperLengthMm = PrintUnits.dotsToMm(heightDots),
            effectiveFontSizeDots = textLayout.fontSizeDots,
            mainAreaWidthMm = PrintUnits.dotsToMm(borderGeometry.textWidthDots),
        )
    }

    fun renderTestPage(settings: PrintSettings): RenderedBitmap {
        val paperWidthMm = settings.paperWidthMm.coerceIn(MIN_PAPER_WIDTH_MM, MAX_PAPER_WIDTH_MM)
        val widthDots = PrintUnits.mmToDots(paperWidthMm)
        val heightDots = PrintUnits.mmToDots(90f)
        val surfaces = createSurfaces(widthDots, heightDots)
        val padding = PrintUnits.mmToDots(5f).toFloat()
        val printPaint = printAreaPaint()
        drawOnBoth(surfaces, printPaint) { canvas, paint ->
            canvas.drawRect(padding, padding, widthDots - padding, padding + 54f, paint)
        }

        val englishPaint = printTextPaint(min(90f, widthDots / 4.8f), TextWeight.BOLD).apply {
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val englishY = padding + 54f + 30f - englishPaint.fontMetrics.ascent
        drawTextOnBoth(surfaces, "TEST", widthDots / 2f, englishY, englishPaint)

        val chinesePaint = printTextPaint(min(80f, widthDots / 5.4f), TextWeight.BOLD)
        val chineseY = englishY + englishPaint.fontMetrics.descent + 42f - chinesePaint.fontMetrics.ascent
        drawTextOnBoth(surfaces, "中文 测试打印", widthDots / 2f, chineseY, chinesePaint)

        return RenderedBitmap(
            previewBitmap = surfaces.preview,
            printMask = surfaces.mask,
            paperWidthMm = paperWidthMm,
            paperLengthMm = 90f,
            effectiveFontSizeDots = chinesePaint.textSize,
        )
    }

    fun renderPolarityTest(settings: PrintSettings): RenderedBitmap {
        val paperWidthMm = settings.paperWidthMm.coerceIn(MIN_PAPER_WIDTH_MM, MAX_PAPER_WIDTH_MM)
        val widthDots = PrintUnits.mmToDots(paperWidthMm)
        val heightDots = PrintUnits.mmToDots(55f)
        val surfaces = createSurfaces(widthDots, heightDots)
        val padding = PrintUnits.mmToDots(4f).toFloat()

        val englishPaint = printTextPaint(min(70f, widthDots / 5.5f), TextWeight.BOLD).apply {
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val englishBaseline = padding - englishPaint.fontMetrics.ascent
        drawTextOnBoth(surfaces, "TEST", widthDots / 2f, englishBaseline, englishPaint)

        val chinesePaint = printTextPaint(min(64f, widthDots / 4.5f), TextWeight.BOLD)
        val chineseBaseline = englishBaseline + englishPaint.fontMetrics.descent +
            PrintUnits.mmToDots(3f) - chinesePaint.fontMetrics.ascent
        drawTextOnBoth(surfaces, "测试", widthDots / 2f, chineseBaseline, chinesePaint)

        val blockSize = PrintUnits.mmToDots(8f).toFloat()
        val blockTop = chineseBaseline + chinesePaint.fontMetrics.descent + PrintUnits.mmToDots(3f)
        val blockLeft = (widthDots - blockSize) / 2f
        drawOnBoth(surfaces, printAreaPaint()) { canvas, paint ->
            canvas.drawRect(blockLeft, blockTop, blockLeft + blockSize, blockTop + blockSize, paint)
        }

        return RenderedBitmap(
            previewBitmap = surfaces.preview,
            printMask = surfaces.mask,
            paperWidthMm = paperWidthMm,
            paperLengthMm = 55f,
            effectiveFontSizeDots = chinesePaint.textSize,
        )
    }

    private fun calculateTextLayout(
        columns: List<List<String>>,
        areaWidthDots: Int,
        settings: PrintSettings,
    ): VerticalTextLayout {
        val horizontalPadding = PrintUnits.mmToDots(2f)
        val availableWidth = (areaWidthDots - horizontalPadding * 2).coerceAtLeast(1)
        val widthUnits = columns.size +
            (columns.size - 1).coerceAtLeast(0) * COLUMN_GAP_RATIO
        val maximumFittingSize = availableWidth / widthUnits
        val fontSize = if (settings.autoFontSize) {
            maximumFittingSize
        } else {
            min(settings.fontSizeDots, maximumFittingSize)
        }.coerceIn(MIN_FONT_SIZE_DOTS, MAX_FONT_SIZE_DOTS)
        val paint = printTextPaint(fontSize, settings.textWeight)
        val metrics = paint.fontMetrics
        val glyphHeight = metrics.descent - metrics.ascent
        val spacing = settings.characterSpacingDots.coerceAtLeast(0f)
        val maxCharacters = columns.maxOf { it.size }
        val contentHeight = glyphHeight * maxCharacters + spacing * (maxCharacters - 1).coerceAtLeast(0)
        return VerticalTextLayout(fontSize, glyphHeight + spacing, contentHeight)
    }

    private fun calculateBorderGeometry(settings: PrintSettings, widthDots: Int): BorderGeometry {
        val borderEnabled = settings.border.style != BorderStyle.NONE
        val leftEnabled = borderEnabled && settings.border.position != BorderPosition.RIGHT
        val rightEnabled = borderEnabled && settings.border.position != BorderPosition.LEFT
        val edgeInset = PrintUnits.mmToDots(settings.border.edgeInsetMm.coerceIn(0f, 20f))
        val requestedWidth = PrintUnits.mmToDots(settings.border.widthMm.coerceIn(2f, 15f))
        val borderWidth = requestedWidth.coerceAtMost((widthDots / 4).coerceAtLeast(1))
        val textGap = PrintUnits.mmToDots(settings.border.textGapMm.coerceIn(0f, 20f))
        val leftReserved = if (leftEnabled) edgeInset + borderWidth + textGap else 0
        val rightReserved = if (rightEnabled) edgeInset + borderWidth + textGap else 0
        val textStart = leftReserved.coerceAtMost((widthDots - 1).coerceAtLeast(0))
        val textWidth = (widthDots - leftReserved - rightReserved).coerceAtLeast(1)
        val leftBounds = if (leftEnabled) {
            RectF(edgeInset.toFloat(), 0f, (edgeInset + borderWidth).toFloat(), 0f)
        } else {
            null
        }
        val rightBounds = if (rightEnabled) {
            RectF(
                (widthDots - edgeInset - borderWidth).toFloat(),
                0f,
                (widthDots - edgeInset).toFloat(),
                0f,
            )
        } else {
            null
        }
        return BorderGeometry(textStart, textWidth, leftBounds, rightBounds)
    }

    private fun drawVerticalText(
        surfaces: RenderSurfaces,
        columns: List<List<String>>,
        layout: VerticalTextLayout,
        settings: PrintSettings,
        areaStartX: Int,
        areaWidthDots: Int,
        topDots: Float,
    ) {
        val paint = printTextPaint(layout.fontSizeDots, settings.textWeight)
        val gap = layout.fontSizeDots * COLUMN_GAP_RATIO
        val groupWidth = layout.fontSizeDots * columns.size + gap * (columns.size - 1).coerceAtLeast(0)
        val groupLeft = when (settings.textAlignment) {
            TextHorizontalAlignment.LEFT -> areaStartX.toFloat()
            TextHorizontalAlignment.CENTER -> areaStartX + (areaWidthDots - groupWidth) / 2f
            TextHorizontalAlignment.RIGHT -> areaStartX + areaWidthDots - groupWidth
        }
        val rightColumnCenter = groupLeft + groupWidth - layout.fontSizeDots / 2f
        val metrics = paint.fontMetrics
        columns.forEachIndexed { columnIndex, characters ->
            val x = rightColumnCenter - columnIndex * (layout.fontSizeDots + gap)
            characters.forEachIndexed { characterIndex, character ->
                val baseline = topDots + characterIndex * layout.characterAdvanceDots - metrics.ascent
                surfaces.maskCanvas.drawText(character, x, baseline, paint)
                surfaces.previewCanvas.drawText(character, x, baseline, paint)
            }
        }
    }

    private fun drawBorders(
        surfaces: RenderSurfaces,
        settings: PrintSettings,
        geometry: BorderGeometry,
        heightDots: Int,
    ) {
        listOfNotNull(geometry.leftBounds, geometry.rightBounds).forEach { baseBounds ->
            val bounds = RectF(baseBounds.left, 0f, baseBounds.right, heightDots.toFloat())
            borderRenderer.drawBorder(
                surfaces.maskCanvas,
                settings.border.style,
                bounds,
                settings.border,
                Color.WHITE,
            )
            borderRenderer.drawBorder(
                surfaces.previewCanvas,
                settings.border.style,
                bounds,
                settings.border,
                Color.WHITE,
            )
        }
    }

    private fun normalizedColumns(text: String): List<List<String>> {
        val lines = text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()
            .ifEmpty { listOf(" ") }
        return lines.map { line ->
            val result = mutableListOf<String>()
            var offset = 0
            while (offset < line.length) {
                val codePoint = line.codePointAt(offset)
                result += String(Character.toChars(codePoint))
                offset += Character.charCount(codePoint)
            }
            result
        }
    }

    private fun printTextPaint(size: Float, weight: TextWeight) = Paint(
        Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG,
    ).apply {
        color = Color.WHITE
        textSize = size
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(
            "serif",
            if (weight == TextWeight.BOLD) Typeface.BOLD else Typeface.NORMAL,
        )
    }

    private fun printAreaPaint() = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private fun createSurfaces(width: Int, height: Int): RenderSurfaces {
        val mask = Bitmap.createBitmap(max(1, width), max(1, height), Bitmap.Config.ARGB_8888).also {
            it.eraseColor(Color.TRANSPARENT)
        }
        val preview = Bitmap.createBitmap(max(1, width), max(1, height), Bitmap.Config.ARGB_8888).also {
            it.eraseColor(Color.BLACK)
        }
        return RenderSurfaces(mask, preview, Canvas(mask), Canvas(preview))
    }

    private fun drawTextOnBoth(
        surfaces: RenderSurfaces,
        text: String,
        x: Float,
        baseline: Float,
        paint: Paint,
    ) {
        surfaces.maskCanvas.drawText(text, x, baseline, paint)
        surfaces.previewCanvas.drawText(text, x, baseline, paint)
    }

    private inline fun drawOnBoth(
        surfaces: RenderSurfaces,
        paint: Paint,
        draw: (Canvas, Paint) -> Unit,
    ) {
        draw(surfaces.maskCanvas, paint)
        draw(surfaces.previewCanvas, paint)
    }

    private data class VerticalTextLayout(
        val fontSizeDots: Float,
        val characterAdvanceDots: Float,
        val contentHeightDots: Float,
    )

    private data class BorderGeometry(
        val textStartX: Int,
        val textWidthDots: Int,
        val leftBounds: RectF?,
        val rightBounds: RectF?,
    )

    private data class RenderSurfaces(
        val mask: Bitmap,
        val preview: Bitmap,
        val maskCanvas: Canvas,
        val previewCanvas: Canvas,
    )

    companion object {
        private const val MIN_PAPER_WIDTH_MM = 30f
        private const val MAX_PAPER_WIDTH_MM = 110f
        private const val COLUMN_GAP_RATIO = 0.28f
        private const val MIN_FONT_SIZE_DOTS = 8f
        private const val MAX_FONT_SIZE_DOTS = 420f
        private const val MAX_BITMAP_HEIGHT_DOTS = 16_000
    }
}
