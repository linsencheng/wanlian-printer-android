package com.wanlian.printer.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PairPrintTimingRulesTest {
    @Test
    fun `991 millimeters at two ips includes physical feed time and safety margin`() {
        val duration = PairPrintTimingRules.estimatedPhysicalPrintDurationMs(
            paperLengthMm = 991f,
            speedInchesPerSecond = 2f,
        )

        assertTrue(duration in 22_500L..22_520L)
    }

    @Test
    fun `inter job wait keeps the diagnostic twenty second minimum`() {
        assertEquals(
            20_000L,
            PairPrintTimingRules.interJobWaitMs(
                paperLengthMm = 100f,
                speedInchesPerSecond = 6f,
            ),
        )
    }
}
