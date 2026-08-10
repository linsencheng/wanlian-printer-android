package com.wanlian.printer.printing

import android.graphics.Bitmap
import com.wanlian.printer.bluetooth.BluetoothManager
import com.wanlian.printer.model.PrintSettings
import kotlinx.coroutines.delay
import java.io.ByteArrayOutputStream
import java.util.Locale
import kotlin.math.min

class TsplPrinter(
    private val bluetoothManager: BluetoothManager,
) {
    suspend fun print(
        rendered: RenderedBitmap,
        settings: PrintSettings,
        onProgress: (Float) -> Unit = {},
    ) {
        check(bluetoothManager.isConnected) { "请先连接打印机" }

        val packed = packPrintMask(
            printMask = rendered.printMask,
            threshold = settings.threshold,
            reversePrinting = settings.reversePrinting,
        )
        val widthBytes = (rendered.printMask.width + 7) / 8
        val setup = buildString {
            append("SIZE ${formatMm(rendered.paperWidthMm)} mm,${formatMm(rendered.paperLengthMm)} mm\r\n")
            append("GAP 0 mm,0 mm\r\n")
            append("DIRECTION ${settings.printDirection.tsplValue},0\r\n")
            append("DENSITY ${settings.density.coerceIn(0, 15)}\r\n")
            append("SPEED ${formatSpeed(settings.speedInchesPerSecond)}\r\n")
            append("CLS\r\n")
            append("BITMAP 0,0,$widthBytes,${rendered.printMask.height},0,")
        }.toByteArray(Charsets.US_ASCII)

        bluetoothManager.write(setup)
        var offset = 0
        while (offset < packed.size) {
            val count = min(settings.bitmapChunkSize.coerceIn(256, 4096), packed.size - offset)
            bluetoothManager.write(packed.copyOfRange(offset, offset + count))
            offset += count
            onProgress(offset.toFloat() / packed.size.coerceAtLeast(1))
            // SPP printer input buffers are often small; BLE writes are throttled again per MTU.
            delay(settings.chunkDelayMs.coerceIn(0L, 100L))
        }
        bluetoothManager.write("\r\nPRINT 1,1\r\n".toByteArray(Charsets.US_ASCII))
        onProgress(1f)
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

    private fun formatSpeed(value: Float): String =
        String.format(Locale.US, "%.1f", value.coerceIn(1f, 6f))

}
