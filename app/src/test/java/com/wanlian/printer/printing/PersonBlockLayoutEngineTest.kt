package com.wanlian.printer.printing

import com.wanlian.printer.model.FooterPerson
import com.wanlian.printer.model.PersonBlockSettings
import com.wanlian.printer.model.PersonLayout
import com.wanlian.printer.model.PersonPlacementMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonBlockLayoutEngineTest {
    private val people = listOf(
        FooterPerson(relation = "媳", name = "黄秋", id = "wife"),
        FooterPerson(relation = "儿", name = "夏凡", id = "son"),
    )

    @Test
    fun `side overlay position changes independently inside safe body bounds`() {
        val first = layout(
            PersonBlockSettings(
                enabled = true,
                persons = people,
                layout = PersonLayout.PARALLEL_COLUMNS,
                placementMode = PersonPlacementMode.SIDE_OVERLAY,
            ),
        )
        val moved = layout(
            PersonBlockSettings(
                enabled = true,
                persons = people,
                layout = PersonLayout.PARALLEL_COLUMNS,
                placementMode = PersonPlacementMode.SIDE_OVERLAY,
                offsetXMm = 5f,
                offsetYMm = 20f,
            ),
        )

        assertNotEquals(first.bounds.left, moved.bounds.left, 0.001f)
        assertNotEquals(first.bounds.top, moved.bounds.top, 0.001f)
        assertEquals(first.effectiveFontSizeDots, moved.effectiveFontSizeDots, 0.001f)
        assertTrue(moved.bounds.left >= 100f)
        assertTrue(moved.bounds.right <= 700f)
    }

    @Test
    fun `inline insert uses requested flow top and centers the person block`() {
        val result = layout(
            PersonBlockSettings(
                enabled = true,
                persons = people,
                layout = PersonLayout.PARALLEL_COLUMNS,
                placementMode = PersonPlacementMode.INLINE_INSERT,
            ),
            inlineTop = 420f,
        )

        assertEquals(420f, result.bounds.top, 0.001f)
        assertEquals(400f, (result.bounds.left + result.bounds.right) / 2f, 0.001f)
        assertEquals(result.coordinates.columns[0].startY, result.coordinates.columns[1].startY, 0.001f)
    }

    @Test
    fun `preview and print receive the exact same person block coordinates`() {
        val settings = PersonBlockSettings(
            enabled = true,
            persons = people,
            layout = PersonLayout.PARALLEL_COLUMNS,
            placementMode = PersonPlacementMode.INLINE_INSERT,
        )

        assertEquals(layout(settings, 500f), layout(settings, 500f))
    }

    @Test
    fun `maximum parallel font stays separated and inside the safe paper area`() {
        val result = layout(
            PersonBlockSettings(
                enabled = true,
                persons = people,
                layout = PersonLayout.PARALLEL_COLUMNS,
                placementMode = PersonPlacementMode.SIDE_OVERLAY,
                fontSizeDots = 240f,
                columnGapMm = 20f,
            ),
        )

        val centers = result.coordinates.columns.map { it.centerX }.sorted()
        assertTrue(centers[1] > centers[0])
        assertTrue(result.bounds.left >= 100f)
        assertTrue(result.bounds.right <= 700f)
        assertTrue(result.bounds.top >= 80f)
        assertTrue(result.bounds.bottom <= 1600f)
    }

    private fun layout(
        settings: PersonBlockSettings,
        inlineTop: Float? = null,
    ): PersonBlockLayoutResult = checkNotNull(
        PersonBlockLayoutEngine.layout(
            settings = settings,
            safeLeft = 100f,
            safeTop = 80f,
            safeRight = 700f,
            safeBottom = 1600f,
            requestedGlyphHeightDots = 44f,
            requestedBaselineOffsetDots = 36f,
            fontWidthScale = 1f,
            inlineTop = inlineTop,
        ),
    )
}
