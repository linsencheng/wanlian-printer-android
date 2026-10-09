package com.wanlian.printer.printing

import android.graphics.Canvas
import android.graphics.Color
import com.wanlian.printer.model.PersonBlockSettings
import com.wanlian.printer.model.TextWeight
import kotlin.math.max

class PersonBlockRenderer {
    fun layout(
        settings: PersonBlockSettings,
        safeLeft: Float,
        safeTop: Float,
        safeRight: Float,
        safeBottom: Float,
        inlineTop: Float? = null,
    ): PersonBlockLayoutResult? {
        val requestedPaint = FontRepository.createPaint(
            settings.fontId,
            settings.fontSizeDots,
            TextWeight.NORMAL,
        )
        val metrics = requestedPaint.fontMetrics
        return PersonBlockLayoutEngine.layout(
            settings = settings,
            safeLeft = safeLeft,
            safeTop = safeTop,
            safeRight = safeRight,
            safeBottom = safeBottom,
            requestedGlyphHeightDots = metrics.descent - metrics.ascent,
            requestedBaselineOffsetDots = -metrics.ascent,
            fontWidthScale = max(0.65f, FontRepository.resolve(settings.fontId).textScaleX),
            inlineTop = inlineTop,
        )
    }

    fun naturalHeightDots(settings: PersonBlockSettings, availableWidthDots: Float): Float =
        layout(
            settings = settings,
            safeLeft = 0f,
            safeTop = 0f,
            safeRight = availableWidthDots,
            safeBottom = MAX_MEASURE_HEIGHT_DOTS,
            inlineTop = 0f,
        )?.bounds?.height ?: 0f

    fun draw(
        canvas: Canvas,
        settings: PersonBlockSettings,
        layout: PersonBlockLayoutResult,
        color: Int = Color.WHITE,
    ) {
        val paint = FontRepository.createPaint(
            settings.fontId,
            layout.effectiveFontSizeDots,
            TextWeight.NORMAL,
            color = color,
        )
        layout.coordinates.glyphs.forEach { glyph ->
            canvas.drawText(glyph.text, glyph.centerX, glyph.baselineY, paint)
        }
    }

    private companion object {
        const val MAX_MEASURE_HEIGHT_DOTS = 1_000_000f
    }
}
