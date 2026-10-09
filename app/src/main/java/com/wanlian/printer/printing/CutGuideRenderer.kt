package com.wanlian.printer.printing

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.wanlian.printer.model.CutGuideSettings
import com.wanlian.printer.model.PrintLayoutRules
import com.wanlian.printer.model.PrintUnits
import kotlin.math.max

class CutGuideRenderer {
    fun draw(
        canvas: Canvas,
        widthDots: Int,
        heightDots: Int,
        settings: CutGuideSettings,
        color: Int = Color.WHITE,
    ) {
        if (!settings.enabled) return
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = max(1f, PrintUnits.mmToDots(settings.lineWidthMm.coerceIn(0.2f, 1.5f)).toFloat())
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val segments = PrintLayoutRules.cutGuideSegmentsMm(
            widthMm = PrintUnits.dotsToMm(widthDots),
            heightMm = PrintUnits.dotsToMm(heightDots),
            settings = settings,
        )
        if (segments.isEmpty()) return
        val path = Path().apply {
            segments.forEach { segment ->
                moveTo(
                    PrintUnits.mmToDots(segment.startX).toFloat(),
                    PrintUnits.mmToDots(segment.startY).toFloat(),
                )
                lineTo(PrintUnits.mmToDots(segment.endX).toFloat(), PrintUnits.mmToDots(segment.endY).toFloat())
            }
        }
        canvas.drawPath(path, paint)
    }
}
