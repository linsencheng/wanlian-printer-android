package com.wanlian.printer.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ClosingTextBlockRulesTest {
    @Test
    fun `closing phrase keeps its original character indexes as one block`() {
        val column = listOf("沉", "痛", "悼", "念", "外", "祖", "母", " ", " ", "千", "古")

        val match = ClosingTextBlockRules.matchColumn(column)

        assertEquals("千古", match?.phrase)
        assertEquals(setOf(9, 10), match?.characterIndexes)
    }

    @Test
    fun `ordinary text has no closing block`() {
        assertNull(ClosingTextBlockRules.matchColumn(listOf("沉", "痛", "悼", "念")))
    }

    @Test
    fun `standalone four character main column is not moved as a closing block`() {
        assertNull(ClosingTextBlockRules.matchColumn(listOf("音", "容", "宛", "在")))
        assertEquals(
            "音容宛在",
            ClosingTextBlockRules.matchColumn(
                listOf("沉", "痛", "悼", "念", "音", "容", "宛", "在"),
            )?.phrase,
        )
    }

    @Test
    fun `block offset clamps to the safe drawing area`() {
        assertEquals(
            -20f,
            ClosingTextBlockRules.resolveOffsetDots(
                requestedOffsetDots = -80f,
                blockTopDots = 120f,
                blockBottomDots = 180f,
                safeTopDots = 100f,
                safeBottomDots = 250f,
            ),
            0.001f,
        )
        assertEquals(
            70f,
            ClosingTextBlockRules.resolveOffsetDots(
                requestedOffsetDots = 90f,
                blockTopDots = 120f,
                blockBottomDots = 180f,
                safeTopDots = 100f,
                safeBottomDots = 250f,
            ),
            0.001f,
        )
    }

    @Test
    fun `zero offset preserves the legacy position`() {
        assertEquals(
            0f,
            ClosingTextBlockRules.resolveOffsetDots(
                requestedOffsetDots = 0f,
                blockTopDots = 280f,
                blockBottomDots = 340f,
                safeTopDots = 100f,
                safeBottomDots = 300f,
            ),
            0.001f,
        )
    }

    @Test
    fun `pair alignment solves the moving block offset from real block centers`() {
        val geometry = ClosingTextBlockAlignmentGeometry(
            zeroOffsetCenterYDots = 140f,
            minimumOffsetDots = -30f,
            maximumOffsetDots = 80f,
        )

        val offset = ClosingTextBlockRules.alignmentOffsetDots(
            geometry = geometry,
            anchorCenterYDots = 185f,
        )

        assertEquals(45f, offset, 0.001f)
        assertEquals(185f, geometry.zeroOffsetCenterYDots + offset, 0.001f)
    }

    @Test
    fun `pair alignment clamps moving block inside its own safe area`() {
        val offset = ClosingTextBlockRules.alignmentOffsetDots(
            geometry = ClosingTextBlockAlignmentGeometry(
                zeroOffsetCenterYDots = 140f,
                minimumOffsetDots = -30f,
                maximumOffsetDots = 80f,
            ),
            anchorCenterYDots = 260f,
        )

        assertEquals(80f, offset, 0.001f)
    }
}
