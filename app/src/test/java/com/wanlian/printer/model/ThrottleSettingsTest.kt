package com.wanlian.printer.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThrottleSettingsTest {
    @Test
    fun `default throttle values fall inside the ranges TsplPrinter clamps to`() {
        val s = PrintSettings()
        // TsplPrinter clamps these before use; defaults must be no-ops within those ranges.
        assertTrue(s.drainPauseEveryBytes in 512..64 * 1024)
        assertTrue(s.drainPauseMs in 0L..2_000L)
        assertTrue(s.prePrintPauseMs in 0L..5_000L)
        assertTrue(s.chunkDelayMs in 0L..100L)
        assertTrue(s.bitmapChunkSize in 256..4096)
    }

    @Test
    fun `pair mode keeps throttle parameters shared across both sides`() {
        val pair = CoupletPairDocument.fromSingle(PrintSettings())
        val updated = pair.replaceSelected(
            pair.left.copy(
                bitmapChunkSize = 512,
                chunkDelayMs = 20L,
                drainPauseEveryBytes = 16 * 1024,
                drainPauseMs = 300L,
                prePrintPauseMs = 800L,
            ),
        )

        assertEquals(512, updated.right.bitmapChunkSize)
        assertEquals(20L, updated.right.chunkDelayMs)
        assertEquals(16 * 1024, updated.right.drainPauseEveryBytes)
        assertEquals(300L, updated.right.drainPauseMs)
        assertEquals(800L, updated.right.prePrintPauseMs)
    }
}
