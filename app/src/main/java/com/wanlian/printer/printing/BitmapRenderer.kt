package com.wanlian.printer.printing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.wanlian.printer.model.BorderPosition
import com.wanlian.printer.model.BorderTemplate
import com.wanlian.printer.model.CoupletPairDocument
import com.wanlian.printer.model.PairLayoutRules
import com.wanlian.printer.model.PrintSettings
import com.wanlian.printer.model.PrintLayoutRules
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

data class RenderedCoupletPair(
    val left: RenderedBitmap,
    val right: RenderedBitmap,
    val paperLengthMm: Float,
)

class BitmapRenderer(
    private val borderRenderer: BorderRenderer = BorderRenderer(),
    private val footerLabelRenderer: FooterLabelRenderer = FooterLabelRenderer(),
    private val cutGuideRenderer: CutGuideRenderer = CutGuideRenderer(),
) {
    fun requiredCoupletLengthMm(settings: PrintSettings): Float {
        val paperWidthMm = settings.paperWidthMm.coerceIn(MIN_PAPER_WIDTH_MM, MAX_PAPER_WIDTH_MM)
        val widthDots = PrintUnits.mmToDots(paperWidthMm).coerceAtLeast(1)
        val borderGeometry = calculateBorderGeometry(settings, widthDots)
        val columns = normalizedColumns(settings.text)
        val textLayout = calculateTextLayout(columns, borderGeometry.textWidthDots, settings)
        val mainContentBottom = PrintUnits.mmToDots(settings.topMarginMm.coerceAtLeast(0f)) +
            textLayout.contentHeightDots
        val footerMetrics = footerLabelRenderer.measure(
            settings.footerLabel,
            borderGeometry.textWidthDots.toFloat(),
        )
        val footerDistance = if (settings.footerLabel.enabled) {
            PrintUnits.mmToDots(settings.footerLabel.distanceFromMainMm.coerceAtLeast(0f))
        } else {
            0
        }
        val footerBottomMargin = if (settings.footerLabel.enabled) {
            PrintUnits.mmToDots(settings.footerLabel.bottomMarginMm.coerceAtLeast(0f))
        } else {
            0
        }
        val requiredDots = ceil(
            mainContentBottom + footerDistance + footerMetrics.totalHeightDots +
                footerBottomMargin + PrintUnits.mmToDots(settings.bottomMarginMm.coerceAtLeast(0f)) +
                PrintUnits.mmToDots(PrintLayoutRules.cutGuideReserveMm(settings.cutGuide)),
        ).toInt().coerceAtLeast(1)
        return PrintUnits.dotsToMm(requiredDots)
    }

    fun renderCoupletPair(document: CoupletPairDocument): RenderedCoupletPair {
        val pairLength = resolvePairPaperLengthMm(document)
        return RenderedCoupletPair(
            left = renderCouplet(document.left, forcedPaperLengthMm = pairLength),
            right = renderCouplet(document.right, forcedPaperLengthMm = pairLength),
            paperLengthMm = pairLength,
        )
    }

    fun resolvePairPaperLengthMm(document: CoupletPairDocument): Float {
        val leftRequired = if (document.left.autoPaperLength) {
            requiredCoupletLengthMm(document.left)
        } else {
            document.left.paperLengthMm
        }
        val rightRequired = if (document.right.autoPaperLength) {
            requiredCoupletLengthMm(document.right)
        } else {
            document.right.paperLengthMm
        }
        val preferred = if (document.left.autoPaperLength || document.right.autoPaperLength) {
            max(document.left.preferredAutoLengthMm, document.right.preferredAutoLengthMm)
        } else {
            1f
        }
        return PairLayoutRules.resolveSideLengthsMm(leftRequired, rightRequired, preferred).leftMm
    }

    fun renderCouplet(
        settings: PrintSettings,
        forcedPaperLengthMm: Float? = null,
    ): RenderedBitmap {
        val paperWidthMm = settings.paperWidthMm.coerceIn(MIN_PAPER_WIDTH_MM, MAX_PAPER_WIDTH_MM)
        val widthDots = PrintUnits.mmToDots(paperWidthMm).coerceAtLeast(1)
        val borderGeometry = calculateBorderGeometry(settings, widthDots)
        val columns = normalizedColumns(settings.text)
        val textLayout = calculateTextLayout(columns, borderGeometry.textWidthDots, settings)
        val topMargin = PrintUnits.mmToDots(settings.topMarginMm.coerceAtLeast(0f))
        val bottomMargin = PrintUnits.mmToDots(settings.bottomMarginMm.coerceAtLeast(0f))
        val mainContentBottom = topMargin + textLayout.contentHeightDots
        val footerMetrics = footerLabelRenderer.measure(
            settings.footerLabel,
            borderGeometry.textWidthDots.toFloat(),
        )
        val footerDistance = if (settings.footerLabel.enabled) {
            PrintUnits.mmToDots(settings.footerLabel.distanceFromMainMm.coerceAtLeast(0f))
        } else {
            0
        }
        val footerBottomMargin = if (settings.footerLabel.enabled) {
            PrintUnits.mmToDots(settings.footerLabel.bottomMarginMm.coerceAtLeast(0f))
        } else {
            0
        }
        val cutGuideReserve = PrintUnits.mmToDots(
            PrintLayoutRules.cutGuideReserveMm(settings.cutGuide),
        )
        val requiredHeight = ceil(
            mainContentBottom +
                footerDistance +
                footerMetrics.totalHeightDots +
                footerBottomMargin +
                bottomMargin +
                cutGuideReserve,
        ).toInt().coerceAtLeast(1)
        val heightDots = if (forcedPaperLengthMm != null) {
            PrintUnits.mmToDots(forcedPaperLengthMm.coerceIn(20f, 1500f))
        } else if (settings.autoPaperLength) {
            val requiredLengthMm = PrintUnits.dotsToMm(requiredHeight)
            PrintUnits.mmToDots(
                PrintLayoutRules.resolveAutoLengthMm(
                    requiredLengthMm = requiredLengthMm,
                    preferredAutoLengthMm = settings.preferredAutoLengthMm,
                ),
            )
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
        if (settings.footerLabel.enabled) {
            val reservedBelowFooter = footerBottomMargin + bottomMargin + cutGuideReserve
            val preferredFooterTop = heightDots - reservedBelowFooter - footerMetrics.totalHeightDots
            val minimumFooterTop = mainContentBottom + footerDistance
            val footerTop = max(preferredFooterTop, minimumFooterTop)
            drawFooterLabel(
                surfaces = surfaces,
                settings = settings,
                geometry = borderGeometry,
                topDots = footerTop,
            )
        }
        drawCutGuide(surfaces, settings, widthDots, heightDots)

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
        val fontWidthScale = max(0.65f, FontRepository.resolve(settings.fontId).textScaleX)
        val maximumFittingSize = availableWidth / (widthUnits * fontWidthScale)
        val fontSize = if (settings.autoFontSize) {
            maximumFittingSize * AUTO_FONT_SIZE_SCALE
        } else {
            min(settings.fontSizeDots, maximumFittingSize)
        }.coerceIn(MIN_FONT_SIZE_DOTS, MAX_FONT_SIZE_DOTS)
        val paint = printTextPaint(fontSize, settings.textWeight, settings.fontId)
        val metrics = paint.fontMetrics
        val glyphHeight = metrics.descent - metrics.ascent
        val spacing = settings.characterSpacingDots.coerceAtLeast(0f)
        val maxCharacters = columns.maxOf { it.size }
        val contentHeight = glyphHeight * maxCharacters + spacing * (maxCharacters - 1).coerceAtLeast(0)
        return VerticalTextLayout(fontSize, glyphHeight + spacing, contentHeight)
    }

    private fun calculateBorderGeometry(settings: PrintSettings, widthDots: Int): BorderGeometry {
        val borderEnabled = settings.border.style != BorderTemplate.NONE
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
        val paint = printTextPaint(layout.fontSizeDots, settings.textWeight, settings.fontId)
        val glyphWidth = layout.fontSizeDots * max(0.65f, FontRepository.resolve(settings.fontId).textScaleX)
        val gap = layout.fontSizeDots * COLUMN_GAP_RATIO
        val groupWidth = glyphWidth * columns.size + gap * (columns.size - 1).coerceAtLeast(0)
        val groupLeft = when (settings.textAlignment) {
            TextHorizontalAlignment.LEFT -> areaStartX.toFloat()
            TextHorizontalAlignment.CENTER -> areaStartX + (areaWidthDots - groupWidth) / 2f
            TextHorizontalAlignment.RIGHT -> areaStartX + areaWidthDots - groupWidth
        }
        val rightColumnCenter = groupLeft + groupWidth - glyphWidth / 2f
        val metrics = paint.fontMetrics
        columns.forEachIndexed { columnIndex, characters ->
            val x = rightColumnCenter - columnIndex * (glyphWidth + gap)
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
        listOfNotNull(
            geometry.leftBounds?.let { it to false },
            geometry.rightBounds?.let { it to true },
        ).forEach { (baseBounds, mirrorX) ->
            val bounds = RectF(baseBounds.left, 0f, baseBounds.right, heightDots.toFloat())
            borderRenderer.drawBorder(
                surfaces.maskCanvas,
                settings.border.style,
                bounds,
                settings.border,
                Color.WHITE,
                mirrorX = mirrorX,
            )
            borderRenderer.drawBorder(
                surfaces.previewCanvas,
                settings.border.style,
                bounds,
                settings.border,
                Color.WHITE,
                mirrorX = mirrorX,
            )
        }
    }

    private fun drawFooterLabel(
        surfaces: RenderSurfaces,
        settings: PrintSettings,
        geometry: BorderGeometry,
        topDots: Float,
    ) {
        footerLabelRenderer.draw(
            canvas = surfaces.maskCanvas,
            settings = settings.footerLabel,
            areaStartX = geometry.textStartX.toFloat(),
            areaWidthDots = geometry.textWidthDots.toFloat(),
            topDots = topDots,
        )
        footerLabelRenderer.draw(
            canvas = surfaces.previewCanvas,
            settings = settings.footerLabel,
            areaStartX = geometry.textStartX.toFloat(),
            areaWidthDots = geometry.textWidthDots.toFloat(),
            topDots = topDots,
        )
    }

    private fun drawCutGuide(
        surfaces: RenderSurfaces,
        settings: PrintSettings,
        widthDots: Int,
        heightDots: Int,
    ) {
        cutGuideRenderer.draw(surfaces.maskCanvas, widthDots, heightDots, settings.cutGuide)
        cutGuideRenderer.draw(surfaces.previewCanvas, widthDots, heightDots, settings.cutGuide)
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

    private fun printTextPaint(
        size: Float,
        weight: TextWeight,
        fontId: String = "song",
    ): Paint = FontRepository.createPaint(fontId, size, weight)

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
        private const val AUTO_FONT_SIZE_SCALE = 0.92f
        private const val MIN_FONT_SIZE_DOTS = 8f
        private const val MAX_FONT_SIZE_DOTS = 420f
        private const val MAX_BITMAP_HEIGHT_DOTS = 16_000
    }
}
