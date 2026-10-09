package com.wanlian.printer.model

import kotlin.math.roundToInt

data class PrintSettings(
    val text: String = "沉痛悼念\n王先生千古",
    val fontId: String = "song",
    val autoFontSize: Boolean = true,
    val fontSizeDots: Float = 116f,
    val characterSpacingDots: Float = 16f,
    val topMarginMm: Float = 8f,
    val bottomMarginMm: Float = 8f,
    val paperWidthMm: Float = 100f,
    val autoPaperLength: Boolean = true,
    val paperLengthMm: Float = 200f,
    val preferredAutoLengthMm: Float = 991f,
    val density: Int = 10,
    val speedInchesPerSecond: Float = 3f,
    val threshold: Int = 160,
    val textAlignment: TextHorizontalAlignment = TextHorizontalAlignment.CENTER,
    val textWeight: TextWeight = TextWeight.BOLD,
    val closingTextBlock: ClosingTextBlockSettings = ClosingTextBlockSettings(),
    val border: BorderSettings = BorderSettings(),
    val personBlock: PersonBlockSettings = PersonBlockSettings(),
    val footerLabel: FooterLabelSettings = FooterLabelSettings(),
    val cutGuide: CutGuideSettings = CutGuideSettings(),
    val printDirection: PrintDirection = PrintDirection.FORWARD,
    val reversePrinting: Boolean = false,
    val bitmapChunkSize: Int = 1024,
    val chunkDelayMs: Long = 12L,
    // 阶段性排空等待：每发送 drainPauseEveryBytes 字节位图数据后暂停 drainPauseMs，
    // 给打印机 UART/引擎缓冲留出消化时间，避免连续大块位图导致 BLE 桥接芯片 FIFO 溢出。
    // 以下为保守默认值（基于“桥接芯片 FIFO 较小”的假设），真机可据实调优。
    val drainPauseEveryBytes: Int = 8 * 1024,
    val drainPauseMs: Long = 150L,
    // 位图全部发送完成后、发送 PRINT 前，等待打印机完成位图解析/写入帧缓冲。
    val prePrintPauseMs: Long = 500L,
)

object PrintUnits {
    const val DPI = 203
    private const val MILLIMETERS_PER_INCH = 25.4f
    const val DOTS_PER_MM = DPI / MILLIMETERS_PER_INCH

    fun mmToDots(mm: Float): Int = (mm * DPI / MILLIMETERS_PER_INCH).roundToInt()

    fun dotsToMm(dots: Int): Float = dots * MILLIMETERS_PER_INCH / DPI
}

enum class BluetoothTransport(
    val label: String,
    val badgeLabel: String,
    val connectionLabel: String,
) {
    CLASSIC("SPP", "Classic", "Bluetooth Classic (SPP)"),
    BLE("BLE", "BLE", "Bluetooth Low Energy (BLE)"),
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
