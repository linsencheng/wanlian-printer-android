package com.wanlian.printer.printing

import android.graphics.Canvas
import android.graphics.RectF
import com.wanlian.printer.model.BorderSettings
import com.wanlian.printer.model.BorderTemplate

/** Uses the production renderer for a true left/right preview of every border template. */
class BorderThumbnailRenderer(
    private val borderRenderer: BorderRenderer = BorderRenderer(),
) {
    fun drawPair(
        canvas: Canvas,
        width: Float,
        height: Float,
        template: BorderTemplate,
        settings: BorderSettings,
        color: Int,
    ) {
        if (template == BorderTemplate.NONE || width <= 0f || height <= 0f) return
        val horizontalInset = width * 0.055f
        val laneWidth = width * 0.15f
        val dotsPerMm = (height / THUMBNAIL_PAPER_HEIGHT_MM).coerceAtLeast(1f)
        val thumbnailSettings = settings.copy(
            widthMm = 5f,
            edgeInsetMm = 3f,
            textGapMm = 4f,
        )
        borderRenderer.drawBorder(
            canvas = canvas,
            template = template,
            bounds = RectF(horizontalInset, 0f, horizontalInset + laneWidth, height),
            settings = thumbnailSettings,
            color = color,
            dotsPerMm = dotsPerMm,
        )
        borderRenderer.drawBorder(
            canvas = canvas,
            template = template,
            bounds = RectF(width - horizontalInset - laneWidth, 0f, width - horizontalInset, height),
            settings = thumbnailSettings,
            color = color,
            dotsPerMm = dotsPerMm,
            mirrorX = true,
        )
    }

    private companion object {
        const val THUMBNAIL_PAPER_HEIGHT_MM = 48f
    }
}
