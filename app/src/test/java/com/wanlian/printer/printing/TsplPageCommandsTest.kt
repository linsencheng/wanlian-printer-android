package com.wanlian.printer.printing

import org.junit.Assert.assertEquals
import org.junit.Test

class TsplPageCommandsTest {
    @Test fun longPageUsesExplicitMillimetresAndSinglePageLength() {
        assertEquals("SIZE 100 mm,991 mm\r\nGAP 0 mm,0 mm\r\n",
            TsplPageCommands.build(100f, 990.97534f).toString(Charsets.US_ASCII))
    }
    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidLength() { TsplPageCommands.build(100f, Float.NaN) }
}
