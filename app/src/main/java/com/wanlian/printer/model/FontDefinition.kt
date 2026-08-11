package com.wanlian.printer.model

/**
 * A print-font style backed by Android system families. No font binaries are redistributed.
 * Shape adjustments make the options useful even when a device maps several CJK aliases to
 * the same fallback font.
 */
data class FontDefinition(
    val id: String,
    val displayName: String,
    val familyName: String,
    val style: Int,
    val textScaleX: Float = 1f,
    val textSkewX: Float = 0f,
    val source: String = "Android 系统字体",
    val license: String = "设备系统组件，不随 APK 再分发",
    val localFilePath: String? = null,
)

data class ImportedFont(
    val id: String,
    val displayName: String,
    val filePath: String,
)
