package com.wanlian.printer.printing

import android.graphics.Bitmap
import com.wanlian.printer.bluetooth.BluetoothManager
import com.wanlian.printer.model.PrintSettings
import com.wanlian.printer.storage.DiagnosticLogRepository
import kotlinx.coroutines.delay
import java.io.ByteArrayOutputStream
import java.util.Locale
import kotlin.math.min

enum class TsplPrintStage {
    SETTINGS_COMMANDS_SENT,
    BITMAP_SEND_STARTED,
    BITMAP_SEND_COMPLETED,
    PRINT_COMMAND_SENT,
}

data class TsplPrintDiagnostics(
    val paperWidthMm: Float,
    val paperLengthMm: Float,
    val bitmapWidthDots: Int,
    val bitmapHeightDots: Int,
    val widthBytes: Int,
    val bitmapDataBytes: Int,
    val appliedSettings: AppliedPrinterSettings,
)

class TsplPrinter(
    private val bluetoothManager: BluetoothManager,
    private val diagnosticLogs: DiagnosticLogRepository? = null,
) {
    suspend fun applyPrintSettings(settings: PrintSettings): AppliedPrinterSettings {
        check(bluetoothManager.isConnected) { "请先连接打印机" }
        val applied = TsplSettingsCommands.resolve(settings)
        return try {
            bluetoothManager.write(TsplSettingsCommands.build(settings))
            diagnosticLogs?.append(
                "TSPL_SETTINGS",
                "发送完成 density=${applied.density}, speed=${applied.speedInchesPerSecond}",
            )
            applied
        } catch (error: Throwable) {
            diagnosticLogs?.append("TSPL_SETTINGS_FAILED", "打印设置发送失败", error)
            throw error
        }
    }

    suspend fun print(
        rendered: RenderedBitmap,
        settings: PrintSettings,
        onStage: (TsplPrintStage, TsplPrintDiagnostics) -> Unit = { _, _ -> },
        onProgress: (Float) -> Unit = {},
    ) {
        check(bluetoothManager.isConnected) { "请先连接打印机" }
        check(rendered.printMask.width > 0 && rendered.printMask.height > 0) {
            "打印 Bitmap 为空"
        }

        val packed = packPrintMask(
            printMask = rendered.printMask,
            threshold = settings.threshold,
            reversePrinting = settings.reversePrinting,
        )
        check(packed.isNotEmpty()) { "打印 Bitmap 数据为空" }
        val widthBytes = (rendered.printMask.width + 7) / 8
        val appliedSettings = TsplSettingsCommands.resolve(settings)
        val diagnostics = TsplPrintDiagnostics(
            paperWidthMm = rendered.paperWidthMm,
            paperLengthMm = rendered.paperLengthMm,
            bitmapWidthDots = rendered.printMask.width,
            bitmapHeightDots = rendered.printMask.height,
            widthBytes = widthBytes,
            bitmapDataBytes = packed.size,
            appliedSettings = appliedSettings,
        )
        val pageSetup = buildString {
            append("SIZE ${formatMm(rendered.paperWidthMm)} mm,${formatMm(rendered.paperLengthMm)} mm\r\n")
            append("GAP 0 mm,0 mm\r\n")
        }.toByteArray(Charsets.US_ASCII)
        val bitmapHeader = buildString {
            append("CLS\r\n")
            append("BITMAP 0,0,$widthBytes,${rendered.printMask.height},0,")
        }.toByteArray(Charsets.US_ASCII)
        val chunkSize = settings.bitmapChunkSize.coerceIn(256, 4096)
        val totalChunks = (packed.size + chunkSize - 1) / chunkSize
        val transportLabel = bluetoothManager.connectionInfo.value?.transport?.connectionLabel ?: "unknown"
        var stage = PrintUploadStage.PAGE_SETUP
        var offset = 0
        var completedChunks = 0
        var nextProgressMilestone = 10
        diagnosticLogs?.append(
            "TSPL_JOB",
            "开始 width=${rendered.printMask.width}, height=${rendered.printMask.height}, " +
                "paper=${rendered.paperWidthMm}x${rendered.paperLengthMm}mm, bytes=${packed.size}, " +
                "chunks=$totalChunks, chunkSize=$chunkSize, delay=${settings.chunkDelayMs}ms, " +
                "density=${appliedSettings.density}, speed=${appliedSettings.speedInchesPerSecond}, " +
                "transport=$transportLabel",
        )
        try {
            stage = PrintUploadStage.PAGE_SETUP
            bluetoothManager.write(pageSetup)
            stage = PrintUploadStage.SETTINGS
            bluetoothManager.write(TsplSettingsCommands.build(settings))
            onStage(TsplPrintStage.SETTINGS_COMMANDS_SENT, diagnostics)
            onStage(TsplPrintStage.BITMAP_SEND_STARTED, diagnostics)
            stage = PrintUploadStage.BITMAP_HEADER
            bluetoothManager.write(bitmapHeader)
            stage = PrintUploadStage.BITMAP_DATA
            while (offset < packed.size) {
                val count = min(chunkSize, packed.size - offset)
                bluetoothManager.write(packed.copyOfRange(offset, offset + count))
                offset += count
                completedChunks++
                val percent = (offset * 100L / packed.size.coerceAtLeast(1)).toInt()
                if (percent >= nextProgressMilestone || offset == packed.size) {
                    diagnosticLogs?.append(
                        "TSPL_UPLOAD",
                        "progress=$percent%, bytes=$offset/${packed.size}, chunks=$completedChunks/$totalChunks",
                    )
                    while (nextProgressMilestone <= percent) nextProgressMilestone += 10
                }
                onProgress(offset.toFloat() / packed.size.coerceAtLeast(1))
                // SPP printer input buffers are often small; BLE writes are throttled again per MTU.
                delay(settings.chunkDelayMs.coerceIn(0L, 100L))
            }
            onStage(TsplPrintStage.BITMAP_SEND_COMPLETED, diagnostics)
            stage = PrintUploadStage.PRINT_COMMAND
            bluetoothManager.write("\r\nPRINT 1,1\r\n".toByteArray(Charsets.US_ASCII))
            onStage(TsplPrintStage.PRINT_COMMAND_SENT, diagnostics)
            onProgress(1f)
            diagnosticLogs?.append(
                "TSPL_JOB",
                "完成 bytes=$offset/${packed.size}, chunks=$completedChunks/$totalChunks, PRINT 已发送",
            )
        } catch (error: Throwable) {
            val failure = if (error is PrintUploadException) {
                error
            } else {
                PrintUploadException(
                    stage = stage,
                    sentBytes = offset,
                    totalBytes = packed.size,
                    completedChunks = completedChunks,
                    totalChunks = totalChunks,
                    transportLabel = transportLabel,
                    cause = error,
                )
            }
            diagnosticLogs?.append(
                "TSPL_JOB_FAILED",
                "stage=${failure.stage.name}, bytes=${failure.sentBytes}/${failure.totalBytes}, " +
                    "progress=${failure.progressPercent}%, " +
                    "chunks=${failure.completedChunks}/${failure.totalChunks}, " +
                    "connected=${bluetoothManager.isConnected}",
                failure,
            )
            throw failure
        }
    }

    /**
     * Packs a semantic print mask left-to-right, most-significant bit first.
     *
     * Logical mask contract: shouldPrintPixel=true is always represented by 1 before
     * device encoding. XP-TT426B real-device testing showed that its BITMAP wire polarity
     * is inverted in this print path (wire 0 heats, wire 1 does not), so each logical byte
     * is inverted exactly once here. Padding remains NOT_PRINT.
     */
    fun packPrintMask(
        printMask: Bitmap,
        threshold: Int = 160,
        reversePrinting: Boolean = false,
    ): ByteArray {
        val bytesPerRow = (printMask.width + 7) / 8
        val output = ByteArrayOutputStream(bytesPerRow * printMask.height)
        val row = IntArray(printMask.width)
        val cutoff = threshold.coerceIn(0, 255)

        for (y in 0 until printMask.height) {
            printMask.getPixels(row, 0, printMask.width, 0, y, printMask.width, 1)
            for (byteIndex in 0 until bytesPerRow) {
                var logicalPrintBits = 0
                for (bit in 0 until 8) {
                    val x = byteIndex * 8 + bit
                    if (x < printMask.width && shouldPrintPixel(row[x], cutoff)) {
                        logicalPrintBits = logicalPrintBits or (0x80 shr bit)
                    }
                }
                val outputBits = if (reversePrinting) logicalPrintBits xor 0xff else logicalPrintBits
                output.write(encodeLogicalPrintBitsForXpTt426b(outputBits))
            }
        }
        return output.toByteArray()
    }

    private fun shouldPrintPixel(maskPixel: Int, threshold: Int): Boolean =
        (maskPixel ushr 24 and 0xff) >= threshold

    private fun encodeLogicalPrintBitsForXpTt426b(logicalPrintBits: Int): Int =
        logicalPrintBits xor 0xff

    private fun formatMm(value: Float): String =
        String.format(Locale.US, "%.1f", value)

}
