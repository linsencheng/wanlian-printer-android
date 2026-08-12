package com.wanlian.printer.model

data class CutGuideSettings(
    val enabled: Boolean = false,
    val style: CutGuideStyle = CutGuideStyle.INWARD_V,
    val bottomOffsetMm: Float = 6f,
    val edgeInsetMm: Float = 3f,
    val notchDepthMm: Float = 25f,
    val lineWidthMm: Float = 0.4f,
)

enum class CutGuideStyle(val label: String) {
    STRAIGHT("直线"),
    INWARD_V("燕尾 / 内三角"),
}
