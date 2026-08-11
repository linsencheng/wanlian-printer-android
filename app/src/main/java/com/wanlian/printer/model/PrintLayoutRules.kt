package com.wanlian.printer.model

import kotlin.math.max

object PrintLayoutRules {
    const val DEFAULT_PREFERRED_AUTO_LENGTH_MM = 991f

    fun resolveAutoLengthMm(
        requiredLengthMm: Float,
        preferredAutoLengthMm: Float = DEFAULT_PREFERRED_AUTO_LENGTH_MM,
    ): Float = max(
        requiredLengthMm.coerceAtLeast(1f),
        preferredAutoLengthMm.coerceAtLeast(1f),
    )

    fun cutGuideReserveMm(settings: CutGuideSettings): Float {
        if (!settings.enabled) return 0f
        val guideHeight = when (settings.style) {
            CutGuideStyle.STRAIGHT -> 0f
            CutGuideStyle.INWARD_V -> settings.notchDepthMm.coerceAtLeast(0f)
        }
        return settings.bottomOffsetMm.coerceAtLeast(0f) + guideHeight + CUT_GUIDE_SAFE_SPACING_MM
    }

    fun cutGuideSegmentsMm(
        widthMm: Float,
        heightMm: Float,
        settings: CutGuideSettings,
    ): List<GuideSegmentMm> {
        if (!settings.enabled) return emptyList()
        val safeWidth = widthMm.coerceAtLeast(1f)
        val safeHeight = heightMm.coerceAtLeast(1f)
        val minimumVerticalGap = (safeHeight / 2f).coerceAtMost(0.5f)
        val sideInset = settings.edgeInsetMm
            .coerceAtLeast(0f)
            .coerceAtMost((safeWidth / 2f - 0.5f).coerceAtLeast(0f))
        val bottomY = (safeHeight - settings.bottomOffsetMm.coerceAtLeast(0f))
            .coerceIn(minimumVerticalGap, safeHeight)
        return when (settings.style) {
            CutGuideStyle.STRAIGHT -> listOf(
                GuideSegmentMm(sideInset, bottomY, safeWidth - sideInset, bottomY),
            )
            CutGuideStyle.INWARD_V -> {
                val depth = settings.notchDepthMm
                    .coerceAtLeast(minimumVerticalGap)
                    .coerceAtMost(bottomY)
                val centerTipY = bottomY - depth
                val centerX = safeWidth / 2f
                listOf(
                    GuideSegmentMm(sideInset, bottomY, centerX, centerTipY),
                    GuideSegmentMm(safeWidth - sideInset, bottomY, centerX, centerTipY),
                )
            }
        }
    }

    fun footerReserveMm(settings: FooterLabelSettings): Float {
        if (!settings.enabled) return 0f
        val hasBodyText = settings.text.isNotBlank() || settings.secondaryText.isNotBlank()
        val columns = if (hasBodyText) footerColumns(settings) else emptyList()
        val textUnits = when {
            columns.isEmpty() -> 0
            settings.orientation == FooterTextOrientation.VERTICAL ->
                columns.maxOfOrNull(::codePointCount).orZero().coerceAtLeast(1)
            else -> columns.size.coerceAtLeast(1)
        }
        val textHeightDots = if (textUnits == 0) 0f else {
            settings.fontSizeDots.coerceAtLeast(8f) * textUnits +
                settings.spacingDots.coerceAtLeast(0f) * (textUnits - 1).coerceAtLeast(0)
        }
        val persons = settings.persons.take(PersonLayoutRules.MAX_PERSONS)
            .filter { it.verticalText.isNotBlank() }
        val personUnits = when {
            persons.isEmpty() -> 0
            settings.personLayout == PersonLayout.SEQUENTIAL -> persons.sumOf {
                codePointCount(it.verticalText)
            }
            else -> persons.maxOf { codePointCount(it.verticalText) }
        }
        val personHeightDots = if (personUnits == 0) 0f else {
            settings.personFontSizeDots.coerceIn(
                PersonLayoutRules.MIN_FONT_SIZE_DOTS,
                PersonLayoutRules.MAX_FONT_SIZE_DOTS,
            ) * personUnits + settings.spacingDots.coerceAtLeast(0f) *
                (personUnits - 1).coerceAtLeast(0)
        }
        val personTopGapMm = if (hasBodyText && persons.isNotEmpty()) 3f else 0f
        val flowerReserveMm = if (
            settings.flower.enabled && settings.flower.style != FlowerStyle.NONE
        ) {
            settings.flower.sizeMm.coerceIn(
                FlowerAdjustmentLimits.MIN_SIZE_MM,
                FlowerAdjustmentLimits.MAX_SIZE_MM,
            ) + settings.flower.offsetYmm.coerceIn(
                0f,
                FlowerAdjustmentLimits.MAX_OFFSET_MM,
            ) + 3f
        } else {
            0f
        }
        return settings.distanceFromMainMm.coerceAtLeast(0f) +
            PrintUnits.dotsToMm(textHeightDots.toInt()) +
            personTopGapMm +
            settings.personGroupOffsetYMm.coerceAtLeast(0f) +
            PrintUnits.dotsToMm(personHeightDots.toInt()) +
            flowerReserveMm +
            settings.bottomMarginMm.coerceAtLeast(0f)
    }

    internal fun footerColumns(settings: FooterLabelSettings): List<String> =
        FooterTextLayoutRules.lines(settings)

    private fun codePointCount(value: String): Int = value.codePointCount(0, value.length)

    private fun Int?.orZero(): Int = this ?: 0

    private const val CUT_GUIDE_SAFE_SPACING_MM = 4f
}

data class GuideSegmentMm(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
)

enum class PrintGate {
    ALLOWED,
    BLOCKED_CONNECTING,
    BLOCKED_DISCONNECTED,
    BLOCKED_PRINTING,
    ;

    companion object {
        fun resolve(connectionStatus: ConnectionStatus, isPrinting: Boolean): PrintGate = when {
            isPrinting -> BLOCKED_PRINTING
            connectionStatus == ConnectionStatus.CONNECTED -> ALLOWED
            connectionStatus == ConnectionStatus.CONNECTING ||
                connectionStatus == ConnectionStatus.DISCONNECTING -> BLOCKED_CONNECTING
            else -> BLOCKED_DISCONNECTED
        }
    }
}
