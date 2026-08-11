package com.wanlian.printer.model

import kotlin.math.max
import kotlin.math.min

enum class DocumentMode(val label: String) {
    SINGLE("单联"),
    PAIR("双联"),
}

enum class CoupletSide(val label: String) {
    LEFT("左联"),
    RIGHT("右联"),
}

data class CoupletPairDocument(
    val left: PrintSettings,
    val right: PrintSettings,
    val selectedSide: CoupletSide = CoupletSide.LEFT,
    val sharedBorderSettings: Boolean = true,
    val sharedFooterLayout: Boolean = true,
    val sharedPageLength: Boolean = true,
) {
    val selectedSettings: PrintSettings
        get() = if (selectedSide == CoupletSide.LEFT) left else right

    fun select(side: CoupletSide): CoupletPairDocument = copy(selectedSide = side)

    fun replaceSelected(updated: PrintSettings): CoupletPairDocument {
        val selectedApplied = if (selectedSide == CoupletSide.LEFT) {
            copy(left = updated)
        } else {
            copy(right = updated)
        }
        val other = if (selectedSide == CoupletSide.LEFT) selectedApplied.right else selectedApplied.left
        val synchronizedOther = synchronizeSharedSettings(source = updated, target = other)
        return if (selectedSide == CoupletSide.LEFT) {
            selectedApplied.copy(right = synchronizedOther)
        } else {
            selectedApplied.copy(left = synchronizedOther)
        }
    }

    private fun synchronizeSharedSettings(source: PrintSettings, target: PrintSettings): PrintSettings {
        // Every footer setting is side-local. In pair mode, selectedSide controls inscription,
        // flower, page-bottom spacing and cut-guide state independently. The legacy
        // sharedFooterLayout flag remains only for person-block style coordination.
        val sideLocalFooter = target.footerLabel
        val sharedPersonBlock = if (sharedFooterLayout) {
            target.personBlock.copy(
                layout = source.personBlock.layout,
                placementMode = source.personBlock.placementMode,
                columnGapMm = source.personBlock.columnGapMm,
                fontId = source.personBlock.fontId,
                fontSizeDots = source.personBlock.fontSizeDots,
                characterSpacingDots = source.personBlock.characterSpacingDots,
            )
        } else {
            target.personBlock
        }
        return target.copy(
            paperWidthMm = source.paperWidthMm,
            autoPaperLength = source.autoPaperLength,
            paperLengthMm = source.paperLengthMm,
            preferredAutoLengthMm = source.preferredAutoLengthMm,
            topMarginMm = source.topMarginMm,
            bottomMarginMm = target.bottomMarginMm,
            border = if (sharedBorderSettings) source.border else target.border,
            personBlock = sharedPersonBlock,
            footerLabel = sideLocalFooter,
            cutGuide = target.cutGuide,
            density = source.density,
            speedInchesPerSecond = source.speedInchesPerSecond,
            threshold = source.threshold,
            printDirection = source.printDirection,
            reversePrinting = source.reversePrinting,
            bitmapChunkSize = source.bitmapChunkSize,
            chunkDelayMs = source.chunkDelayMs,
        )
    }

    companion object {
        fun fromSingle(source: PrintSettings): CoupletPairDocument = CoupletPairDocument(
            left = source,
            right = source.copy(text = "音容宛在\n千古长存"),
        )
    }
}

object PairLayoutRules {
    fun resolveLengthMm(
        leftRequiredMm: Float,
        rightRequiredMm: Float,
        preferredAutoLengthMm: Float = PrintLayoutRules.DEFAULT_PREFERRED_AUTO_LENGTH_MM,
    ): Float = max(max(leftRequiredMm, rightRequiredMm), preferredAutoLengthMm)

