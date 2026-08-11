package com.wanlian.printer.model

import kotlin.math.max

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
        val sharedFooter = if (sharedFooterLayout) {
            target.footerLabel.copy(
                enabled = source.footerLabel.enabled,
                fontId = source.footerLabel.fontId,
                fontSizeDots = source.footerLabel.fontSizeDots,
                spacingDots = source.footerLabel.spacingDots,
                distanceFromMainMm = source.footerLabel.distanceFromMainMm,
                bottomMarginMm = source.footerLabel.bottomMarginMm,
                position = source.footerLabel.position,
                orientation = source.footerLabel.orientation,
                personLayout = source.footerLabel.personLayout,
                personColumnGapMm = source.footerLabel.personColumnGapMm,
                personGroupOffsetXMm = source.footerLabel.personGroupOffsetXMm,
                personGroupOffsetYMm = source.footerLabel.personGroupOffsetYMm,
                personFontId = source.footerLabel.personFontId,
                personFontSizeDots = source.footerLabel.personFontSizeDots,
                flower = source.footerLabel.flower,
            )
        } else {
            target.footerLabel
        }
        return target.copy(
            paperWidthMm = source.paperWidthMm,
            autoPaperLength = source.autoPaperLength,
            paperLengthMm = source.paperLengthMm,
            preferredAutoLengthMm = source.preferredAutoLengthMm,
            topMarginMm = source.topMarginMm,
            bottomMarginMm = source.bottomMarginMm,
            border = if (sharedBorderSettings) source.border else target.border,
            footerLabel = sharedFooter,
            cutGuide = if (sharedFooterLayout) source.cutGuide else target.cutGuide,
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
