package com.wanlian.printer.printing

import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.wanlian.printer.model.BorderSettings
import com.wanlian.printer.model.BorderTemplate
import com.wanlian.printer.model.PrintUnits
import kotlin.math.max

/** Draws clear, resolution-independent vertical ornaments without bitmap assets. */
class BorderRenderer {
    fun drawBorder(
        canvas: Canvas,
        template: BorderTemplate,
        bounds: RectF,
        settings: BorderSettings,
        color: Int,
        dotsPerMm: Float = PrintUnits.DOTS_PER_MM,
        mirrorX: Boolean = false,
    ) {
        if (template == BorderTemplate.NONE || bounds.width() <= 0f || bounds.height() <= 0f) return
        val baseStroke = max(1f, settings.strokeWidthMm * dotsPerMm * template.strokeScale)
        val unitHeight = max(
            bounds.width() * 1.15f,
            settings.patternUnitHeightMm * dotsPerMm * template.unitHeightScale,
        )
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = baseStroke
            strokeCap = Paint.Cap.SQUARE
            strokeJoin = Paint.Join.MITER
        }
        val saveCount = canvas.save()
        canvas.clipRect(bounds)
        if (mirrorX) canvas.scale(-1f, 1f, bounds.centerX(), bounds.centerY())
        when (template) {
            BorderTemplate.NONE -> Unit
            BorderTemplate.DOUBLE_LINE -> drawParallelLines(canvas, bounds, paint, 2)
            BorderTemplate.TRIPLE_LINE -> drawParallelLines(canvas, bounds, paint, 3)
            BorderTemplate.DOUBLE_LINE_WITH_DOTS -> drawLineAndDots(canvas, bounds, paint, unitHeight)
            BorderTemplate.THICK_DOUBLE_LINE -> drawParallelLines(canvas, bounds, paint, 2)
            BorderTemplate.SINGLE_LINE -> drawParallelLines(canvas, bounds, paint, 1)
            BorderTemplate.THIN_THICK_LINE -> drawThinThickLines(canvas, bounds, paint)
            BorderTemplate.FOUR_LINE -> drawParallelLines(canvas, bounds, paint, 4)
            BorderTemplate.DASHED_DOUBLE_LINE -> drawParallelLines(
                canvas,
                bounds,
                Paint(paint).apply {
                    pathEffect = DashPathEffect(floatArrayOf(baseStroke * 5f, baseStroke * 3f), 0f)
                },
                2,
            )
            BorderTemplate.LINE_BEAD_CHAIN -> drawLineBeadChain(canvas, bounds, paint, unitHeight)

            BorderTemplate.THIN_GREEK_KEY,
            BorderTemplate.THICK_GREEK_KEY,
            -> drawGreekKey(canvas, bounds, paint, unitHeight)
            BorderTemplate.BROKEN_KEY -> drawGreekKey(
                canvas,
                bounds,
                Paint(paint).apply {
                    pathEffect = DashPathEffect(floatArrayOf(baseStroke * 4f, baseStroke * 2.5f), 0f)
                },
                unitHeight,
            )
            BorderTemplate.INNER_OUTER_KEY -> drawInnerOuterKey(canvas, bounds, paint, unitHeight)
            BorderTemplate.DENSE_KEY_PATTERN -> drawDenseKey(canvas, bounds, paint, unitHeight)
            BorderTemplate.GREEK_KEY_SMALL,
            BorderTemplate.GREEK_KEY_OPEN,
            -> drawGreekKey(canvas, bounds, paint, unitHeight)
            BorderTemplate.GREEK_KEY_DOUBLE_ROW -> drawDoubleRowGreekKey(canvas, bounds, paint, unitHeight)
            BorderTemplate.GREEK_KEY_SQUARE -> drawSquareGreekKey(canvas, bounds, paint, unitHeight)
            BorderTemplate.LEI_WEN -> drawLeiWen(canvas, bounds, paint, unitHeight)

            BorderTemplate.CLOUD_SOFT -> drawCloud(canvas, bounds, paint, unitHeight, 0.88f)
            BorderTemplate.CLOUD_TALL -> drawCloud(canvas, bounds, paint, unitHeight, 1f)
            BorderTemplate.CLOUD_SPIRAL -> drawCloudSpiral(canvas, bounds, paint, unitHeight)
            BorderTemplate.DOUBLE_CLOUD -> drawDoubleCloud(canvas, bounds, paint, unitHeight)
            BorderTemplate.CLOUD_FINE -> drawCloud(canvas, bounds, paint, unitHeight, 0.72f)
            BorderTemplate.CLOUD_LINKED -> drawCloudSpiral(canvas, bounds, paint, unitHeight)
            BorderTemplate.CLOUD_BEAD -> drawCloudBead(canvas, bounds, paint, unitHeight)
            BorderTemplate.RUYI_CLOUD -> drawRuyiCloud(canvas, bounds, paint, unitHeight)

            BorderTemplate.SCALLOP_SMALL,
            BorderTemplate.SCALLOP_LARGE,
            -> drawScallop(canvas, bounds, paint, unitHeight)
            BorderTemplate.DOUBLE_SCALLOP -> drawDoubleScallop(canvas, bounds, paint, unitHeight)
            BorderTemplate.WAVE_THIN,
            BorderTemplate.WAVE_BOLD,
            -> drawWave(canvas, bounds, paint, unitHeight)
            BorderTemplate.FISH_SCALE -> drawFishScale(canvas, bounds, paint, unitHeight)
            BorderTemplate.WAVE_DOUBLE -> drawDoubleWave(canvas, bounds, paint, unitHeight)
            BorderTemplate.WATER_RIPPLE -> drawWaterRipple(canvas, bounds, paint, unitHeight)
            BorderTemplate.ZIGZAG_WAVE -> drawZigzagWave(canvas, bounds, paint, unitHeight)

            BorderTemplate.SWIRL_THIN -> drawSwirl(canvas, bounds, paint, unitHeight)
            BorderTemplate.SWIRL_DOUBLE -> drawDoubleSwirl(canvas, bounds, paint, unitHeight)
            BorderTemplate.FLORAL_LOOP -> drawFloralLoop(canvas, bounds, paint, unitHeight)
            BorderTemplate.PETAL_CHAIN -> drawPetalChain(canvas, bounds, paint, unitHeight)
            BorderTemplate.LEAF_VINE -> drawLeafVine(canvas, bounds, paint, unitHeight)

            BorderTemplate.SEAL_PATTERN -> drawSealPattern(canvas, bounds, paint, unitHeight)
            BorderTemplate.MAZE_PATTERN -> drawMazePattern(canvas, bounds, paint, unitHeight)
            BorderTemplate.KNOT_PATTERN -> drawKnotPattern(canvas, bounds, paint, unitHeight)
            BorderTemplate.DIAMOND_CHAIN -> drawDiamondChain(canvas, bounds, paint, unitHeight)
            BorderTemplate.PEARL_CHAIN -> drawPearlChain(canvas, bounds, paint, unitHeight)

            BorderTemplate.BOLD_CLOUD_STRIP -> drawBoldCloudStrip(canvas, bounds, paint, unitHeight)
            BorderTemplate.BOLD_ORNAMENT_STRIP -> drawBoldOrnamentStrip(canvas, bounds, paint, unitHeight)
            BorderTemplate.BOLD_BLACK_WHITE_PATTERN -> drawBoldBlackWhitePattern(
                canvas,
                bounds,
                paint,
                unitHeight,
            )
        }
        canvas.restoreToCount(saveCount)
    }

    private fun drawParallelLines(canvas: Canvas, bounds: RectF, paint: Paint, count: Int) {
        val span = bounds.width() * 0.52f
        val start = bounds.centerX() - span / 2f
        val step = if (count <= 1) 0f else span / (count - 1)
        repeat(count) { index ->
            val x = start + step * index
            canvas.drawLine(x, bounds.top, x, bounds.bottom, paint)
        }
    }

    private fun drawLineAndDots(canvas: Canvas, bounds: RectF, paint: Paint, unitHeight: Float) {
        drawParallelLines(canvas, bounds, paint, 2)
        val fill = Paint(paint).apply { style = Paint.Style.FILL }
        val radius = max(paint.strokeWidth * 1.15f, bounds.width() * 0.07f)
        var y = bounds.top + unitHeight * 0.25f
        while (y < bounds.bottom) {
            canvas.drawCircle(bounds.centerX(), y, radius, fill)
            y += unitHeight * 0.5f
        }
    }

    private fun drawThinThickLines(canvas: Canvas, bounds: RectF, paint: Paint) {
        val leftX = bounds.centerX() - bounds.width() * 0.22f
        val rightX = bounds.centerX() + bounds.width() * 0.22f
        canvas.drawLine(leftX, bounds.top, leftX, bounds.bottom, Paint(paint).apply { strokeWidth *= 0.65f })
        canvas.drawLine(rightX, bounds.top, rightX, bounds.bottom, Paint(paint).apply { strokeWidth *= 1.65f })
    }

    private fun drawLineBeadChain(canvas: Canvas, bounds: RectF, paint: Paint, unitHeight: Float) {
        val lineX = bounds.left + bounds.width() * 0.22f
        val beadX = bounds.right - bounds.width() * 0.30f
        canvas.drawLine(lineX, bounds.top, lineX, bounds.bottom, paint)
        val fill = Paint(paint).apply { style = Paint.Style.FILL }
        var y = bounds.top + unitHeight * 0.25f
        while (y < bounds.bottom) {
            canvas.drawCircle(beadX, y, max(paint.strokeWidth, bounds.width() * 0.10f), fill)
            canvas.drawLine(lineX, y, beadX, y, Paint(paint).apply { strokeWidth *= 0.65f })
            y += unitHeight * 0.50f
        }
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
        forEachUnit(bounds, unitHeight) { top, bottom ->
            val quarter = (bottom - top) / 4f
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
        }
    }

    private fun drawInnerOuterKey(canvas: Canvas, bounds: RectF, paint: Paint, unitHeight: Float) {
        val outerX = bounds.left + bounds.width() * 0.14f
        canvas.drawLine(outerX, bounds.top, outerX, bounds.bottom, paint)
        drawGreekKey(
            canvas,
            RectF(bounds.left + bounds.width() * 0.28f, bounds.top, bounds.right, bounds.bottom),
            Paint(paint).apply { strokeWidth *= 0.82f },
            unitHeight,
        )
    }

    private fun drawDenseKey(canvas: Canvas, bounds: RectF, paint: Paint, unitHeight: Float) {
        drawGreekKey(canvas, bounds, paint, unitHeight)
        canvas.drawLine(bounds.centerX(), bounds.top, bounds.centerX(), bounds.bottom, paint)
    }

    private fun drawDoubleRowGreekKey(canvas: Canvas, bounds: RectF, paint: Paint, unitHeight: Float) {
        val gap = bounds.width() * 0.05f
        val narrowPaint = Paint(paint).apply { strokeWidth *= 0.78f }
        drawGreekKey(
            canvas,
            RectF(bounds.left, bounds.top, bounds.centerX() - gap, bounds.bottom),
            narrowPaint,
            unitHeight,
        )
        drawGreekKey(
            canvas,
            RectF(bounds.centerX() + gap, bounds.top, bounds.right, bounds.bottom),
            narrowPaint,
            unitHeight,
        )
    }

    private fun drawSquareGreekKey(canvas: Canvas, bounds: RectF, paint: Paint, unitHeight: Float) {
        forEachUnit(bounds, unitHeight) { top, bottom ->
            val middle = (top + bottom) / 2f
            val inset = bounds.width() * 0.18f
            val path = Path().apply {
                moveTo(bounds.centerX(), top)
                lineTo(bounds.right - inset, top)
                lineTo(bounds.right - inset, middle)
                lineTo(bounds.left + inset, middle)
                lineTo(bounds.left + inset, bottom)
                lineTo(bounds.centerX(), bottom)
            }
            canvas.drawPath(path, paint)
        }
    }

    private fun drawLeiWen(canvas: Canvas, bounds: RectF, paint: Paint, unitHeight: Float) {
        forEachUnit(bounds, unitHeight) { top, bottom ->
            val q = (bottom - top) / 4f
            val path = Path().apply {
                moveTo(bounds.centerX(), top)
                lineTo(bounds.right, top + q)
                lineTo(bounds.centerX(), top + q * 2f)
                lineTo(bounds.left, top + q * 3f)
                lineTo(bounds.centerX(), bottom)
                moveTo(bounds.left, top + q)
                lineTo(bounds.centerX(), top + q * 2f)
                lineTo(bounds.right, top + q * 3f)
            }
            canvas.drawPath(path, paint)
        }
    }

    private fun drawWave(canvas: Canvas, bounds: RectF, paint: Paint, unitHeight: Float) {
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
        amplitudeScale: Float,
    ) {
        val center = bounds.centerX()
        val halfWidth = bounds.width() * 0.5f * amplitudeScale
        val left = center - halfWidth
        val right = center + halfWidth
        forEachUnit(bounds, unitHeight) { top, bottom ->
            val q = (bottom - top) / 4f
            val path = Path().apply {
                moveTo(center, top)
                cubicTo(right, top, right, top + q, center, top + q)
                cubicTo(left, top + q, left, top + q * 2f, center, top + q * 2f)
                cubicTo(right, top + q * 2f, right, top + q * 3f, center, top + q * 3f)
                cubicTo(left, top + q * 3f, left, bottom, center, bottom)
            }
            canvas.drawPath(path, paint)
        }
    }

    private fun drawCloudSpiral(canvas: Canvas, bounds: RectF, paint: Paint, unitHeight: Float) {
        val radius = bounds.width() * 0.34f
        forEachUnit(bounds, unitHeight) { top, bottom ->
            val centerY = top + (bottom - top) * 0.5f
            val path = Path().apply {
                moveTo(bounds.centerX(), top)
                cubicTo(bounds.right, top, bounds.right, centerY, bounds.centerX(), centerY)
                cubicTo(
                    bounds.centerX() - radius,
                    centerY,
                    bounds.centerX() - radius,
                    centerY - radius,
                    bounds.centerX(),
                    centerY - radius,
                )
                cubicTo(
                    bounds.centerX() + radius * 0.7f,
                    centerY - radius,
                    bounds.centerX() + radius * 0.7f,
                    centerY,
                    bounds.centerX(),
                    centerY,
                )
                cubicTo(bounds.left, centerY, bounds.left, bottom, bounds.centerX(), bottom)
            }
            canvas.drawPath(path, paint)
        }
    }

    private fun drawDoubleCloud(canvas: Canvas, bounds: RectF, paint: Paint, unitHeight: Float) {
        val gap = bounds.width() * 0.06f
        drawCloud(
            canvas,
            RectF(bounds.left, bounds.top, bounds.centerX() - gap, bounds.bottom),
            Paint(paint).apply { strokeWidth *= 0.78f },
            unitHeight,
            0.9f,
        )
        drawCloud(
            canvas,
            RectF(bounds.centerX() + gap, bounds.top, bounds.right, bounds.bottom),
            Paint(paint).apply { strokeWidth *= 0.78f },
            unitHeight,
            0.9f,
        )
    }

    private fun drawCloudBead(canvas: Canvas, bounds: RectF, paint: Paint, unitHeight: Float) {
        drawCloud(canvas, bounds, paint, unitHeight, 0.84f)
        val fill = Paint(paint).apply { style = Paint.Style.FILL }
        var y = bounds.top + unitHeight * 0.50f
        while (y < bounds.bottom) {
            canvas.drawCircle(bounds.centerX(), y, max(paint.strokeWidth, bounds.width() * 0.075f), fill)
            y += unitHeight
        }
    }

    private fun drawRuyiCloud(canvas: Canvas, bounds: RectF, paint: Paint, unitHeight: Float) {
        forEachUnit(bounds, unitHeight) { top, bottom ->
            val middle = (top + bottom) / 2f
            val path = Path().apply {
                moveTo(bounds.centerX(), top)
                cubicTo(bounds.right, top, bounds.right, middle, bounds.centerX(), middle)
                cubicTo(bounds.left, middle, bounds.left, bottom, bounds.centerX(), bottom)
                moveTo(bounds.centerX(), middle)
                cubicTo(
                    bounds.centerX() - bounds.width() * 0.28f,
                    middle,
                    bounds.centerX() - bounds.width() * 0.28f,
                    middle - bounds.width() * 0.28f,
                    bounds.centerX(),
                    middle - bounds.width() * 0.28f,
                )
            }
            canvas.drawPath(path, paint)
        }
    }

    private fun drawScallop(canvas: Canvas, bounds: RectF, paint: Paint, unitHeight: Float) {
        forEachUnit(bounds, unitHeight) { top, bottom ->
            canvas.drawArc(RectF(bounds.left, top, bounds.right, bottom), -90f, 180f, false, paint)
        }
    }

    private fun drawDoubleScallop(canvas: Canvas, bounds: RectF, paint: Paint, unitHeight: Float) {
        drawScallop(canvas, bounds, paint, unitHeight)
        drawScallop(
            canvas,
            RectF(bounds.left + bounds.width() * 0.28f, bounds.top, bounds.right, bounds.bottom),
            Paint(paint).apply { strokeWidth *= 0.78f },
            unitHeight,
        )
    }

    private fun drawFishScale(canvas: Canvas, bounds: RectF, paint: Paint, unitHeight: Float) {
        val halfWidth = bounds.width() * 0.58f
        forEachUnit(bounds, unitHeight) { top, bottom ->
            val middle = (top + bottom) / 2f
            canvas.drawArc(RectF(bounds.left, top, bounds.left + halfWidth, bottom), -90f, 180f, false, paint)
            canvas.drawArc(RectF(bounds.right - halfWidth, middle, bounds.right, bottom + unitHeight / 2f), -90f, 180f, false, paint)
        }
    }

    private fun drawDoubleWave(canvas: Canvas, bounds: RectF, paint: Paint, unitHeight: Float) {
        val gap = bounds.width() * 0.08f
        drawWave(
            canvas,
            RectF(bounds.left, bounds.top, bounds.centerX() - gap, bounds.bottom),
            Paint(paint).apply { strokeWidth *= 0.82f },
            unitHeight,
        )
        drawWave(
            canvas,
            RectF(bounds.centerX() + gap, bounds.top, bounds.right, bounds.bottom),
            Paint(paint).apply { strokeWidth *= 0.82f },
            unitHeight,
        )
    }

    private fun drawWaterRipple(canvas: Canvas, bounds: RectF, paint: Paint, unitHeight: Float) {
        val narrowPaint = Paint(paint).apply { strokeWidth *= 0.74f }
        listOf(-0.26f, 0f, 0.26f).forEach { horizontalOffset ->
            val center = bounds.centerX() + bounds.width() * horizontalOffset
            val narrowBounds = RectF(
                center - bounds.width() * 0.18f,
                bounds.top,
                center + bounds.width() * 0.18f,
                bounds.bottom,
            )
            drawWave(canvas, narrowBounds, narrowPaint, unitHeight)
        }
    }

    private fun drawZigzagWave(canvas: Canvas, bounds: RectF, paint: Paint, unitHeight: Float) {
        val path = Path().apply { moveTo(bounds.centerX(), bounds.top) }
        var y = bounds.top
        var right = true
        val step = unitHeight / 2f
        while (y < bounds.bottom) {
            y += step
            path.lineTo(if (right) bounds.right else bounds.left, y)
            right = !right
        }
        canvas.drawPath(path, paint)
    }

    private fun drawSwirl(canvas: Canvas, bounds: RectF, paint: Paint, unitHeight: Float) {
        val center = bounds.centerX()
        val radius = bounds.width() * 0.38f
        forEachUnit(bounds, unitHeight) { top, bottom ->
            val middleY = (top + bottom) / 2f
            val path = Path().apply {
                moveTo(center, top)
                cubicTo(bounds.right, top + unitHeight * 0.18f, bounds.right, middleY, center, middleY)
                cubicTo(center - radius, middleY, center - radius, middleY - radius, center, middleY - radius)
                cubicTo(center + radius * 0.65f, middleY - radius, center + radius * 0.65f, middleY, center, middleY)
                cubicTo(bounds.left, middleY, bounds.left, bottom - unitHeight * 0.18f, center, bottom)
            }
            canvas.drawPath(path, paint)
        }
    }

    private fun drawDoubleSwirl(canvas: Canvas, bounds: RectF, paint: Paint, unitHeight: Float) {
        val narrowPaint = Paint(paint).apply { strokeWidth *= 0.75f }
        drawSwirl(
            canvas,
            RectF(bounds.left, bounds.top, bounds.centerX() + bounds.width() * 0.08f, bounds.bottom),
            narrowPaint,
            unitHeight,
        )
        drawSwirl(
            canvas,
            RectF(bounds.centerX() - bounds.width() * 0.08f, bounds.top, bounds.right, bounds.bottom),
            narrowPaint,
            unitHeight,
        )
    }

    private fun drawFloralLoop(canvas: Canvas, bounds: RectF, paint: Paint, unitHeight: Float) {
        val center = bounds.centerX()
        canvas.drawLine(center, bounds.top, center, bounds.bottom, paint)
        forEachUnit(bounds, unitHeight) { top, bottom ->
            val middle = (top + bottom) / 2f
            val petalHeight = (bottom - top) * 0.48f
            canvas.drawOval(RectF(bounds.left, middle - petalHeight / 2f, center, middle + petalHeight / 2f), paint)
            canvas.drawOval(RectF(center, middle - petalHeight / 2f, bounds.right, middle + petalHeight / 2f), paint)
        }
    }

    private fun drawPetalChain(canvas: Canvas, bounds: RectF, paint: Paint, unitHeight: Float) {
        val center = bounds.centerX()
        forEachUnit(bounds, unitHeight) { top, bottom ->
            val middle = (top + bottom) / 2f
            val path = Path().apply {
                moveTo(center, top)
                cubicTo(bounds.right, top + unitHeight * 0.18f, bounds.right, middle, center, middle)
                cubicTo(bounds.right, middle, bounds.right, bottom - unitHeight * 0.18f, center, bottom)
                cubicTo(bounds.left, bottom - unitHeight * 0.18f, bounds.left, middle, center, middle)
                cubicTo(bounds.left, middle, bounds.left, top + unitHeight * 0.18f, center, top)
            }
            canvas.drawPath(path, paint)
        }
    }

    private fun drawLeafVine(canvas: Canvas, bounds: RectF, paint: Paint, unitHeight: Float) {
        val center = bounds.centerX()
        canvas.drawLine(center, bounds.top, center, bounds.bottom, paint)
        forEachUnit(bounds, unitHeight) { top, bottom ->
            val middle = (top + bottom) / 2f
            val leftLeaf = Path().apply {
                moveTo(center, middle)
                cubicTo(bounds.left, middle - unitHeight * 0.30f, bounds.left, middle, center, middle + unitHeight * 0.20f)
            }
            val rightLeaf = Path().apply {
                moveTo(center, middle + unitHeight * 0.18f)
                cubicTo(bounds.right, middle, bounds.right, middle + unitHeight * 0.30f, center, middle + unitHeight * 0.42f)
            }
            canvas.drawPath(leftLeaf, paint)
            canvas.drawPath(rightLeaf, paint)
        }
    }

    private fun drawSealPattern(canvas: Canvas, bounds: RectF, paint: Paint, unitHeight: Float) {
        val center = bounds.centerX()
        canvas.drawLine(center, bounds.top, center, bounds.bottom, paint)
        forEachUnit(bounds, unitHeight) { top, bottom ->
            val insetX = bounds.width() * 0.08f
            val insetY = (bottom - top) * 0.12f
            val outer = RectF(bounds.left + insetX, top + insetY, bounds.right - insetX, bottom - insetY)
            canvas.drawRect(outer, paint)
            val innerInset = bounds.width() * 0.22f
            canvas.drawRect(
                RectF(outer.left + innerInset, outer.top + innerInset, outer.right - innerInset, outer.bottom - innerInset),
                paint,
            )
        }
    }

    private fun drawMazePattern(canvas: Canvas, bounds: RectF, paint: Paint, unitHeight: Float) {
        val left = bounds.left + paint.strokeWidth / 2f
        val right = bounds.right - paint.strokeWidth / 2f
        val center = bounds.centerX()
        forEachUnit(bounds, unitHeight) { top, bottom ->
            val step = (bottom - top) / 6f
            val path = Path().apply {
                moveTo(center, top)
                lineTo(right, top + step)
                lineTo(left, top + step * 2f)
                lineTo(right, top + step * 3f)
                lineTo(left, top + step * 4f)
                lineTo(right, top + step * 5f)
                lineTo(center, bottom)
            }
            canvas.drawPath(path, paint)
        }
    }

    private fun drawKnotPattern(canvas: Canvas, bounds: RectF, paint: Paint, unitHeight: Float) {
        val center = bounds.centerX()
        forEachUnit(bounds, unitHeight) { top, bottom ->
            val q = (bottom - top) / 4f
            val path = Path().apply {
                moveTo(center, top)
                lineTo(bounds.right, top + q)
                lineTo(center, top + q * 2f)
                lineTo(bounds.left, top + q)
                lineTo(center, top)
                moveTo(center, top + q * 2f)
                lineTo(bounds.right, top + q * 3f)
                lineTo(center, bottom)
                lineTo(bounds.left, top + q * 3f)
                close()
            }
            canvas.drawPath(path, paint)
            canvas.drawCircle(center, top + q * 2f, bounds.width() * 0.11f, paint)
        }
    }

    private fun drawDiamondChain(canvas: Canvas, bounds: RectF, paint: Paint, unitHeight: Float) {
        forEachUnit(bounds, unitHeight) { top, bottom ->
            val middle = (top + bottom) / 2f
            val diamond = Path().apply {
                moveTo(bounds.centerX(), top)
                lineTo(bounds.right, middle)
                lineTo(bounds.centerX(), bottom)
                lineTo(bounds.left, middle)
                close()
            }
            canvas.drawPath(diamond, paint)
            canvas.drawLine(bounds.left, middle, bounds.right, middle, Paint(paint).apply { strokeWidth *= 0.68f })
        }
    }

    private fun drawPearlChain(canvas: Canvas, bounds: RectF, paint: Paint, unitHeight: Float) {
        canvas.drawLine(bounds.centerX(), bounds.top, bounds.centerX(), bounds.bottom, paint)
        val fill = Paint(paint).apply { style = Paint.Style.FILL }
        var y = bounds.top + unitHeight * 0.25f
        var large = true
        while (y < bounds.bottom) {
            val radius = bounds.width() * if (large) 0.18f else 0.10f
            canvas.drawCircle(bounds.centerX(), y, radius, if (large) paint else fill)
            y += unitHeight * 0.50f
            large = !large
        }
    }

    private fun drawBoldCloudStrip(canvas: Canvas, bounds: RectF, paint: Paint, unitHeight: Float) {
        drawCloud(canvas, bounds, paint, unitHeight, 1f)
        val fill = Paint(paint).apply { style = Paint.Style.FILL }
        var y = bounds.top
        while (y < bounds.bottom) {
            canvas.drawCircle(bounds.centerX(), y, max(paint.strokeWidth, bounds.width() * 0.08f), fill)
            y += unitHeight / 2f
        }
    }

    private fun drawBoldOrnamentStrip(canvas: Canvas, bounds: RectF, paint: Paint, unitHeight: Float) {
        val center = bounds.centerX()
        canvas.drawLine(center, bounds.top, center, bounds.bottom, paint)
        val fill = Paint(paint).apply { style = Paint.Style.FILL }
        forEachUnit(bounds, unitHeight) { top, bottom ->
            val middle = (top + bottom) / 2f
            val diamond = Path().apply {
                moveTo(center, top + unitHeight * 0.08f)
                lineTo(bounds.right, middle)
                lineTo(center, bottom - unitHeight * 0.08f)
                lineTo(bounds.left, middle)
                close()
            }
            canvas.drawPath(diamond, paint)
            canvas.drawCircle(center, middle, bounds.width() * 0.14f, fill)
        }
    }

    private fun drawBoldBlackWhitePattern(canvas: Canvas, bounds: RectF, paint: Paint, unitHeight: Float) {
        val fill = Paint(paint).apply { style = Paint.Style.FILL }
        forEachUnit(bounds, unitHeight) { top, bottom ->
            val middle = (top + bottom) / 2f
            val topChevron = Path().apply {
                moveTo(bounds.left, top)
                lineTo(bounds.right, top)
                lineTo(bounds.centerX(), middle)
                close()
            }
            val bottomChevron = Path().apply {
                moveTo(bounds.left, bottom)
                lineTo(bounds.right, bottom)
                lineTo(bounds.centerX(), middle)
                close()
            }
            canvas.drawPath(topChevron, fill)
            canvas.drawPath(bottomChevron, fill)
            canvas.drawCircle(bounds.centerX(), middle, bounds.width() * 0.1f, fill)
        }
    }

    private inline fun forEachUnit(
        bounds: RectF,
        requestedUnitHeight: Float,
        draw: (top: Float, bottom: Float) -> Unit,
    ) {
        val unitHeight = requestedUnitHeight.coerceAtLeast(1f)
        var top = bounds.top
        while (top < bounds.bottom) {
            draw(top, top + unitHeight)
            top += unitHeight
        }
    }
}
