package com.wanlian.printer.model

enum class BorderStyle(val label: String) {
    NONE("无"),
    DOUBLE_LINE("双直线"),
    THIN_GREEK_KEY("细回纹"),
    THICK_GREEK_KEY("粗回纹"),
    WAVE("波浪纹"),
    CLOUD("云纹"),
    SCALLOP("半圆连续纹"),
    SWIRL("卷草纹"),
}

enum class BorderPosition(val label: String) {
    BOTH("左右"),
    LEFT("仅左"),
    RIGHT("仅右"),
}

data class BorderSettings(
    val style: BorderStyle = BorderStyle.DOUBLE_LINE,
    val position: BorderPosition = BorderPosition.BOTH,
    val widthMm: Float = 5f,
    val edgeInsetMm: Float = 3f,
    val strokeWidthMm: Float = 0.8f,
    val patternUnitHeightMm: Float = 12f,
    val textGapMm: Float = 4f,
)

enum class TextHorizontalAlignment(val label: String) {
    LEFT("靠左"),
    CENTER("居中"),
    RIGHT("靠右"),
}

enum class TextWeight(val label: String) {
    NORMAL("常规"),
    BOLD("粗体"),
}

enum class PrintDirection(val label: String, val tsplValue: Int) {
    FORWARD("正向", 1),
    REVERSE("反向", 0),
}
