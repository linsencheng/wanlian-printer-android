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
    fun `pair alignment solves the moving block offset from the first tail character`() {
        val geometry = ClosingTextBlockAlignmentGeometry(
            zeroOffsetFirstCharacterCenterYDots = 140f,
            minimumOffsetDots = -30f,
            maximumOffsetDots = 80f,
        )

        val offset = ClosingTextBlockRules.alignmentOffsetDots(
            geometry = geometry,
            anchorFirstCharacterCenterYDots = 185f,
        )

        assertEquals(45f, offset, 0.001f)
        assertEquals(185f, geometry.zeroOffsetFirstCharacterCenterYDots + offset, 0.001f)
    }

    @Test
    fun `pair alignment clamps moving block inside its own safe area`() {
        val offset = ClosingTextBlockRules.alignmentOffsetDots(
            geometry = ClosingTextBlockAlignmentGeometry(
                zeroOffsetFirstCharacterCenterYDots = 140f,
                minimumOffsetDots = -30f,
                maximumOffsetDots = 80f,
            ),
            anchorFirstCharacterCenterYDots = 260f,
        )

        assertEquals(80f, offset, 0.001f)
    }

    @Test
    fun `pair alignment corrects each tail character after anchoring the first one`() {
        val offsets = ClosingTextBlockRules.characterOffsetsForAlignment(
            movingZeroOffsetCenters = listOf(100f, 160f),
            anchorCenters = listOf(200f, 280f),
        )

        assertEquals(listOf(0f, 20f), offsets)
        val blockOffset = 100f
        assertEquals(200f, 100f + blockOffset + requireNotNull(offsets)[0], 0.001f)
        assertEquals(280f, 160f + blockOffset + requireNotNull(offsets)[1], 0.001f)
    }

    @Test
    fun `pair alignment corrects both characters when block offset is clamped`() {
        val offsets = requireNotNull(
            ClosingTextBlockRules.characterOffsetsForAlignment(
                movingZeroOffsetCenters = listOf(100f, 160f),
                anchorCenters = listOf(200f, 280f),
                resolvedBlockOffsetDots = 80f,
            ),
        )

        assertEquals(listOf(20f, 40f), offsets)
        assertEquals(200f, 100f + 80f + offsets[0], 0.001f)
        assertEquals(280f, 160f + 80f + offsets[1], 0.001f)
    }

    @Test
    fun `residual correction is calculated for every visible tail character`() {
        assertEquals(
            listOf(3f, -5f),
            ClosingTextBlockRules.residualCharacterCorrections(
                movingCenters = listOf(197f, 285f),
                anchorCenters = listOf(200f, 280f),
            ),
        )
    }
}
