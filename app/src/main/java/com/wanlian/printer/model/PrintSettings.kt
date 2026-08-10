package com.wanlian.printer.model

import kotlin.math.roundToInt

data class PrintSettings(
    val text: String = "沉痛悼念\n王先生千古",
    val autoFontSize: Boolean = true,
    val fontSizeDots: Float = 116f,
    val characterSpacingDots: Float = 16f,
    val topMarginMm: Float = 8f,
    val bottomMarginMm: Float = 8f,
    val paperWidthMm: Float = 100f,
    val autoPaperLength: Boolean = true,
    val paperLengthMm: Float = 200f,
    val density: Int = 10,
    val speedInchesPerSecond: Float = 3f,
    val threshold: Int = 160,
    val textAlignment: TextHorizontalAlignment = TextHorizontalAlignment.CENTER,
    val textWeight: TextWeight = TextWeight.BOLD,
    val border: BorderSettings = BorderSettings(),
    val printDirection: PrintDirection = PrintDirection.FORWARD,
    val reversePrinting: Boolean = false,
    val bitmapChunkSize: Int = 1024,
    val chunkDelayMs: Long = 8L,
)

object PrintUnits {
    const val DPI = 203
    private const val MILLIMETERS_PER_INCH = 25.4f
    const val DOTS_PER_MM = DPI / MILLIMETERS_PER_INCH

    fun mmToDots(mm: Float): Int = (mm * DPI / MILLIMETERS_PER_INCH).roundToInt()

    fun dotsToMm(dots: Int): Float = dots * MILLIMETERS_PER_INCH / DPI
}

enum class BluetoothTransport(val label: String) {
    CLASSIC("SPP"),
    BLE("BLE"),
}

data class PrinterDevice(
    val name: String,
    val address: String,
    val transports: Set<BluetoothTransport>,
    val bonded: Boolean = false,
    val rssi: Int? = null,
) {
    val displayName: String get() = name.ifBlank { "未知设备" }
    val transportLabel: String get() = transports.joinToString(" / ") { it.label }
}

enum class ConnectionStatus(val label: String) {
    DISCONNECTED("未连接"),
    CONNECTING("连接中"),
    CONNECTED("已连接"),
    DISCONNECTING("断开中"),
    ERROR("连接异常"),
}
