package com.wanlian.printer.printing

import com.wanlian.printer.model.PrintSettings
import java.util.Locale

data class AppliedPrinterSettings(
    val density: Int,
    val speedInchesPerSecond: Float,
)

object TsplSettingsCommands {
    fun resolve(settings: PrintSettings): AppliedPrinterSettings = AppliedPrinterSettings(
        density = settings.density.coerceIn(0, 15),
        speedInchesPerSecond = settings.speedInchesPerSecond.coerceIn(1f, 6f),
    )

    fun build(settings: PrintSettings): ByteArray {
        val applied = resolve(settings)
        return buildString {
            append("DIRECTION ${settings.printDirection.tsplValue},0\r\n")
            append("DENSITY ${applied.density}\r\n")
            append("SPEED ${formatSpeed(applied.speedInchesPerSecond)}\r\n")
        }.toByteArray(Charsets.US_ASCII)
    }

    fun formatSpeed(value: Float): String =
        String.format(Locale.US, "%.1f", value.coerceIn(1f, 6f))
}
