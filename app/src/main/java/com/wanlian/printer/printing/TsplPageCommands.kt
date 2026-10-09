package com.wanlian.printer.printing

import kotlin.math.roundToInt

object TsplPageCommands {
    fun build(widthMm: Float, lengthMm: Float): ByteArray {
        require(widthMm.isFinite() && lengthMm.isFinite() && widthMm > 0 && lengthMm > 0)
        // Unitless TSPL SIZE values are inches, not millimetres.
        return "SIZE ${widthMm.roundToInt()} mm,${lengthMm.roundToInt()} mm\r\nGAP 0 mm,0 mm\r\n"
            .toByteArray(Charsets.US_ASCII)
    }
}
