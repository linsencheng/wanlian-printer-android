package com.wanlian.printer.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FlowerSettingsTest {
    @Test
    fun `picker exposes at least forty printable flower styles`() {
        val printableStyles = FlowerStyle.entries.filterNot { it == FlowerStyle.NONE }
        assertTrue(printableStyles.size >= 40)
        assertEquals(printableStyles.size, printableStyles.distinctBy(FlowerStyle::name).size)
    }

    @Test
    fun `flower adjustment range supports large ornaments`() {
        assertEquals(8f, FlowerAdjustmentLimits.MIN_SIZE_MM, 0.001f)
        assertEquals(80f, FlowerAdjustmentLimits.MAX_SIZE_MM, 0.001f)
        assertEquals(-60f, FlowerAdjustmentLimits.MIN_OFFSET_MM, 0.001f)
        assertEquals(60f, FlowerAdjustmentLimits.MAX_OFFSET_MM, 0.001f)
    }

    @Test
    fun `clarity mode targets dense center flower styles`() {
        assertTrue(FlowerClarityRules.supports(FlowerStyle.WHITE_CHRYSANTHEMUM))
        assertTrue(FlowerClarityRules.supports(FlowerStyle.CHRYSANTHEMUM_REALISTIC))
        assertTrue(FlowerClarityRules.supports(FlowerStyle.PEONY_OUTLINE))
        assertTrue(FlowerClarityRules.supports(FlowerStyle.DAHLIA))
        assertTrue(!FlowerClarityRules.supports(FlowerStyle.LOTUS))
        assertTrue(!FlowerClarityRules.supports(FlowerStyle.FLOWER_BRANCH))
    }

    @Test
    fun `clarity mode remains opt in for existing templates`() {
        assertTrue(!FlowerSettings().clarityModeEnabled)
    }
}
