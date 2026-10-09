package com.wanlian.printer.printing

import com.wanlian.printer.model.PrintDirection
import com.wanlian.printer.model.PrintSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class TsplSettingsCommandsTest {
    @Test
    fun `build emits the selected density and decimal speed`() {
        val command = TsplSettingsCommands.build(
            PrintSettings(
                density = 12,
                speedInchesPerSecond = 1.5f,
                printDirection = PrintDirection.REVERSE,
            ),
        ).toString(Charsets.US_ASCII)

        assertEquals(
            "DIRECTION 0,0\r\nDENSITY 12\r\nSPEED 1.5\r\n",
            command,
        )
    }

    @Test
    fun `resolve clamps values to supported command ranges`() {
        val applied = TsplSettingsCommands.resolve(
            PrintSettings(density = 99, speedInchesPerSecond = 0.25f),
        )

        assertEquals(15, applied.density)
        assertEquals(1f, applied.speedInchesPerSecond)
    }
}
