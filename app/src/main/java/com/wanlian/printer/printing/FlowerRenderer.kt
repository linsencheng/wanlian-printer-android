package com.wanlian.printer.printing

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.wanlian.printer.model.FlowerStyle
import kotlin.math.max

/** Original procedural flower marks; no external bitmap assets are used. */
class FlowerRenderer {
    fun draw(
        canvas: Canvas,
        style: FlowerStyle,
        centerX: Float,
        topY: Float,
        sizeDots: Float,
        sourcePaint: Paint,
    ) {
        if (style == FlowerStyle.NONE) return
        val size = sizeDots.coerceAtLeast(8f)
        val centerY = topY + size / 2f
        val stroke = Paint(sourcePaint).apply {
            this.style = Paint.Style.STROKE
            strokeWidth = max(1f, size * 0.035f)
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val fill = Paint(sourcePaint).apply { this.style = Paint.Style.FILL }
        when (style) {
            FlowerStyle.NONE -> Unit
            FlowerStyle.WHITE_CHRYSANTHEMUM -> drawChrysanthemum(
                canvas, centerX, centerY, size, stroke, fill, petals = 16,
            )
            FlowerStyle.CHRYSANTHEMUM_SINGLE -> drawChrysanthemum(
                canvas, centerX, centerY, size, stroke, fill, petals = 10,
            )
            FlowerStyle.CHRYSANTHEMUM_DOUBLE -> {
                drawChrysanthemum(canvas, centerX, centerY, size, stroke, fill, petals = 18)
                drawChrysanthemum(canvas, centerX, centerY, size * 0.62f, stroke, fill, petals = 10)
            }
            FlowerStyle.LOTUS -> drawLotus(canvas, centerX, centerY, size, stroke)
            FlowerStyle.PLUM_BLOSSOM -> drawPlumBlossom(canvas, centerX, centerY, size, stroke, fill)
            FlowerStyle.ORCHID_OUTLINE -> drawOrchid(canvas, centerX, centerY, size, stroke)
            FlowerStyle.ROSE_OUTLINE -> drawRose(canvas, centerX, centerY, size, stroke)
            FlowerStyle.CAMELLIA -> drawCamellia(canvas, centerX, centerY, size, stroke, fill)
            FlowerStyle.FLOWER_BOUQUET -> drawBouquet(canvas, centerX, centerY, size, stroke, fill)
            FlowerStyle.FLOWER_BRANCH -> drawFlowerBranch(canvas, centerX, centerY, size, stroke, fill)
            FlowerStyle.MEMORIAL_FLORAL -> drawMemorialFloral(canvas, centerX, centerY, size, stroke, fill)
            FlowerStyle.CHRYSANTHEMUM_REALISTIC -> drawRealisticChrysanthemum(
                canvas, centerX, centerY, size, stroke, fill,
            )
            FlowerStyle.MEMORIAL_SINGLE -> drawMemorialSingle(
                canvas, centerX, centerY, size, stroke, fill,
            )
            FlowerStyle.DOUBLE_BLOOM -> drawDoubleBloom(
                canvas, centerX, centerY, size, stroke, fill,
            )
            FlowerStyle.LEAFY_BOUQUET -> drawLeafyBouquet(
                canvas, centerX, centerY, size, stroke, fill,
            )
            FlowerStyle.VERTICAL_FLORAL -> drawVerticalFloral(
                canvas, centerX, centerY, size, stroke, fill,
            )
            FlowerStyle.HORIZONTAL_FLORAL -> drawHorizontalFloral(
                canvas, centerX, centerY, size, stroke, fill,
            )
            FlowerStyle.LILY_OUTLINE -> drawLily(canvas, centerX, centerY, size, stroke, fill)
            FlowerStyle.MAGNOLIA -> drawMagnolia(canvas, centerX, centerY, size, stroke, fill)
            FlowerStyle.PEONY_OUTLINE -> drawPeony(canvas, centerX, centerY, size, stroke, fill)
            FlowerStyle.DAHLIA -> drawDahlia(canvas, centerX, centerY, size, stroke, fill)
            FlowerStyle.SUNFLOWER_LINE -> drawSunflower(canvas, centerX, centerY, size, stroke, fill)
            FlowerStyle.DAISY_SPRAY -> drawDaisySpray(canvas, centerX, centerY, size, stroke, fill)
            FlowerStyle.CHINESE_KNOT_FLOWER -> drawChineseKnotFlower(
                canvas, centerX, centerY, size, stroke, fill,
            )
            FlowerStyle.CORNER_BLOSSOM_LEFT -> drawCornerBlossom(
                canvas, centerX, centerY, size, stroke, fill, mirror = false,
            )
            FlowerStyle.CORNER_BLOSSOM_RIGHT -> drawCornerBlossom(
                canvas, centerX, centerY, size, stroke, fill, mirror = true,
            )
            FlowerStyle.CRESCENT_WREATH -> drawWreath(
                canvas, centerX, centerY, size, stroke, fill, crescent = true,
            )
            FlowerStyle.OVAL_WREATH -> drawWreath(
                canvas, centerX, centerY, size, stroke, fill, crescent = false,
            )
            FlowerStyle.LOTUS_DOUBLE -> drawDoubleLotus(canvas, centerX, centerY, size, stroke)
            FlowerStyle.ROSE_SPRAY -> drawRoseSpray(canvas, centerX, centerY, size, stroke, fill)
            FlowerStyle.PLUM_BRANCH -> drawPlumBranch(canvas, centerX, centerY, size, stroke, fill)
            FlowerStyle.ORCHID_SPRAY -> drawOrchidSpray(canvas, centerX, centerY, size, stroke, fill)
            FlowerStyle.CAMELLIA_PAIR -> drawCamelliaPair(canvas, centerX, centerY, size, stroke, fill)
            FlowerStyle.CHRYSANTHEMUM_SPRAY -> drawChrysanthemumSpray(
                canvas, centerX, centerY, size, stroke, fill,
            )
            FlowerStyle.MEMORIAL_WREATH -> drawMemorialWreath(
                canvas, centerX, centerY, size, stroke, fill,
            )
            FlowerStyle.RIBBON_BOUQUET -> drawRibbonBouquet(
                canvas, centerX, centerY, size, stroke, fill,
            )
            FlowerStyle.LEAF_GARLAND -> drawLeafGarland(canvas, centerX, centerY, size, stroke, fill)
            FlowerStyle.BUD_BRANCH -> drawBudBranch(canvas, centerX, centerY, size, stroke, fill)
            FlowerStyle.THREE_BLOOM -> drawThreeBloom(canvas, centerX, centerY, size, stroke, fill)
            FlowerStyle.FAN_FLORAL -> drawFanFloral(canvas, centerX, centerY, size, stroke, fill)
        }
    }

    private fun drawChrysanthemum(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        size: Float,
        stroke: Paint,
        fill: Paint,
        petals: Int,
    ) {
        val petalWidth = size * if (petals > 12) 0.15f else 0.20f
        val petalLength = size * 0.38f
        repeat(petals) { index ->
            canvas.save()
            canvas.rotate(index * (360f / petals), centerX, centerY)
            canvas.drawOval(
                RectF(
                    centerX - petalWidth / 2f,
                    centerY - petalLength,
                    centerX + petalWidth / 2f,
                    centerY + size * 0.03f,
                ),
                stroke,
            )
            canvas.restore()
        }
        canvas.drawCircle(centerX, centerY, size * 0.10f, fill)
        canvas.drawCircle(centerX, centerY, size * 0.17f, stroke)
    }

    private fun drawLotus(canvas: Canvas, centerX: Float, centerY: Float, size: Float, paint: Paint) {
        val half = size / 2f
        val path = Path().apply {
            moveTo(centerX, centerY + half * 0.68f)
            cubicTo(centerX - half * 0.22f, centerY, centerX - half * 0.20f, centerY - half, centerX, centerY - half * 0.78f)
            cubicTo(centerX + half * 0.20f, centerY - half, centerX + half * 0.22f, centerY, centerX, centerY + half * 0.68f)
            moveTo(centerX - half * 0.95f, centerY + half * 0.28f)
            cubicTo(centerX - half * 0.70f, centerY - half * 0.55f, centerX - half * 0.30f, centerY - half * 0.48f, centerX, centerY + half * 0.68f)
            cubicTo(centerX + half * 0.30f, centerY - half * 0.48f, centerX + half * 0.70f, centerY - half * 0.55f, centerX + half * 0.95f, centerY + half * 0.28f)
            quadTo(centerX, centerY + half, centerX - half * 0.95f, centerY + half * 0.28f)
        }
        canvas.drawPath(path, paint)
    }

    private fun drawRose(canvas: Canvas, centerX: Float, centerY: Float, size: Float, paint: Paint) {
        val radius = size * 0.45f
        canvas.drawCircle(centerX, centerY, radius, paint)
        val path = Path().apply {
            moveTo(centerX, centerY)
            cubicTo(centerX + radius * 0.12f, centerY - radius * 0.62f, centerX + radius * 0.78f, centerY - radius * 0.36f, centerX + radius * 0.55f, centerY + radius * 0.12f)
            cubicTo(centerX + radius * 0.32f, centerY + radius * 0.66f, centerX - radius * 0.55f, centerY + radius * 0.55f, centerX - radius * 0.52f, centerY - radius * 0.02f)
            cubicTo(centerX - radius * 0.48f, centerY - radius * 0.48f, centerX + radius * 0.18f, centerY - radius * 0.48f, centerX + radius * 0.20f, centerY - radius * 0.12f)
            cubicTo(centerX + radius * 0.22f, centerY + radius * 0.16f, centerX - radius * 0.18f, centerY + radius * 0.24f, centerX, centerY)
        }
        canvas.drawPath(path, paint)
    }

    private fun drawPlumBlossom(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        size: Float,
        stroke: Paint,
        fill: Paint,
    ) {
        val petalLength = size * 0.34f
        repeat(5) { index ->
            canvas.save()
            canvas.rotate(index * 72f, centerX, centerY)
            canvas.drawOval(
                RectF(
                    centerX - size * 0.14f,
                    centerY - petalLength,
                    centerX + size * 0.14f,
                    centerY + size * 0.02f,
                ),
                stroke,
            )
            canvas.restore()
        }
        canvas.drawCircle(centerX, centerY, size * 0.08f, fill)
        repeat(5) { index ->
            val angle = Math.toRadians((index * 72.0) - 90.0)
            canvas.drawCircle(
                centerX + kotlin.math.cos(angle).toFloat() * size * 0.16f,
                centerY + kotlin.math.sin(angle).toFloat() * size * 0.16f,
                size * 0.025f,
                fill,
            )
        }
    }

    private fun drawOrchid(canvas: Canvas, centerX: Float, centerY: Float, size: Float, paint: Paint) {
        val half = size / 2f
        val path = Path().apply {
            moveTo(centerX, centerY + half)
            cubicTo(centerX - half * 0.08f, centerY + half * 0.1f, centerX - half * 0.55f, centerY - half * 0.1f, centerX - half * 0.82f, centerY - half * 0.72f)
            cubicTo(centerX - half * 0.30f, centerY - half * 0.52f, centerX - half * 0.08f, centerY - half * 0.20f, centerX, centerY)
            cubicTo(centerX + half * 0.12f, centerY - half * 0.50f, centerX + half * 0.52f, centerY - half * 0.76f, centerX + half * 0.88f, centerY - half * 0.36f)
            cubicTo(centerX + half * 0.48f, centerY - half * 0.18f, centerX + half * 0.20f, centerY + half * 0.02f, centerX, centerY)
            cubicTo(centerX - half * 0.18f, centerY + half * 0.06f, centerX - half * 0.34f, centerY + half * 0.34f, centerX - half * 0.48f, centerY + half * 0.62f)
        }
        canvas.drawPath(path, paint)
    }

    private fun drawCamellia(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        size: Float,
        stroke: Paint,
        fill: Paint,
    ) {
        repeat(8) { index ->
            canvas.save()
            canvas.rotate(index * 45f + if (index % 2 == 0) 0f else 8f, centerX, centerY)
            canvas.drawOval(
                RectF(
                    centerX - size * 0.18f,
                    centerY - size * 0.43f,
                    centerX + size * 0.18f,
                    centerY + size * 0.10f,
                ),
                stroke,
            )
            canvas.restore()
        }
        canvas.drawCircle(centerX, centerY, size * 0.13f, stroke)
        repeat(7) { index ->
            val angle = Math.toRadians(index * (360.0 / 7.0))
            canvas.drawCircle(
                centerX + kotlin.math.cos(angle).toFloat() * size * 0.08f,
                centerY + kotlin.math.sin(angle).toFloat() * size * 0.08f,
                size * 0.018f,
                fill,
            )
        }
    }

    private fun drawBouquet(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        size: Float,
        stroke: Paint,
        fill: Paint,
    ) {
        val bottom = centerY + size * 0.45f
        val flowers = listOf(
            Triple(centerX - size * 0.23f, centerY - size * 0.18f, size * 0.34f),
            Triple(centerX + size * 0.18f, centerY - size * 0.28f, size * 0.42f),
            Triple(centerX + size * 0.30f, centerY + size * 0.02f, size * 0.26f),
        )
        flowers.forEachIndexed { index, (x, y, bloomSize) ->
            canvas.drawLine(centerX, bottom, x, y, stroke)
            drawSimpleBloom(canvas, x, y, bloomSize, stroke, fill, petals = if (index == 1) 7 else 5)
        }
        canvas.drawLine(centerX - size * 0.28f, centerY + size * 0.18f, centerX, bottom, stroke)
        canvas.drawLine(centerX + size * 0.34f, centerY + size * 0.20f, centerX, bottom, stroke)
    }

    private fun drawFlowerBranch(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        size: Float,
        stroke: Paint,
        fill: Paint,
    ) {
        val startX = centerX - size * 0.44f
        val startY = centerY + size * 0.42f
        val endX = centerX + size * 0.42f
        val endY = centerY - size * 0.35f
        val branch = Path().apply {
            moveTo(startX, startY)
            cubicTo(centerX - size * 0.12f, centerY + size * 0.18f, centerX + size * 0.08f, centerY - size * 0.10f, endX, endY)
        }
        canvas.drawPath(branch, stroke)
        listOf(-0.22f to 0.18f, 0.02f to -0.02f, 0.24f to -0.22f).forEachIndexed { index, (dx, dy) ->
            val x = centerX + size * dx
            val y = centerY + size * dy
            drawSimpleBloom(canvas, x, y, size * if (index == 1) 0.26f else 0.20f, stroke, fill, 5)
            canvas.save()
            canvas.rotate(if (index % 2 == 0) -35f else 35f, x, y + size * 0.13f)
            canvas.drawOval(RectF(x - size * 0.12f, y + size * 0.07f, x + size * 0.12f, y + size * 0.28f), stroke)
            canvas.restore()
        }
    }

    private fun drawMemorialFloral(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        size: Float,
        stroke: Paint,
        fill: Paint,
    ) {
        val path = Path().apply {
            moveTo(centerX - size * 0.42f, centerY + size * 0.32f)
            cubicTo(centerX - size * 0.18f, centerY + size * 0.08f, centerX - size * 0.22f, centerY - size * 0.28f, centerX + size * 0.04f, centerY - size * 0.42f)
            cubicTo(centerX + size * 0.18f, centerY - size * 0.08f, centerX + size * 0.32f, centerY + size * 0.12f, centerX + size * 0.45f, centerY + size * 0.38f)
            moveTo(centerX - size * 0.06f, centerY + size * 0.38f)
            cubicTo(centerX + size * 0.02f, centerY + size * 0.14f, centerX + size * 0.08f, centerY - size * 0.04f, centerX + size * 0.26f, centerY - size * 0.18f)
        }
        canvas.drawPath(path, stroke)
        drawSimpleBloom(canvas, centerX - size * 0.14f, centerY - size * 0.20f, size * 0.30f, stroke, fill, 6)
        drawSimpleBloom(canvas, centerX + size * 0.24f, centerY + size * 0.02f, size * 0.20f, stroke, fill, 5)
    }

    private fun drawRealisticChrysanthemum(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        size: Float,
        stroke: Paint,
        fill: Paint,
    ) {
        drawPetalRing(canvas, centerX, centerY, size, 24, 0.46f, 0.095f, stroke, 4f)
        drawPetalRing(canvas, centerX, centerY, size, 18, 0.34f, 0.085f, stroke, 13f)
        drawPetalRing(canvas, centerX, centerY, size, 12, 0.23f, 0.075f, stroke, 2f)
        canvas.drawCircle(centerX, centerY, size * 0.075f, fill)
        repeat(8) { index ->
            val angle = Math.toRadians(index * 45.0)
            canvas.drawCircle(
                centerX + kotlin.math.cos(angle).toFloat() * size * 0.105f,
                centerY + kotlin.math.sin(angle).toFloat() * size * 0.105f,
                size * 0.014f,
                fill,
            )
        }
    }

    private fun drawMemorialSingle(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        size: Float,
        stroke: Paint,
        fill: Paint,
    ) {
        val bloomY = centerY - size * 0.12f
        drawRealisticChrysanthemum(canvas, centerX, bloomY, size * 0.70f, stroke, fill)
        val stemBottom = centerY + size * 0.46f
        canvas.drawLine(centerX, bloomY + size * 0.24f, centerX, stemBottom, stroke)
        drawLeaf(canvas, centerX - size * 0.13f, centerY + size * 0.19f, size * 0.22f, size * 0.42f, -52f, stroke)
        drawLeaf(canvas, centerX + size * 0.14f, centerY + size * 0.28f, size * 0.20f, size * 0.36f, 48f, stroke)
    }

    private fun drawDoubleBloom(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        size: Float,
        stroke: Paint,
        fill: Paint,
    ) {
        val bottomY = centerY + size * 0.43f
        val leftX = centerX - size * 0.19f
        val leftY = centerY - size * 0.15f
        val rightX = centerX + size * 0.22f
        val rightY = centerY + size * 0.02f
        canvas.drawLine(centerX, bottomY, leftX, leftY, stroke)
        canvas.drawLine(centerX, bottomY, rightX, rightY, stroke)
        drawRealisticChrysanthemum(canvas, leftX, leftY, size * 0.54f, stroke, fill)
        drawChrysanthemum(canvas, rightX, rightY, size * 0.42f, stroke, fill, petals = 14)
        drawLeaf(canvas, centerX - size * 0.18f, centerY + size * 0.24f, size * 0.17f, size * 0.34f, -60f, stroke)
        drawLeaf(canvas, centerX + size * 0.17f, centerY + size * 0.29f, size * 0.17f, size * 0.32f, 58f, stroke)
    }

    private fun drawLeafyBouquet(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        size: Float,
        stroke: Paint,
        fill: Paint,
    ) {
        val tieY = centerY + size * 0.34f
        val blooms = listOf(
            Triple(centerX - size * 0.26f, centerY - size * 0.18f, size * 0.34f),
            Triple(centerX + size * 0.02f, centerY - size * 0.29f, size * 0.42f),
            Triple(centerX + size * 0.29f, centerY - size * 0.10f, size * 0.30f),
            Triple(centerX + size * 0.09f, centerY + size * 0.03f, size * 0.24f),
        )
        blooms.forEachIndexed { index, (x, y, bloomSize) ->
            canvas.drawLine(centerX, tieY, x, y, stroke)
            if (index == 1) {
                drawRealisticChrysanthemum(canvas, x, y, bloomSize, stroke, fill)
            } else {
                drawSimpleBloom(canvas, x, y, bloomSize, stroke, fill, if (index == 0) 7 else 6)
            }
        }
        drawLeaf(canvas, centerX - size * 0.31f, centerY + size * 0.13f, size * 0.17f, size * 0.38f, -66f, stroke)
        drawLeaf(canvas, centerX + size * 0.34f, centerY + size * 0.16f, size * 0.17f, size * 0.40f, 68f, stroke)
        drawLeaf(canvas, centerX - size * 0.17f, centerY + size * 0.28f, size * 0.15f, size * 0.30f, -42f, stroke)
        canvas.drawOval(
            RectF(centerX - size * 0.09f, tieY - size * 0.035f, centerX + size * 0.09f, tieY + size * 0.035f),
            fill,
        )
    }

    private fun drawVerticalFloral(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        size: Float,
        stroke: Paint,
        fill: Paint,
    ) {
        val stem = Path().apply {
            moveTo(centerX - size * 0.10f, centerY + size * 0.47f)
            cubicTo(
                centerX + size * 0.13f, centerY + size * 0.19f,
                centerX - size * 0.14f, centerY - size * 0.12f,
                centerX + size * 0.10f, centerY - size * 0.46f,
            )
        }
        canvas.drawPath(stem, stroke)
        drawSimpleBloom(canvas, centerX + size * 0.10f, centerY - size * 0.35f, size * 0.30f, stroke, fill, 7)
        drawSimpleBloom(canvas, centerX - size * 0.14f, centerY - size * 0.05f, size * 0.24f, stroke, fill, 6)
        drawSimpleBloom(canvas, centerX + size * 0.14f, centerY + size * 0.20f, size * 0.20f, stroke, fill, 5)
        drawLeaf(canvas, centerX + size * 0.20f, centerY - size * 0.17f, size * 0.14f, size * 0.30f, 56f, stroke)
        drawLeaf(canvas, centerX - size * 0.20f, centerY + size * 0.16f, size * 0.14f, size * 0.32f, -58f, stroke)
        drawLeaf(canvas, centerX + size * 0.16f, centerY + size * 0.35f, size * 0.12f, size * 0.25f, 44f, stroke)
    }

    private fun drawHorizontalFloral(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        size: Float,
        stroke: Paint,
        fill: Paint,
    ) {
        val branch = Path().apply {
            moveTo(centerX - size * 0.47f, centerY + size * 0.10f)
            cubicTo(
                centerX - size * 0.18f, centerY - size * 0.10f,
                centerX + size * 0.17f, centerY + size * 0.09f,
                centerX + size * 0.47f, centerY - size * 0.12f,
            )
        }
        canvas.drawPath(branch, stroke)
        val blooms = listOf(
            Triple(-0.32f, -0.08f, 0.25f),
            Triple(-0.08f, 0.02f, 0.31f),
            Triple(0.18f, -0.06f, 0.23f),
            Triple(0.36f, -0.17f, 0.17f),
        )
        blooms.forEachIndexed { index, (dx, dy, bloomSize) ->
            drawSimpleBloom(
                canvas,
                centerX + size * dx,
                centerY + size * dy,
                size * bloomSize,
                stroke,
                fill,
                if (index == 1) 7 else 5,
            )
        }
        drawLeaf(canvas, centerX - size * 0.37f, centerY + size * 0.15f, size * 0.12f, size * 0.25f, -54f, stroke)
        drawLeaf(canvas, centerX + size * 0.05f, centerY + size * 0.16f, size * 0.12f, size * 0.27f, 48f, stroke)
        drawLeaf(canvas, centerX + size * 0.30f, centerY + size * 0.05f, size * 0.11f, size * 0.23f, 56f, stroke)
    }

    private fun drawLily(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        size: Float,
        stroke: Paint,
        fill: Paint,
    ) {
        repeat(6) { index ->
            canvas.save()
            canvas.rotate(index * 60f + if (index % 2 == 0) -4f else 4f, centerX, centerY)
            val petal = Path().apply {
                moveTo(centerX, centerY)
                cubicTo(
                    centerX - size * 0.13f, centerY - size * 0.16f,
                    centerX - size * 0.11f, centerY - size * 0.40f,
                    centerX, centerY - size * 0.47f,
                )
                cubicTo(
                    centerX + size * 0.11f, centerY - size * 0.40f,
                    centerX + size * 0.13f, centerY - size * 0.16f,
                    centerX, centerY,
                )
            }
            canvas.drawPath(petal, stroke)
            canvas.restore()
        }
        repeat(6) { index ->
            val angle = Math.toRadians(index * 60.0)
            val endX = centerX + kotlin.math.cos(angle).toFloat() * size * 0.24f
            val endY = centerY + kotlin.math.sin(angle).toFloat() * size * 0.24f
            canvas.drawLine(centerX, centerY, endX, endY, stroke)
            canvas.drawCircle(endX, endY, size * 0.022f, fill)
        }
    }

    private fun drawMagnolia(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        size: Float,
        stroke: Paint,
        fill: Paint,
    ) {
        repeat(7) { index ->
            canvas.save()
            canvas.rotate(index * (360f / 7f) + 7f, centerX, centerY)
            val petal = Path().apply {
                moveTo(centerX, centerY + size * 0.04f)
                cubicTo(
                    centerX - size * 0.20f, centerY - size * 0.14f,
                    centerX - size * 0.17f, centerY - size * 0.38f,
                    centerX, centerY - size * 0.45f,
                )
                cubicTo(
                    centerX + size * 0.17f, centerY - size * 0.38f,
                    centerX + size * 0.20f, centerY - size * 0.14f,
                    centerX, centerY + size * 0.04f,
                )
            }
            canvas.drawPath(petal, stroke)
            canvas.restore()
        }
        canvas.drawCircle(centerX, centerY, size * 0.07f, fill)
        drawLeaf(canvas, centerX + size * 0.28f, centerY + size * 0.28f, size * 0.16f, size * 0.34f, 50f, stroke)
    }

    private fun drawPeony(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        size: Float,
        stroke: Paint,
        fill: Paint,
    ) {
        drawPetalRing(canvas, centerX, centerY, size, 14, 0.43f, 0.17f, stroke, 3f)
        drawPetalRing(canvas, centerX, centerY, size, 11, 0.31f, 0.14f, stroke, 18f)
        drawPetalRing(canvas, centerX, centerY, size, 8, 0.20f, 0.11f, stroke, 6f)
        canvas.drawCircle(centerX, centerY, size * 0.065f, fill)
    }

    private fun drawDahlia(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        size: Float,
        stroke: Paint,
        fill: Paint,
    ) {
        drawPetalRing(canvas, centerX, centerY, size, 20, 0.46f, 0.075f, stroke, 0f)
        drawPetalRing(canvas, centerX, centerY, size, 16, 0.34f, 0.065f, stroke, 11f)
        drawPetalRing(canvas, centerX, centerY, size, 12, 0.23f, 0.055f, stroke, 4f)
        canvas.drawCircle(centerX, centerY, size * 0.055f, fill)
    }

    private fun drawSunflower(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        size: Float,
        stroke: Paint,
        fill: Paint,
    ) {
        drawPetalRing(canvas, centerX, centerY, size, 24, 0.46f, 0.08f, stroke, 0f)
        drawPetalRing(canvas, centerX, centerY, size, 16, 0.33f, 0.07f, stroke, 11f)
        canvas.drawCircle(centerX, centerY, size * 0.17f, stroke)
        repeat(12) { index ->
            val angle = Math.toRadians(index * 30.0)
            canvas.drawCircle(
                centerX + kotlin.math.cos(angle).toFloat() * size * 0.105f,
                centerY + kotlin.math.sin(angle).toFloat() * size * 0.105f,
                size * 0.014f,
                fill,
            )
        }
    }

    private fun drawDaisySpray(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        size: Float,
        stroke: Paint,
        fill: Paint,
    ) {
        val baseY = centerY + size * 0.43f
        val blooms = listOf(
            Triple(centerX - size * 0.25f, centerY - size * 0.18f, size * 0.34f),
            Triple(centerX + size * 0.03f, centerY - size * 0.31f, size * 0.38f),
            Triple(centerX + size * 0.29f, centerY - size * 0.04f, size * 0.29f),
        )
        blooms.forEach { (x, y, bloomSize) ->
            canvas.drawLine(centerX, baseY, x, y, stroke)
            drawSimpleBloom(canvas, x, y, bloomSize, stroke, fill, 9)
        }
        drawLeaf(canvas, centerX - size * 0.18f, centerY + size * 0.20f, size * 0.15f, size * 0.32f, -55f, stroke)
        drawLeaf(canvas, centerX + size * 0.20f, centerY + size * 0.24f, size * 0.14f, size * 0.30f, 54f, stroke)
    }

    private fun drawChineseKnotFlower(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        size: Float,
        stroke: Paint,
        fill: Paint,
    ) {
        drawDoubleLotus(canvas, centerX, centerY - size * 0.19f, size * 0.62f, stroke)
        val knotY = centerY + size * 0.20f
        val diamond = Path().apply {
            moveTo(centerX, knotY - size * 0.13f)
            lineTo(centerX + size * 0.14f, knotY)
            lineTo(centerX, knotY + size * 0.13f)
            lineTo(centerX - size * 0.14f, knotY)
            close()
            moveTo(centerX - size * 0.14f, knotY)
            lineTo(centerX + size * 0.14f, knotY)
            moveTo(centerX, knotY + size * 0.13f)
            lineTo(centerX - size * 0.08f, centerY + size * 0.48f)
            moveTo(centerX, knotY + size * 0.13f)
            lineTo(centerX + size * 0.08f, centerY + size * 0.48f)
        }
        canvas.drawPath(diamond, stroke)
        canvas.drawCircle(centerX, knotY, size * 0.025f, fill)
    }

    private fun drawCornerBlossom(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        size: Float,
        stroke: Paint,
        fill: Paint,
        mirror: Boolean,
    ) {
        canvas.save()
        if (mirror) canvas.scale(-1f, 1f, centerX, centerY)
        val branch = Path().apply {
            moveTo(centerX - size * 0.44f, centerY + size * 0.42f)
            cubicTo(
                centerX - size * 0.14f, centerY + size * 0.18f,
                centerX + size * 0.10f, centerY - size * 0.12f,
                centerX + size * 0.43f, centerY - size * 0.40f,
            )
        }
        canvas.drawPath(branch, stroke)
        drawSimpleBloom(canvas, centerX - size * 0.12f, centerY + size * 0.07f, size * 0.28f, stroke, fill, 6)
        drawSimpleBloom(canvas, centerX + size * 0.20f, centerY - size * 0.22f, size * 0.23f, stroke, fill, 5)
        drawLeaf(canvas, centerX - size * 0.25f, centerY + size * 0.27f, size * 0.14f, size * 0.29f, -58f, stroke)
        drawLeaf(canvas, centerX + size * 0.29f, centerY - size * 0.08f, size * 0.12f, size * 0.26f, 52f, stroke)
        canvas.restore()
    }

    private fun drawWreath(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        size: Float,
        stroke: Paint,
        fill: Paint,
        crescent: Boolean,
    ) {
        val oval = RectF(
            centerX - size * 0.34f,
            centerY - size * 0.44f,
            centerX + size * 0.34f,
            centerY + size * 0.44f,
        )
        if (crescent) {
            canvas.drawArc(oval, 82f, 196f, false, stroke)
            canvas.drawArc(oval, 104f, 150f, false, stroke)
        } else {
            canvas.drawOval(oval, stroke)
        }
        val leafPoints = if (crescent) {
            listOf(-0.29f to -0.28f, -0.35f to -0.05f, -0.32f to 0.19f, -0.19f to 0.36f)
        } else {
            listOf(-0.29f to -0.27f, 0.29f to -0.27f, -0.31f to 0.19f, 0.31f to 0.19f)
        }
        leafPoints.forEachIndexed { index, (dx, dy) ->
            drawLeaf(
                canvas,
                centerX + size * dx,
                centerY + size * dy,
                size * 0.10f,
                size * 0.22f,
                if (dx < 0f) -55f else 55f,
                stroke,
            )
            if (index % 2 == 0) {
                drawSimpleBloom(
                    canvas,
                    centerX + size * (dx * 0.90f),
                    centerY + size * (dy - 0.06f),
                    size * 0.17f,
                    stroke,
                    fill,
                    5,
                )
            }
        }
        if (!crescent) {
            drawSimpleBloom(canvas, centerX, centerY + size * 0.39f, size * 0.25f, stroke, fill, 7)
        }
    }

    private fun drawDoubleLotus(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        size: Float,
        stroke: Paint,
    ) {
        drawLotus(canvas, centerX, centerY, size, stroke)
        drawLotus(canvas, centerX, centerY + size * 0.05f, size * 0.58f, Paint(stroke).apply { strokeWidth *= 0.82f })
        canvas.drawArc(
            RectF(
                centerX - size * 0.45f,
                centerY + size * 0.25f,
                centerX + size * 0.45f,
                centerY + size * 0.48f,
            ),
            8f,
            164f,
            false,
            stroke,
        )
    }

    private fun drawRoseSpray(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        size: Float,
        stroke: Paint,
        fill: Paint,
    ) {
        val baseY = centerY + size * 0.42f
        val leftX = centerX - size * 0.20f
        val rightX = centerX + size * 0.22f
        val leftY = centerY - size * 0.17f
        val rightY = centerY + size * 0.01f
        canvas.drawLine(centerX, baseY, leftX, leftY, stroke)
        canvas.drawLine(centerX, baseY, rightX, rightY, stroke)
        drawRose(canvas, leftX, leftY, size * 0.48f, stroke)
        drawRose(canvas, rightX, rightY, size * 0.36f, stroke)
        canvas.drawCircle(leftX, leftY, size * 0.025f, fill)
        drawLeaf(canvas, centerX - size * 0.18f, centerY + size * 0.23f, size * 0.14f, size * 0.30f, -58f, stroke)
        drawLeaf(canvas, centerX + size * 0.20f, centerY + size * 0.28f, size * 0.14f, size * 0.30f, 58f, stroke)
    }

    private fun drawPlumBranch(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        size: Float,
        stroke: Paint,
        fill: Paint,
    ) {
        val branch = Path().apply {
            moveTo(centerX - size * 0.45f, centerY + size * 0.34f)
            cubicTo(
                centerX - size * 0.20f, centerY + size * 0.17f,
                centerX + size * 0.04f, centerY - size * 0.06f,
                centerX + size * 0.43f, centerY - size * 0.35f,
            )
            moveTo(centerX - size * 0.02f, centerY - size * 0.03f)
            lineTo(centerX - size * 0.10f, centerY - size * 0.33f)
        }
        canvas.drawPath(branch, stroke)
        listOf(
            -0.27f to 0.20f,
            -0.08f to 0.00f,
            -0.09f to -0.31f,
            0.17f to -0.15f,
            0.37f to -0.32f,
        ).forEachIndexed { index, (dx, dy) ->
            drawSimpleBloom(
                canvas,
                centerX + size * dx,
                centerY + size * dy,
                size * if (index == 2) 0.22f else 0.18f,
                stroke,
                fill,
                5,
            )
        }
    }

    private fun drawOrchidSpray(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        size: Float,
        stroke: Paint,
        fill: Paint,
    ) {
        val baseY = centerY + size * 0.45f
        listOf(-0.22f to -0.16f, 0.03f to -0.31f, 0.27f to -0.05f).forEachIndexed { index, (dx, dy) ->
            val x = centerX + size * dx
            val y = centerY + size * dy
            canvas.drawLine(centerX, baseY, x, y, stroke)
            drawOrchid(canvas, x, y, size * if (index == 1) 0.38f else 0.30f, stroke)
            canvas.drawCircle(x, y, size * 0.018f, fill)
        }
        drawLeaf(canvas, centerX - size * 0.22f, centerY + size * 0.23f, size * 0.11f, size * 0.42f, -34f, stroke)
        drawLeaf(canvas, centerX + size * 0.23f, centerY + size * 0.25f, size * 0.11f, size * 0.43f, 34f, stroke)
    }

    private fun drawCamelliaPair(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        size: Float,
        stroke: Paint,
        fill: Paint,
    ) {
        val leftX = centerX - size * 0.19f
        val rightX = centerX + size * 0.21f
        drawCamellia(canvas, leftX, centerY - size * 0.10f, size * 0.54f, stroke, fill)
        drawCamellia(canvas, rightX, centerY + size * 0.13f, size * 0.42f, stroke, fill)
        canvas.drawLine(centerX - size * 0.05f, centerY + size * 0.42f, leftX, centerY, stroke)
        canvas.drawLine(centerX - size * 0.05f, centerY + size * 0.42f, rightX, centerY + size * 0.20f, stroke)
        drawLeaf(canvas, centerX, centerY + size * 0.31f, size * 0.15f, size * 0.30f, 42f, stroke)
    }

    private fun drawChrysanthemumSpray(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        size: Float,
        stroke: Paint,
        fill: Paint,
    ) {
        val baseY = centerY + size * 0.45f
        val blooms = listOf(
            Triple(centerX - size * 0.24f, centerY - size * 0.10f, size * 0.42f),
            Triple(centerX + size * 0.07f, centerY - size * 0.28f, size * 0.50f),
            Triple(centerX + size * 0.30f, centerY + size * 0.06f, size * 0.31f),
        )
        blooms.forEachIndexed { index, (x, y, bloomSize) ->
            canvas.drawLine(centerX, baseY, x, y, stroke)
            if (index == 1) drawRealisticChrysanthemum(canvas, x, y, bloomSize, stroke, fill)
            else drawChrysanthemum(canvas, x, y, bloomSize, stroke, fill, petals = 14)
        }
        drawLeaf(canvas, centerX - size * 0.20f, centerY + size * 0.25f, size * 0.14f, size * 0.29f, -58f, stroke)
        drawLeaf(canvas, centerX + size * 0.22f, centerY + size * 0.29f, size * 0.14f, size * 0.31f, 58f, stroke)
    }

    private fun drawMemorialWreath(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        size: Float,
        stroke: Paint,
        fill: Paint,
    ) {
        drawWreath(canvas, centerX, centerY - size * 0.03f, size * 0.90f, stroke, fill, crescent = false)
        drawRealisticChrysanthemum(canvas, centerX, centerY + size * 0.32f, size * 0.28f, stroke, fill)
        drawLeaf(canvas, centerX - size * 0.19f, centerY + size * 0.36f, size * 0.12f, size * 0.28f, -70f, stroke)
        drawLeaf(canvas, centerX + size * 0.19f, centerY + size * 0.36f, size * 0.12f, size * 0.28f, 70f, stroke)
    }

    private fun drawRibbonBouquet(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        size: Float,
        stroke: Paint,
        fill: Paint,
    ) {
        drawLeafyBouquet(canvas, centerX, centerY - size * 0.06f, size * 0.88f, stroke, fill)
        val tieY = centerY + size * 0.29f
        val ribbon = Path().apply {
            moveTo(centerX, tieY)
            cubicTo(centerX - size * 0.22f, tieY - size * 0.10f, centerX - size * 0.27f, tieY + size * 0.06f, centerX - size * 0.08f, tieY + size * 0.07f)
            cubicTo(centerX + size * 0.02f, tieY + size * 0.05f, centerX + size * 0.02f, tieY + size * 0.01f, centerX, tieY)
            cubicTo(centerX + size * 0.22f, tieY - size * 0.10f, centerX + size * 0.27f, tieY + size * 0.06f, centerX + size * 0.08f, tieY + size * 0.07f)
            moveTo(centerX - size * 0.03f, tieY + size * 0.05f)
            lineTo(centerX - size * 0.12f, centerY + size * 0.47f)
            moveTo(centerX + size * 0.03f, tieY + size * 0.05f)
            lineTo(centerX + size * 0.14f, centerY + size * 0.47f)
        }
        canvas.drawPath(ribbon, stroke)
    }

    private fun drawLeafGarland(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        size: Float,
        stroke: Paint,
        fill: Paint,
    ) {
        val vine = Path().apply {
            moveTo(centerX - size * 0.48f, centerY + size * 0.05f)
            cubicTo(
                centerX - size * 0.20f, centerY - size * 0.17f,
                centerX + size * 0.20f, centerY + size * 0.17f,
                centerX + size * 0.48f, centerY - size * 0.05f,
            )
        }
        canvas.drawPath(vine, stroke)
        listOf(-0.34f to -48f, -0.14f to 55f, 0.10f to -52f, 0.31f to 50f).forEachIndexed { index, (dx, rotation) ->
            val y = centerY + size * if (index % 2 == 0) -0.03f else 0.04f
            drawLeaf(canvas, centerX + size * dx, y, size * 0.11f, size * 0.24f, rotation, stroke)
        }
        listOf(-0.23f, 0f, 0.25f).forEachIndexed { index, dx ->
            drawSimpleBloom(
                canvas,
                centerX + size * dx,
                centerY + size * if (index == 1) -0.02f else 0.02f,
                size * if (index == 1) 0.22f else 0.17f,
                stroke,
                fill,
                5,
            )
        }
    }

    private fun drawBudBranch(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        size: Float,
        stroke: Paint,
        fill: Paint,
    ) {
        val stem = Path().apply {
            moveTo(centerX - size * 0.36f, centerY + size * 0.43f)
            cubicTo(
                centerX - size * 0.15f, centerY + size * 0.17f,
                centerX + size * 0.08f, centerY - size * 0.10f,
                centerX + size * 0.30f, centerY - size * 0.43f,
            )
        }
        canvas.drawPath(stem, stroke)
        listOf(-0.17f to 0.15f, 0.02f to -0.08f, 0.23f to -0.33f).forEachIndexed { index, (dx, dy) ->
            val x = centerX + size * dx
            val y = centerY + size * dy
            canvas.save()
            canvas.rotate(35f, x, y)
            canvas.drawOval(
                RectF(x - size * 0.07f, y - size * 0.15f, x + size * 0.07f, y + size * 0.05f),
                if (index == 1) fill else stroke,
            )
            canvas.restore()
            drawLeaf(canvas, x - size * 0.08f, y + size * 0.12f, size * 0.10f, size * 0.22f, -52f, stroke)
        }
    }

    private fun drawThreeBloom(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        size: Float,
        stroke: Paint,
        fill: Paint,
    ) {
        val baseY = centerY + size * 0.43f
        val positions = listOf(
            Triple(centerX - size * 0.27f, centerY - size * 0.08f, size * 0.38f),
            Triple(centerX, centerY - size * 0.29f, size * 0.45f),
            Triple(centerX + size * 0.28f, centerY + size * 0.01f, size * 0.34f),
        )
        positions.forEachIndexed { index, (x, y, bloomSize) ->
            canvas.drawLine(centerX, baseY, x, y, stroke)
            when (index) {
                0 -> drawSimpleBloom(canvas, x, y, bloomSize, stroke, fill, 7)
                1 -> drawPeony(canvas, x, y, bloomSize, stroke, fill)
                else -> drawRose(canvas, x, y, bloomSize, stroke)
            }
        }
        drawLeaf(canvas, centerX - size * 0.16f, centerY + size * 0.25f, size * 0.14f, size * 0.29f, -55f, stroke)
        drawLeaf(canvas, centerX + size * 0.18f, centerY + size * 0.28f, size * 0.14f, size * 0.30f, 55f, stroke)
    }

    private fun drawFanFloral(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        size: Float,
        stroke: Paint,
        fill: Paint,
    ) {
        val baseY = centerY + size * 0.42f
        val stems = listOf(
            -0.34f to -0.08f,
            -0.18f to -0.27f,
            0f to -0.38f,
            0.19f to -0.25f,
            0.35f to -0.06f,
        )
        stems.forEachIndexed { index, (dx, dy) ->
            val x = centerX + size * dx
            val y = centerY + size * dy
            canvas.drawLine(centerX, baseY, x, y, stroke)
            drawSimpleBloom(
                canvas,
                x,
                y,
                size * if (index == 2) 0.25f else 0.18f,
                stroke,
                fill,
                if (index % 2 == 0) 6 else 5,
            )
        }
        canvas.drawArc(
            RectF(centerX - size * 0.36f, centerY + size * 0.24f, centerX + size * 0.36f, centerY + size * 0.48f),
            8f,
            164f,
            false,
            stroke,
        )
    }

    private fun drawPetalRing(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        size: Float,
        petals: Int,
        lengthFactor: Float,
        widthFactor: Float,
        paint: Paint,
        rotationOffset: Float,
    ) {
        repeat(petals) { index ->
            canvas.save()
            val irregularOffset = when (index % 3) {
                0 -> -2.5f
                1 -> 1.5f
                else -> 0f
            }
            canvas.rotate(index * (360f / petals) + rotationOffset + irregularOffset, centerX, centerY)
            val petal = Path().apply {
                moveTo(centerX, centerY + size * 0.015f)
                cubicTo(
                    centerX - size * widthFactor,
                    centerY - size * lengthFactor * 0.40f,
                    centerX - size * widthFactor * 0.44f,
                    centerY - size * lengthFactor,
                    centerX,
                    centerY - size * lengthFactor,
                )
                cubicTo(
                    centerX + size * widthFactor * 0.44f,
                    centerY - size * lengthFactor,
                    centerX + size * widthFactor,
                    centerY - size * lengthFactor * 0.40f,
                    centerX,
                    centerY + size * 0.015f,
                )
            }
            canvas.drawPath(petal, paint)
            canvas.restore()
        }
    }

    private fun drawLeaf(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        width: Float,
        height: Float,
        rotation: Float,
        paint: Paint,
    ) {
        canvas.save()
        canvas.rotate(rotation, centerX, centerY)
        val leaf = Path().apply {
            moveTo(centerX, centerY + height / 2f)
            cubicTo(
                centerX - width / 2f, centerY + height * 0.12f,
                centerX - width / 2f, centerY - height * 0.25f,
                centerX, centerY - height / 2f,
            )
            cubicTo(
                centerX + width / 2f, centerY - height * 0.25f,
                centerX + width / 2f, centerY + height * 0.12f,
                centerX, centerY + height / 2f,
            )
            moveTo(centerX, centerY + height * 0.42f)
            lineTo(centerX, centerY - height * 0.40f)
        }
        canvas.drawPath(leaf, paint)
        canvas.restore()
    }

    private fun drawSimpleBloom(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        size: Float,
        stroke: Paint,
        fill: Paint,
        petals: Int,
    ) {
        repeat(petals) { index ->
            canvas.save()
            canvas.rotate(index * (360f / petals), centerX, centerY)
            canvas.drawOval(
                RectF(centerX - size * 0.12f, centerY - size * 0.45f, centerX + size * 0.12f, centerY),
                stroke,
            )
            canvas.restore()
        }
        canvas.drawCircle(centerX, centerY, size * 0.08f, fill)
    }
}
