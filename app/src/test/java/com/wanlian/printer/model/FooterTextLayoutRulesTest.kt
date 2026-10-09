package com.wanlian.printer.model

import org.junit.Assert.assertEquals
import org.junit.Test

class FooterTextLayoutRulesTest {
    @Test
    fun `vertical glyph advance adds footer character spacing`() {
        assertEquals(48f, FooterTextLayoutRules.glyphAdvanceDots(48f, 0f), 0.001f)
        assertEquals(94f, FooterTextLayoutRules.glyphAdvanceDots(48f, 46f), 0.001f)
    }

    @Test
    fun `horizontal glyph run adds spacing between every adjacent character`() {
        assertEquals(
            42f,
            FooterTextLayoutRules.horizontalRunWidthDots(
                glyphWidthsDots = listOf(10f, 12f, 8f),
                spacingDots = 6f,
            ),
            0.001f,
        )
    }
}
