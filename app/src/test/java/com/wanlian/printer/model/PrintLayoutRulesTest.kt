package com.wanlian.printer.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrintLayoutRulesTest {
    @Test
    fun `negative character spacing reduces glyph advance and content height`() {
        assertEquals(-240f, CharacterSpacingRules.clamp(-999f), 0.001f)
        assertEquals(24f, CharacterSpacingRules.glyphAdvanceDots(44f, -20f), 0.001f)
        assertEquals(92f, CharacterSpacingRules.contentExtentDots(44f, 3, -20f), 0.001f)
        assertEquals(1f, CharacterSpacingRules.glyphAdvanceDots(44f, -240f), 0.001f)
    }

    @Test
    fun `auto length raises 700 mm to preferred 991 mm`() {
        assertEquals(991f, PrintLayoutRules.resolveAutoLengthMm(700f), 0.001f)
    }

    @Test
    fun `auto length never clips content longer than preferred target`() {
        assertTrue(PrintLayoutRules.resolveAutoLengthMm(1050f) >= 1050f)
    }

    @Test
    fun `footer reserve is zero when disabled and positive when enabled`() {
        val disabled = FooterLabelSettings(enabled = false)
        val enabled = disabled.copy(enabled = true, text = "全家敬挽")
        assertEquals(0f, PrintLayoutRules.footerReserveMm(disabled), 0.001f)
        assertTrue(PrintLayoutRules.footerReserveMm(enabled) > 0f)
    }

    @Test
    fun `footer label vertical offset supports fifty millimeters in both directions`() {
        assertEquals(
            FooterLabelAdjustmentLimits.MIN_OFFSET_Y_MM,
            FooterLabelAdjustmentLimits.clampOffsetYMm(-80f),
            0.001f,
        )
        assertEquals(
            FooterLabelAdjustmentLimits.MAX_OFFSET_Y_MM,
            FooterLabelAdjustmentLimits.clampOffsetYMm(80f),
            0.001f,
        )
    }

    @Test
    fun `zero body distance still allows footer offset to move upward through real free space`() {
        val layout = FooterBlockLayoutEngine.layout(
            mainContentBottomDots = 200f,
            paperHeightDots = 1200f,
            blockHeightDots = 100f,
            distanceFromMainMm = 0f,
            distanceFromPageEndMm = 10f,
            offsetYMm = -20f,
            pageBottomSafetyDots = 40f,
        )

        assertEquals(820f, layout.topDots, 0.001f)
        assertTrue(layout.topDots > layout.minimumTopDots)
    }

    @Test
    fun `footer offset stops only at the actual body safety boundary`() {
        val layout = FooterBlockLayoutEngine.layout(
            mainContentBottomDots = 320f,
            paperHeightDots = 900f,
            blockHeightDots = 100f,
            distanceFromMainMm = 0f,
            distanceFromPageEndMm = 10f,
            offsetYMm = -50f,
            pageBottomSafetyDots = 40f,
        )

        assertEquals(320f, layout.topDots, 0.001f)
    }

    @Test
    fun `page end distance changes footer anchor when legal space exists`() {
        val nearEnd = FooterBlockLayoutEngine.layout(
            mainContentBottomDots = 100f,
            paperHeightDots = 1200f,
            blockHeightDots = 100f,
            distanceFromMainMm = 0f,
            distanceFromPageEndMm = 5f,
            offsetYMm = 0f,
            pageBottomSafetyDots = 40f,
        )
        val farFromEnd = FooterBlockLayoutEngine.layout(
            mainContentBottomDots = 100f,
            paperHeightDots = 1200f,
            blockHeightDots = 100f,
            distanceFromMainMm = 0f,
            distanceFromPageEndMm = 35f,
            offsetYMm = 0f,
            pageBottomSafetyDots = 40f,
        )

        assertEquals(PrintUnits.mmToDots(30f).toFloat(), nearEnd.topDots - farFromEnd.topDots, 0.001f)
    }

    @Test
    fun `disabled cut guide produces no preview or print-mask segments`() {
        val settings = CutGuideSettings(enabled = false)
        assertEquals(0f, PrintLayoutRules.cutGuideReserveMm(settings), 0.001f)
        assertTrue(PrintLayoutRules.cutGuideSegmentsMm(100f, 991f, settings).isEmpty())
    }

    @Test
    fun `straight cut guide produces one horizontal segment`() {
        val settings = CutGuideSettings(enabled = true, style = CutGuideStyle.STRAIGHT)
        val segments = PrintLayoutRules.cutGuideSegmentsMm(100f, 991f, settings)
        assertEquals(1, segments.size)
        assertEquals(segments.first().startY, segments.first().endY, 0.001f)
    }

    @Test
    fun `inward V center tip always points toward the paper interior`() {
        val settings = CutGuideSettings(enabled = true, style = CutGuideStyle.INWARD_V)
        val segments = PrintLayoutRules.cutGuideSegmentsMm(100f, 991f, settings)
        assertEquals(2, segments.size)
        val bottomY = segments.first().startY
        val centerTipY = segments.first().endY
        assertTrue(centerTipY < bottomY)
        assertEquals(centerTipY, segments.last().endY, 0.001f)
        assertEquals(50f, segments.first().endX, 0.001f)
        assertEquals(50f, segments.last().endX, 0.001f)
    }

    @Test
    fun `auto length remains 991 when disabled cut guide adds no reserve`() {
        val reserve = PrintLayoutRules.cutGuideReserveMm(CutGuideSettings(enabled = false))
        assertEquals(991f, PrintLayoutRules.resolveAutoLengthMm(700f + reserve), 0.001f)
    }

    @Test
    fun `auto length remains 991 when enabled guide still fits target`() {
        val reserve = PrintLayoutRules.cutGuideReserveMm(CutGuideSettings(enabled = true))
        assertEquals(35f, reserve, 0.001f)
        assertEquals(991f, PrintLayoutRules.resolveAutoLengthMm(700f + reserve), 0.001f)
    }

    @Test
    fun `auto length grows when content plus guide reserve exceeds target`() {
        val reserve = PrintLayoutRules.cutGuideReserveMm(CutGuideSettings(enabled = true))
        assertEquals(1005f, PrintLayoutRules.resolveAutoLengthMm(970f + reserve), 0.001f)
    }

    @Test
    fun `disconnected printer gate blocks print job`() {
        assertEquals(
            PrintGate.BLOCKED_DISCONNECTED,
            PrintGate.resolve(ConnectionStatus.DISCONNECTED, isPrinting = false),
        )
    }
}
