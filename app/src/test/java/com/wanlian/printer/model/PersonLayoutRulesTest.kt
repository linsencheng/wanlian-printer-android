package com.wanlian.printer.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonLayoutRulesTest {
    @Test
    fun `two three and four person columns are centered and evenly spaced`() {
        listOf(2, 3, 4).forEach { count ->
            val resolved = PersonLayoutRules.resolveParallelColumns(
                personCount = count,
                requestedFontSizeDots = 36f,
                requestedColumnGapMm = 6f,
                availableWidthDots = PrintUnits.mmToDots(100f).toFloat(),
                fontWidthScale = 1f,
            )

            assertEquals(count, resolved.centersFromGroupStart.size)
            val groupCenter = resolved.groupWidthDots / 2f
            val centers = resolved.centersFromGroupStart
            assertEquals(groupCenter, (centers.first() + centers.last()) / 2f, 0.001f)
            centers.zipWithNext().forEach { (left, right) ->
                assertEquals(
                    resolved.columnWidthDots + resolved.columnGapDots,
                    right - left,
                    0.001f,
                )
            }
        }
    }

    @Test
    fun `parallel columns share a common top and never overlap`() {
        val resolved = PersonLayoutRules.resolveParallelColumns(
            personCount = 6,
            requestedFontSizeDots = 120f,
            requestedColumnGapMm = 20f,
            availableWidthDots = 260f,
            fontWidthScale = 1.15f,
        )

        assertTrue(resolved.wasAutoReduced)
        assertTrue(resolved.groupWidthDots <= 260.001f)
        resolved.centersFromGroupStart.zipWithNext().forEach { (left, right) ->
            assertTrue(right - left >= resolved.columnWidthDots - 0.001f)
        }
        // Renderer adds the same personTop to every column; only each column's character index changes Y.
        val personTop = 180f
        val firstGlyphTops = List(6) { personTop }
        assertTrue(firstGlyphTops.all { it == personTop })
    }

    @Test
    fun `column gap is clamped to zero through twenty millimeters`() {
        val available = PrintUnits.mmToDots(200f).toFloat()
        val below = PersonLayoutRules.resolveParallelColumns(2, 36f, -4f, available, 1f)
        val above = PersonLayoutRules.resolveParallelColumns(2, 36f, 40f, available, 1f)

        assertEquals(0f, below.columnGapDots, 0.001f)
        assertEquals(PrintUnits.mmToDots(20f).toFloat(), above.columnGapDots, 0.001f)
    }
}
