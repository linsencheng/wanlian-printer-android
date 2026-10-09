package com.wanlian.printer.model

import kotlin.math.abs

data class PersonBlockSettings(
    val enabled: Boolean = false,
    val persons: List<FooterPerson> = emptyList(),
    val layout: PersonLayout = PersonLayout.PARALLEL_COLUMNS,
    val placementMode: PersonPlacementMode = PersonPlacementMode.SIDE_OVERLAY,
    val personInsertIndex: Int = 4,
    val positionXNorm: Float = DEFAULT_POSITION_X_NORM,
    val positionYNorm: Float = DEFAULT_POSITION_Y_NORM,
    val offsetXMm: Float = 0f,
    val offsetYMm: Float = 0f,
    val columnGapMm: Float = 6f,
    val fontId: String = "elegant-song",
    val fontSizeDots: Float = DEFAULT_FONT_SIZE_DOTS,
    val characterSpacingDots: Float = 8f,
) {
    val participatesInMainTextFlow: Boolean
        get() = enabled &&
            placementMode == PersonPlacementMode.INLINE_INSERT &&
            persons.any { it.verticalText.isNotBlank() }

    fun movedBy(deltaXMm: Float, deltaYMm: Float): PersonBlockSettings = copy(
        offsetXMm = (offsetXMm + deltaXMm).coerceIn(MIN_OFFSET_X_MM, MAX_OFFSET_X_MM),
        offsetYMm = (offsetYMm + deltaYMm).coerceIn(MIN_OFFSET_Y_MM, MAX_OFFSET_Y_MM),
    )

    companion object {
        const val DEFAULT_POSITION_X_NORM = 0.5f
        const val DEFAULT_POSITION_Y_NORM = 0.55f
        const val DEFAULT_FONT_SIZE_DOTS = 72f
        const val MIN_OFFSET_X_MM = -100f
        const val MAX_OFFSET_X_MM = 100f
        const val MIN_OFFSET_Y_MM = -1000f
        const val MAX_OFFSET_Y_MM = 1000f
    }
}

enum class PersonPlacementMode(val label: String) {
    SIDE_OVERLAY("旁置"),
    INLINE_INSERT("插入正文"),
}

enum class PersonBlockHorizontalPreset(
    val label: String,
    val positionXNorm: Float?,
) {
    LEFT("左侧", 0.35f),
    CENTER("居中", 0.5f),
    RIGHT("右侧", 0.65f),
    CUSTOM("自定义", null),
    ;

    companion object {
        fun resolve(positionXNorm: Float, offsetXMm: Float): PersonBlockHorizontalPreset {
            if (abs(offsetXMm) > 0.01f) return CUSTOM
            return entries.firstOrNull { preset ->
                preset.positionXNorm?.let { abs(positionXNorm - it) < 0.001f } == true
            } ?: CUSTOM
        }
    }
}