    fun resolveSideLengthsMm(
        leftRequiredMm: Float,
        rightRequiredMm: Float,
        preferredAutoLengthMm: Float = PrintLayoutRules.DEFAULT_PREFERRED_AUTO_LENGTH_MM,
    ): PairResolvedLengths {
        val shared = resolveLengthMm(leftRequiredMm, rightRequiredMm, preferredAutoLengthMm)
        return PairResolvedLengths(leftMm = shared, rightMm = shared)
    }

    fun hasAlignedTextStart(document: CoupletPairDocument): Boolean =
        document.left.topMarginMm == document.right.topMarginMm
}

/**
 * Aligns the other side's footer bottom baseline to the currently selected side.
 * Footer content and decoration stay side-local after this one-shot operation.
 */
object PairFooterAlignmentRules {
    private const val MAX_FOOTER_BOTTOM_MARGIN_MM = 300f

    fun alignOtherToSelected(document: CoupletPairDocument): CoupletPairDocument {
        val source = document.selectedSettings
        val target = if (document.selectedSide == CoupletSide.LEFT) document.right else document.left
        if (!source.footerLabel.enabled || !target.footerLabel.enabled) return document

        val alignedTarget = alignTargetToSource(source = source, target = target)
        return if (document.selectedSide == CoupletSide.LEFT) {
            document.copy(right = alignedTarget)
        } else {
            document.copy(left = alignedTarget)
        }
    }

    fun pageEndBaselineMm(settings: PrintSettings): Float =
        settings.bottomMarginMm.coerceAtLeast(0f) +
            PrintLayoutRules.cutGuideReserveMm(settings.cutGuide) +
            settings.footerLabel.bottomMarginMm.coerceAtLeast(0f) -
            FooterLabelAdjustmentLimits.clampOffsetYMm(settings.footerLabel.offsetYMm)

    private fun alignTargetToSource(source: PrintSettings, target: PrintSettings): PrintSettings {
        val targetFixedSafetyMm = target.bottomMarginMm.coerceAtLeast(0f) +
            PrintLayoutRules.cutGuideReserveMm(target.cutGuide)
        val requestedRelativeBaselineMm = pageEndBaselineMm(source) - targetFixedSafetyMm
        val relativeBaselineMm = requestedRelativeBaselineMm.coerceIn(
            -FooterLabelAdjustmentLimits.MAX_OFFSET_Y_MM,
            MAX_FOOTER_BOTTOM_MARGIN_MM - FooterLabelAdjustmentLimits.MIN_OFFSET_Y_MM,
        )

        // bottomMargin - offset = relativeBaseline. Retain the target fine offset
        // where possible, and only clamp it when the supported ranges require it.
        val minimumOffset = max(
            FooterLabelAdjustmentLimits.MIN_OFFSET_Y_MM,
            -relativeBaselineMm,
        )
        val maximumOffset = min(
            FooterLabelAdjustmentLimits.MAX_OFFSET_Y_MM,
            MAX_FOOTER_BOTTOM_MARGIN_MM - relativeBaselineMm,
        )
        val alignedOffset = FooterLabelAdjustmentLimits.clampOffsetYMm(
            target.footerLabel.offsetYMm,
        ).coerceIn(minimumOffset, maximumOffset)
        val alignedBottomMargin = (relativeBaselineMm + alignedOffset).coerceIn(
            0f,
            MAX_FOOTER_BOTTOM_MARGIN_MM,
        )

        return target.copy(
            footerLabel = target.footerLabel.copy(
                bottomMarginMm = alignedBottomMargin,
                offsetYMm = alignedOffset,
            ),
        )
    }
}

data class PairResolvedLengths(
    val leftMm: Float,
    val rightMm: Float,
)

data class PairPrintJob(
    val side: CoupletSide,
    val settings: PrintSettings,
)

object PairPrintPlan {
    fun jobs(document: CoupletPairDocument): List<PairPrintJob> = listOf(
        PairPrintJob(CoupletSide.LEFT, document.left),
        PairPrintJob(CoupletSide.RIGHT, document.right),
    )
}
