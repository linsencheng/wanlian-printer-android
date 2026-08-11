package com.wanlian.printer.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PersonLayoutPersistenceTest {
    @Test
    fun `person block copy preserves every layout field and stable ids`() {
        val source = PersonBlockSettings(
            enabled = true,
            persons = listOf(
                FooterPerson(relation = "媳", name = "黄秋", id = "person-1"),
                FooterPerson(relation = "儿", name = "夏凡", id = "person-2"),
            ),
            layout = PersonLayout.PARALLEL_COLUMNS,
            placementMode = PersonPlacementMode.INLINE_INSERT,
            personInsertIndex = 4,
            positionXNorm = 0.35f,
            positionYNorm = 0.55f,
            offsetXMm = -4f,
            offsetYMm = 9f,
            columnGapMm = 11f,
            fontId = "imported:family-font",
            fontSizeDots = 52f,
            characterSpacingDots = 12f,
        )

        val restored = source.copy()

        assertEquals(source, restored)
        assertEquals(listOf("person-1", "person-2"), restored.persons.map { it.id })
    }

    @Test
    fun `new person block defaults to a larger parallel side overlay`() {
        assertEquals(PersonPlacementMode.SIDE_OVERLAY, PersonBlockSettings().placementMode)
        assertEquals(PersonLayout.PARALLEL_COLUMNS, PersonBlockSettings().layout)
        assertEquals(0.5f, PersonBlockSettings().positionXNorm, 0.001f)
        assertEquals(0.55f, PersonBlockSettings().positionYNorm, 0.001f)
        assertEquals(72f, PersonBlockSettings().fontSizeDots, 0.001f)
    }

    @Test
    fun `side overlay never participates in main text flow or preferred paper length`() {
        val side = PersonBlockSettings(
            enabled = true,
            persons = listOf(FooterPerson("媳", "黄秋"), FooterPerson("儿", "夏凡")),
            placementMode = PersonPlacementMode.SIDE_OVERLAY,
        )
        val moved = side.movedBy(12f, 200f)
        val before = PrintSettings(fontSizeDots = 116f, personBlock = side)
        val after = before.copy(personBlock = moved)

        assertEquals(false, moved.participatesInMainTextFlow)
        assertEquals(991f, PrintLayoutRules.resolveAutoLengthMm(700f), 0.001f)
        assertEquals(72f, moved.fontSizeDots, 0.001f)
        assertEquals(before.fontSizeDots, after.fontSizeDots, 0.001f)
    }
}
