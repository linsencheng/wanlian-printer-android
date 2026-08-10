package com.wanlian.printer.printing

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.wanlian.printer.model.BorderSettings
import com.wanlian.printer.model.BorderStyle
import com.wanlian.printer.model.PrintUnits
import kotlin.math.max

/** Draws resolution-independent, repeatable vertical border patterns with no bitmap assets. */
class BorderRenderer {
    fun drawBorder(
        canvas: Canvas,
        style: BorderStyle,
        bounds: RectF,
        settings: BorderSettings,
        color: Int,
        dotsPerMm: Float = PrintUnits.DOTS_PER_MM,
    ) {
        if (style == BorderStyle.NONE || bounds.width() <= 0f || bounds.height() <= 0f) return
        val baseStroke = max(1f, settings.strokeWidthMm * dotsPerMm)
        val unitHeight = max(bounds.width() * 1.35f, settings.patternUnitHeightMm * dotsPerMm)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            this.style = Paint.Style.STROKE
            strokeWidth = baseStroke
            strokeCap = Paint.Cap.SQUARE
            strokeJoin = Paint.Join.MITER
        }
        val saveCount = canvas.save()
        canvas.clipRect(bounds)
        when (style) {
            BorderStyle.NONE -> Unit
            BorderStyle.DOUBLE_LINE -> drawDoubleLine(canvas, bounds, paint)
            BorderStyle.THIN_GREEK_KEY -> drawGreekKey(canvas, bounds, paint, unitHeight)
            BorderStyle.THICK_GREEK_KEY -> {
                paint.strokeWidth = baseStroke * 1.75f
                drawGreekKey(canvas, bounds, paint, unitHeight * 1.1f)
            }
            BorderStyle.WAVE -> drawWave(canvas, bounds, paint, unitHeight)
            BorderStyle.CLOUD -> drawCloud(canvas, bounds, paint, unitHeight)
            BorderStyle.SCALLOP -> drawScallop(canvas, bounds, paint, unitHeight)
            BorderStyle.SWIRL -> drawSwirl(canvas, bounds, paint, unitHeight)
        }
        canvas.restoreToCount(saveCount)
    }

    private fun drawDoubleLine(canvas: Canvas, bounds: RectF, paint: Paint) {
        val leftX = bounds.left + bounds.width() * 0.28f
        val rightX = bounds.right - bounds.width() * 0.28f
        canvas.drawLine(leftX, bounds.top, leftX, bounds.bottom, paint)
        canvas.drawLine(rightX, bounds.top, rightX, bounds.bottom, paint)
    }

    private fun drawGreekKey(
        canvas: Canvas,
        bounds: RectF,
        paint: Paint,
        unitHeight: Float,
    ) {
        val inset = paint.strokeWidth / 2f
        val left = bounds.left + inset
        val right = bounds.right - inset
        val innerLeft = bounds.left + bounds.width() * 0.34f
        val innerRight = bounds.right - bounds.width() * 0.34f
        var top = bounds.top
        while (top < bounds.bottom) {
            val bottom = top + unitHeight
            val quarter = unitHeight / 4f
            val path = Path().apply {
                moveTo(left, top)
                lineTo(right, top)
                lineTo(right, top + quarter)
                lineTo(innerLeft, top + quarter)
                lineTo(innerLeft, top + quarter * 2f)
                lineTo(innerRight, top + quarter * 2f)
                lineTo(innerRight, top + quarter * 3f)
                lineTo(left, top + quarter * 3f)
                lineTo(left, bottom)
                lineTo(right, bottom)
            }
            canvas.drawPath(path, paint)
            top = bottom
        }
    }

    private fun drawWave(
        canvas: Canvas,
        bounds: RectF,
        paint: Paint,
        unitHeight: Float,
    ) {
        val centerX = bounds.centerX()
        val amplitude = bounds.width() * 0.42f
        val halfUnit = unitHeight / 2f
        val path = Path().apply { moveTo(centerX, bounds.top) }
        var y = bounds.top
        var direction = 1f
        while (y < bounds.bottom) {
            val nextY = y + halfUnit
            path.cubicTo(
                centerX + amplitude * direction,
                y + halfUnit * 0.2f,
                centerX + amplitude * direction,
                y + halfUnit * 0.8f,
                centerX,
                nextY,
            )
            direction *= -1f
            y = nextY
        }
        canvas.drawPath(path, paint)
    }

    private fun drawCloud(
        canvas: Canvas,
        bounds: RectF,
        paint: Paint,
        unitHeight: Float,
    ) {
        val left = bounds.left + paint.strokeWidth / 2f
        val right = bounds.right - paint.strokeWidth / 2f
        val center = bounds.centerX()
        var top = bounds.top
        while (top < bounds.bottom) {
            val q = unitHeight / 4f
            val path = Path().apply {
                moveTo(center, top)
                cubicTo(right, top, right, top + q, center, top + q)
                cubicTo(left, top + q, left, top + q * 2f, center, top + q * 2f)
                cubicTo(right, top + q * 2f, right, top + q * 3f, center, top + q * 3f)
                cubicTo(left, top + q * 3f, left, top + q * 4f, center, top + q * 4f)
            }
            canvas.drawPath(path, paint)
            top += unitHeight
        }
    }

    private fun drawScallop(
        canvas: Canvas,
        bounds: RectF,
        paint: Paint,
        unitHeight: Float,
    ) {
        var top = bounds.top
        while (top < bounds.bottom) {
            val oval = RectF(bounds.left, top, bounds.right, top + unitHeight)
            canvas.drawArc(oval, -90f, 180f, false, paint)
            top += unitHeight
        }
    }

    private fun drawSwirl(
        canvas: Canvas,
        bounds: RectF,
        paint: Paint,
        unitHeight: Float,
    ) {
        val center = bounds.centerX()
        val radius = bounds.width() * 0.38f
        var top = bounds.top
        while (top < bounds.bottom) {
            val middleY = top + unitHeight / 2f
            val path = Path().apply {
                moveTo(center, top)
                cubicTo(bounds.right, top + unitHeight * 0.18f, bounds.right, middleY, center, middleY)
                cubicTo(center - radius, middleY, center - radius, middleY - radius, center, middleY - radius)
                cubicTo(center + radius * 0.65f, middleY - radius, center + radius * 0.65f, middleY, center, middleY)
                cubicTo(bounds.left, middleY, bounds.left, top + unitHeight * 0.82f, center, top + unitHeight)
            }
            canvas.drawPath(path, paint)
            top += unitHeight
        }
    }
}
